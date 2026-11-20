-- =====================================================================
-- M19_02  小微版 boss 角色补录 system:oss:upload 权限
-- =====================================================================
-- 问题：小微版 boss 角色调用 POST /system/oss/upload 报 403
--       "无此权限：system:oss:upload"。
-- 根因：boss 的 role_template.menu_keys 仅含 micro:* 系列权限，
--       sys_role_menu 中无 menu_id=1131(system:oss:upload)，
--       而前端通用上传组件默认走 /system/oss/upload 接口。
-- 修复：为所有小微版 boss 克隆角色补录 menu_id 1131（文件上传，F 类型按钮，perms=system:oss:upload）。
--       ★只补 1131，绝不补其祖先 1(系统管理,M)/118(文件管理,C)：
--         selectMenuPermsByUserId 只需 1131 即可放行 system:oss:upload；
--         而 selectMenuTreeByUserId 只渲染 M/C 类型，补 1/118 会让 boss 侧边栏冒出「系统管理 > 文件管理」。
-- 影响：仅 tenant_type='micro' 的租户，不影响企业版。
-- 执行后需 boss 用户退出重新登录刷新 Sa-Token 权限快照。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 为所有小微版 boss 克隆角色补录 OSS 上传权限（幂等 INSERT IGNORE）
-- menu_id: 1131=文件上传(F 类型按钮，perms=system:oss:upload，不在侧边栏渲染)
-- ★不补 1(系统管理,M)/118(文件管理,C)，否则 boss 侧边栏会出现「系统管理」
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 1131
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 1131
  );

-- =====================================================================
-- 验收查询
-- =====================================================================
-- A. boss 克隆角色已包含 1131
-- SELECT t.id, t.company_name, r.role_id, r.role_key,
--        COUNT(rm.menu_id) AS menu_count,
--        SUM(CASE WHEN rm.menu_id = 1131 THEN 1 ELSE 0 END) AS has_oss_upload
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, r.role_id, r.role_key;
--   -- 预期：每个租户 has_oss_upload = 1
--
-- B. menu_id=1131 对应的权限码
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id = 1131;
--   -- 预期：1 行，perms='system:oss:upload'
