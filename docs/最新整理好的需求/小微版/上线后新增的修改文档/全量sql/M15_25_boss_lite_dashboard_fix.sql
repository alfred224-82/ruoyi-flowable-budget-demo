-- =====================================================================
-- M15_25  micro_lite(免费版) boss 经营驾驶舱首页修复（v1.41，2026-09-19）
-- =====================================================================
-- 根因：role_template micro_lite boss 的 menu_keys 缺少 micro:dashboard:view，
--       导致 boss 克隆角色无 7101（经营概览）菜单，登录后无默认首页，
--       侧边栏仅显示推广发布/店铺管理两个目录（无首页入口）。
-- 修复：
--   ① role_template micro_lite boss 追加 micro:dashboard:view（JSON_CONTAINS 守卫）；
--   ② 存量小微版租户 boss 克隆角色补写 sys_role_menu（INSERT IGNORE 幂等）；
--   ③ PC 前端 micro/dashboard/index.vue 同步适配：lite 档显示简化首页（推广统计+升级提示），
--      pro 档保持完整经营驾驶舱。
-- 依据：《五角色菜单骨架_V3.7_最终完整版.md》§3 老板（经营者）
--       默认首页：🏠 经营驾驶舱（核心经营数据 / 成本构成 / 业务待办与异常处理中心）
--       micro_lite 免费版：推广发布 + 店铺管理 + 活动浏览 + 经营概览（简化版）
-- 前置：[21]M15_06（菜单 7000~7499）+ [25]M15_10（micro_lite 分档配方）
-- 幂等：JSON_CONTAINS 守卫 + INSERT IGNORE，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. role_template micro_lite boss 追加 micro:dashboard:view
-- ---------------------------------------------------------------------
SET SQL_SAFE_UPDATES = 0;
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:dashboard:view')
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:dashboard:view"');
SET SQL_SAFE_UPDATES = 1;

-- 验证：
-- SELECT JSON_CONTAINS(menu_keys, '"micro:dashboard:view"') AS has_dashboard
--   FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：1

-- ---------------------------------------------------------------------
-- 2. 存量小微版租户 boss 克隆角色补写 7101（经营概览）菜单
--    7101 perms='micro:dashboard:view'，component='micro/dashboard/index'
--    copyMenuTemplateForMicro 会自动补全 M 类型祖先目录 7100（小微运营）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7101
  FROM `sys_tenant` t
  JOIN `sys_role` r ON r.`tenant_id` = t.`id`
 WHERE t.`tenant_type` = 'micro'
   AND t.`version_type` = 'micro_lite'
   AND r.`role_key` = 'boss'
   AND r.`del_flag` = '0';

-- 同时补 M 类型祖先目录 7100（perms 为空，不会被 menu_keys 匹配，但 selectMenuTreeByUserId 需要它来构建菜单树）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7100
  FROM `sys_tenant` t
  JOIN `sys_role` r ON r.`tenant_id` = t.`id`
 WHERE t.`tenant_type` = 'micro'
   AND t.`version_type` = 'micro_lite'
   AND r.`role_key` = 'boss'
   AND r.`del_flag` = '0';

-- 验证：
-- SELECT t.company_name, r.role_key, COUNT(rm.menu_id) AS menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss'
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--  GROUP BY t.id, t.company_name, r.role_key;
-- 预期：menu_count >= 11（原 10 + dashboard 7101 + 目录 7100）
