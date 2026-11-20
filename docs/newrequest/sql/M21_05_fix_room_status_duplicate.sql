-- =====================================================================
-- M21_05  房间状态管理重复数据检查与清理（v1.75 修复）
-- 版本：v1.0  日期：2026-10-07
-- 依据：用户反馈 Boss PC 房间状态管理页面房间重复显示 2 次
-- 原因：自愈多次触发或手动操作可能产生同名房间记录
-- =====================================================================
-- ★幂等：DELETE 使用 MIN(id) 保留最早记录，仅删除重复项
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 检查：查看当前租户重复的房间记录（仅查询，不修改）
-- ----------------------------
SELECT tenant_id, category_id, name, COUNT(*) AS cnt,
       GROUP_CONCAT(id ORDER BY id) AS duplicate_ids
FROM service_item
WHERE del_flag = '0'
  AND category_id IN (
      SELECT id FROM service_category WHERE category_type = 'space' AND del_flag = '0'
  )
GROUP BY tenant_id, category_id, name
HAVING COUNT(*) > 1;

-- ----------------------------
-- 2. 清理：删除重复房间记录（保留每组同名房间中 id 最小的一条）
--    ★先执行上面检查 SQL 确认有重复数据后再执行本清理语句
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

DELETE FROM service_item
WHERE del_flag = '0'
  AND category_id IN (
      SELECT id FROM service_category WHERE category_type = 'space' AND del_flag = '0'
  )
  AND id NOT IN (
      -- 保留每组同名房间中 id 最小的记录
      SELECT min_id FROM (
          SELECT MIN(id) AS min_id
          FROM service_item
          WHERE del_flag = '0'
            AND category_id IN (
                SELECT id FROM service_category WHERE category_type = 'space' AND del_flag = '0'
            )
          GROUP BY tenant_id, category_id, name
      ) tmp
  );

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. 验证：清理后不应有重复记录（预期返回 0 行）
-- ----------------------------
SELECT tenant_id, category_id, name, COUNT(*) AS cnt
FROM service_item
WHERE del_flag = '0'
  AND category_id IN (
      SELECT id FROM service_category WHERE category_type = 'space' AND del_flag = '0'
  )
GROUP BY tenant_id, category_id, name
HAVING COUNT(*) > 1;
