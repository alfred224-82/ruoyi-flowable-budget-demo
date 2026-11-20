-- =====================================================================
-- PROD_04 重复房间（空间分类 service_item）清理：同租户+同分类同名去重保留 1 条
-- 版本：v1.0  日期：2026-10-25
-- 背景：并发自愈初始化（IndustryTemplateServiceImpl.selfHealCurrentTenantResources
--       「判空→补录」非原子、无锁）导致行业模板 preset 房间被双插，
--       房间资源管理 / 房间布局配置 / 自助预约选房间等入口出现同名房间×2。
--       代码侧已同步修复：租户级初始化锁 + 名称级幂等 + 读侧去重防御 + 同名禁建，
--       本脚本负责清理存量重复数据。
-- 策略：按 (tenant_id, category_id, name) 重复组保留 1 条：
--       ① 组内存在被 JSON 引用的 id（booking_order.remark 的 mainResourceId/optionalRoomIds、
--          promotion_activity.selected_room_ids）时保留 MIN(被引用 id)
--          （JSON 数组无法安全整串改写，保留被引用行使引用继续有效）；
--       ② 否则保留 MIN(id)。
--       简单 FK 列（booking_order.service_item_id / price_change_log.item_id /
--       room_seat_layout.room_id / resource_status_log.resource_id）由被弃 id 改指保留 id；
--       被弃行逻辑删除（del_flag='2'），可回滚恢复。
-- 幂等：临时表驱动，重复执行时已无重复组 → 零变更。
-- 前置：M15_04（service_category/service_item）+ M15_23（space 分类 / rooms 预设）
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 重复组识别（空间分类下 del_flag='0' 的同租户+同分类+同名，count>1）
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_group;
CREATE TEMPORARY TABLE tmp_room_dup_group AS
SELECT si.tenant_id, si.category_id, si.name, MIN(si.id) AS min_id
  FROM service_item si
  JOIN service_category sc ON sc.id = si.category_id AND sc.category_type = 'space'
 WHERE si.del_flag = '0'
 GROUP BY si.tenant_id, si.category_id, si.name
HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------
-- 2. 组内被 JSON 引用的 id（边界匹配，避免误伤其它数字 token）
--    数组 token 四种形态：[D,  ,D,  ,D]  [D]；mainResourceId 标量带 key 锚定
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_json_ref;
CREATE TEMPORARY TABLE tmp_room_dup_json_ref AS
SELECT DISTINCT si.id
  FROM service_item si
  JOIN tmp_room_dup_group g
    ON g.tenant_id = si.tenant_id AND g.category_id = si.category_id AND g.name = si.name
 WHERE si.del_flag = '0'
   AND (
        EXISTS (SELECT 1 FROM booking_order bo
                 WHERE bo.remark LIKE CONCAT('%[', si.id, ',%')
                    OR bo.remark LIKE CONCAT('%,', si.id, ',%')
                    OR bo.remark LIKE CONCAT('%,', si.id, ']%')
                    OR bo.remark LIKE CONCAT('%[', si.id, ']%')
                    OR bo.remark LIKE CONCAT('%"mainResourceId":', si.id, ',%')
                    OR bo.remark LIKE CONCAT('%"mainResourceId":', si.id, '}%'))
     OR EXISTS (SELECT 1 FROM promotion_activity pa
                 WHERE pa.selected_room_ids LIKE CONCAT('%[', si.id, ',%')
                    OR pa.selected_room_ids LIKE CONCAT('%,', si.id, ',%')
                    OR pa.selected_room_ids LIKE CONCAT('%,', si.id, ']%')
                    OR pa.selected_room_ids LIKE CONCAT('%[', si.id, ']%'))
   );

-- ---------------------------------------------------------------------
-- 3. 保留 id 决策：MIN(被引用 id) 优先，否则 MIN(id)
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_keep;
CREATE TEMPORARY TABLE tmp_room_dup_keep AS
SELECT g.tenant_id, g.category_id, g.name,
       COALESCE((SELECT MIN(j.id)
                   FROM tmp_room_dup_json_ref j
                   JOIN service_item si2 ON si2.id = j.id
                  WHERE si2.tenant_id = g.tenant_id
                    AND si2.category_id = g.category_id
                    AND si2.name = g.name), g.min_id) AS keep_id
  FROM tmp_room_dup_group g;

-- ---------------------------------------------------------------------
-- 4. 被弃行清单（组内除 keep_id 外的全部存活行）
-- ---------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_drop;
CREATE TEMPORARY TABLE tmp_room_dup_drop AS
SELECT si.id AS dup_id, k.keep_id
  FROM service_item si
  JOIN tmp_room_dup_keep k
    ON k.tenant_id = si.tenant_id AND k.category_id = si.category_id AND k.name = si.name
 WHERE si.del_flag = '0'
   AND si.id <> k.keep_id;

-- 清理前留痕：本次将合并的重复行（执行结果集可见）
SELECT d.dup_id, d.keep_id, si.tenant_id, si.name, si.price, si.resource_status
  FROM tmp_room_dup_drop d
  JOIN service_item si ON si.id = d.dup_id
 ORDER BY si.tenant_id, si.name, d.dup_id;

-- ---------------------------------------------------------------------
-- 5. 简单 FK 改指 + 被弃行逻辑删除
--    ★被弃行若处于 reserved/in_use，状态以保留行为准（重复行创建即空闲，影响可忽略）
-- ---------------------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE booking_order bo
  JOIN tmp_room_dup_drop d ON bo.service_item_id = d.dup_id
   SET bo.service_item_id = d.keep_id;

UPDATE price_change_log p
  JOIN tmp_room_dup_drop d ON p.item_id = d.dup_id
   SET p.item_id = d.keep_id;

UPDATE room_seat_layout l
  JOIN tmp_room_dup_drop d ON l.room_id = d.dup_id
   SET l.room_id = d.keep_id;

UPDATE resource_status_log r
  JOIN tmp_room_dup_drop d ON r.resource_id = d.dup_id AND r.resource_type = 'room'
   SET r.resource_id = d.keep_id;

UPDATE service_item si
  JOIN tmp_room_dup_drop d ON si.id = d.dup_id
   SET si.del_flag = '2';

SET SQL_SAFE_UPDATES = 1;

-- ---------------------------------------------------------------------
-- 6. 验收：剩余重复组（预期 0 行）
-- ---------------------------------------------------------------------
SELECT si.tenant_id, si.category_id, si.name, COUNT(*) AS cnt
  FROM service_item si
  JOIN service_category sc ON sc.id = si.category_id AND sc.category_type = 'space'
 WHERE si.del_flag = '0'
 GROUP BY si.tenant_id, si.category_id, si.name
HAVING COUNT(*) > 1;

DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_group;
DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_json_ref;
DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_keep;
DROP TEMPORARY TABLE IF EXISTS tmp_room_dup_drop;

-- =====================================================================
-- 回滚（如需恢复被逻辑删除的重复行）：按执行时第 5 节前的留痕结果集逐 id 恢复：
--   UPDATE service_item SET del_flag = '0' WHERE id IN (<dup_id 列表>);
--   （FK 改指为无损操作：保留行与被弃行同租户同分类同名，无需回改）
-- =====================================================================
