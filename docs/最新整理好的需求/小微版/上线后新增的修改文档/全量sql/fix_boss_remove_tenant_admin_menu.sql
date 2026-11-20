-- =====================================================================
-- 修复：boss 角色误关联「租户管理菜单」（7500~7509）+ sys_user_role 双表不同步
-- 日期：2026-10-10
-- 概述： boss 角色侧边栏不应出现「租户管理菜单」目录（menu_id 7500~7509），
--       该目录是 platform_admin（租户管理员）专属菜单；
--       ② sys_user_role（legacy 表）与 user_tenant_role（小微版新表）不同步，
--       导致用户被分配 boss 后，登录时从 sys_user_role 回退读到 platform_admin 菜单。
-- 修复：① 从 boss 克隆角色的 sys_role_menu 中移除 7500~7509；
--       ② 同步 sys_user_role：按 user_tenant_role 的实际分配重写 sys_user_role 关联；
--       ③ 配套 Java 修复 UserTenantRoleServiceImpl.assign() 新增 syncSysUserRole()。
-- 幂等：DELETE + INSERT 幂等，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- ----------------------------
-- 1. 删除所有小微版 boss 克隆角色中残留的 7500~7509 菜单关联
-- ----------------------------
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro'
  AND r.`role_key` = 'boss'
  AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7500 AND 7509;

-- ----------------------------
-- 2. ★同步 sys_user_role 与 user_tenant_role（双表不一致根因修复）
--    根因：assign() 此前只写 user_tenant_role，但登录时 SysPermissionService.getRolePermission()
--          通过 sys_user_role → sys_role 读取角色权限，两表不一致导致用户拿到错误角色的菜单。
--    策略：按 user_tenant_role 的实际分配，重写每个小微版用户在 sys_user_role 中的克隆角色关联。
-- ----------------------------
-- 2.1 删除所有小微版用户在 sys_user_role 中的克隆角色关联（tenant_id 非 NULL 的角色）
DELETE sur
FROM `sys_user_role` sur
JOIN `sys_role` r ON r.`role_id` = sur.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro';

-- 2.2 按 user_tenant_role 的实际分配重新插入 sys_user_role
INSERT INTO `sys_user_role` (`user_id`, `role_id`)
SELECT utr.`user_id`, r.`role_id`
FROM `user_tenant_role` utr
JOIN `sys_role` r ON r.`role_key` = utr.`role_code` AND r.`tenant_id` = utr.`tenant_id`
JOIN `sys_tenant` t ON t.`id` = utr.`tenant_id` AND t.`tenant_type` = 'micro'
WHERE utr.`del_flag` = '0'
  AND utr.`status` = '0'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` sur
      WHERE sur.`user_id` = utr.`user_id` AND sur.`role_id` = r.`role_id`
  );

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- A. boss 克隆角色不应持有 7500~7509 任何菜单
-- SELECT r.role_id, t.company_name, rm.menu_id
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--    AND rm.menu_id BETWEEN 7500 AND 7509;
--   -- 预期：0 行
--
-- B. platform_admin 克隆角色仍持有 7500 目录
-- SELECT r.role_id, t.company_name, rm.menu_id
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--    AND rm.menu_id = 7500;
--   -- 预期：每个租户 1 行（menu_id=7500）
--
-- C. ★sys_user_role 与 user_tenant_role 一致（把 'lijie' 替换为实际用户名）
-- SELECT u.user_name, r.role_key AS sys_user_role_key
--   FROM sys_user u
--   JOIN sys_user_role sur ON sur.user_id = u.user_id
--   JOIN sys_role r ON r.role_id = sur.role_id
--  WHERE u.user_name = 'lijie' AND r.del_flag = '0';
--   -- 预期：仅显示 user_tenant_role 中分配的角色（如 boss），不含 platform_admin
--
-- D. 所有小微版用户 sys_user_role 角色数 = user_tenant_role 角色数
-- SELECT u.user_name,
--   (SELECT COUNT(*) FROM sys_user_role sur JOIN sys_role r ON r.role_id=sur.role_id
--     JOIN sys_tenant t ON t.id=r.tenant_id WHERE sur.user_id=u.user_id AND t.tenant_type='micro' AND r.del_flag='0') AS sur_cnt,
--   (SELECT COUNT(*) FROM user_tenant_role utr WHERE utr.user_id=u.user_id AND utr.del_flag='0' AND utr.status='0') AS utr_cnt
--   FROM sys_user u
--   JOIN sys_tenant t ON t.id = u.tenant_id AND t.tenant_type = 'micro'
--  HAVING sur_cnt != utr_cnt;
--   -- 预期：0 行（两表完全一致）
