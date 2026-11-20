-- =====================================================================
-- M19_04  micro_lite(免费版) boss 活动管理增删改查权限补全（v1.0，2026-09-19）
-- =====================================================================
-- 需求：免费版 boss 活动管理需要完整的增删改查能力，M19_03 精简时仅保留了
--       micro:promo:list（查看列表），导致「发布活动/编辑/删除/分享海报」按钮全部消失。
--
-- 修复内容：
--   ① role_template micro_lite boss 的 menu_keys 追加
--      micro:promo:add / micro:promo:edit / micro:promo:remove / micro:promo:poster；
--   ② 存量小微版免费版租户 boss 克隆角色补录 sys_role_menu：
--      7152（发布活动）+ 7153（编辑活动）+ 7154（删除活动）+ 7155（生成分享海报）。
--
-- 菜单对照：
--   menu_id  perms                      menu_name        menu_type  parent_id
--   7151     micro:promo:list           活动管理         C          7150
--   7152     micro:promo:add            发布活动         F          7151
--   7153     micro:promo:edit           编辑活动         F          7151
--   7154     micro:promo:remove         删除活动         F          7151
--   7155     micro:promo:poster         生成分享海报     F          7150
--
-- 前置：[64]M19_03（micro_lite boss 菜单精简）
-- 幂等：JSON_SET 覆盖 + INSERT IGNORE，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. role_template micro_lite boss 追加活动 CRUD 权限
-- ---------------------------------------------------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY(
        'micro:dashboard:view',
        'micro:promo:list',
        'micro:promo:add',
        'micro:promo:edit',
        'micro:promo:remove',
        'micro:promo:poster'
    ),
    `remark` = 'micro_lite 老板：经营概览 + 活动管理（增删改查+海报）；★无系统管理/推广发布/店铺管理/推广分销/小程序菜单'
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss';

-- 验证：
-- SELECT menu_keys FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：["micro:dashboard:view","micro:promo:list","micro:promo:add","micro:promo:edit","micro:promo:remove","micro:promo:poster"]

-- ---------------------------------------------------------------------
-- 2. 存量小微版免费版租户 boss 克隆角色补录 sys_role_menu
--    补录 7152/7153/7154/7155（F 类型按钮，祖先 M 目录 7100/7150 已由 M19_03 覆盖）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN (
    SELECT 7152 AS `menu_id`
    UNION ALL SELECT 7153
    UNION ALL SELECT 7154
    UNION ALL SELECT 7155
) m
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` = 'micro_lite'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm2
       WHERE rm2.`role_id` = r.`role_id` AND rm2.`menu_id` = m.`menu_id`
  );

-- ---------------------------------------------------------------------
-- 3. 验证查询（执行后人工核对）
-- ---------------------------------------------------------------------

-- 3.1 role_template micro_lite boss 含活动 CRUD 全量权限
-- SELECT menu_keys FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：6 项 JSON 数组

-- 3.2 存量租户 boss 已包含活动操作按钮
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7152 THEN 1 ELSE 0 END) AS has_add,
--        SUM(CASE WHEN rm.menu_id = 7153 THEN 1 ELSE 0 END) AS has_edit,
--        SUM(CASE WHEN rm.menu_id = 7154 THEN 1 ELSE 0 END) AS has_remove,
--        SUM(CASE WHEN rm.menu_id = 7155 THEN 1 ELSE 0 END) AS has_poster
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id, t.company_name;
-- 预期：每个租户 has_add/has_edit/has_remove/has_poster 均为 1
