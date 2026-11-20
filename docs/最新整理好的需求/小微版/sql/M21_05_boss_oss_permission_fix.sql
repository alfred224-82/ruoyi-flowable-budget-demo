-- =====================================================================
-- M21_05  小微版 boss OSS 上传/查询/删除权限存量修复（v1.0，2026-10-07）
-- =====================================================================
-- 问题：部分存量小微版租户（如 AIA桌游店 micro_pro）的 boss 角色 sys_role_menu
--       缺少 menu_id 1130(文件查询)/1131(文件上传)/1133(文件删除)，
--       导致发布活动上传图片时报 403「无此权限：system:oss:upload」。
-- 根因：这些租户创建时 role_template 尚未包含 system:oss:upload/query/remove，
--       后续 M19_05/M19_06/M20_01 更新了 role_template（模板源头），
--       但存量租户的 sys_role_menu（运行时实例）未同步补录。
-- 修复：为所有小微版 boss 角色补录 1130/1131/1133（均为 F 类型按钮，不污染侧边栏）。
-- ★幂等：INSERT IGNORE + NOT EXISTS，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 为所有小微版 boss 角色补录 OSS 上传/查询/删除权限（F 类型按钮）
-- menu_id: 1130=文件查询(F, system:oss:query)
--          1131=文件上传(F, system:oss:upload)
--          1133=文件删除(F, system:oss:remove)
-- ★不补 1(系统管理,M)/118(文件管理,C)，否则 boss 侧边栏会出现「系统管理」
-- ---------------------------------------------------------------------

-- 补录 1131（文件上传，system:oss:upload）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 1131
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 1131
  );

-- 补录 1130（文件查询，system:oss:query）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 1130
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 1130
  );

-- 补录 1133（文件删除，system:oss:remove）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 1133
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 1133
  );

-- =====================================================================
-- 验收查询
-- =====================================================================
-- A. 所有小微版 boss 已包含 OSS 三权限
-- SELECT t.id, t.company_name, t.version_type, r.role_id,
--        SUM(CASE WHEN rm.menu_id = 1131 THEN 1 ELSE 0 END) AS has_oss_upload,
--        SUM(CASE WHEN rm.menu_id = 1130 THEN 1 ELSE 0 END) AS has_oss_query,
--        SUM(CASE WHEN rm.menu_id = 1133 THEN 1 ELSE 0 END) AS has_oss_remove
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type, r.role_id;
--   -- 预期：每个租户 has_oss_upload=1, has_oss_query=1, has_oss_remove=1
--
-- B. 所有小微版 boss 均无系统管理菜单 1/118（侧边栏不出现「系统管理」）
-- SELECT COUNT(*) AS system_menu_leak
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro' AND rm.menu_id IN (1, 118);
--   -- 预期：0
