-- =====================================================================
-- 紧急修复：老板 / 租户管理员角色错配修复（v2.0，2026-10-12）
-- ---------------------------------------------------------------------
-- ★v1.0 严重缺陷（v2.0 修正）：v1.0 误把 sys_tenant.admin_user_id 当作老板账号，
--   Step 1 删除了租户管理员的 platform_admin 关联、Step 2 反而把 boss 克隆角色
--   挂给租户管理员 → 租户管理员变成「老板 + 租户组织管理员」双角色
--   （存量脏数据由 M25_05 Step 4 清理，本脚本 v2.0 起语义修正且幂等）。
-- ★正确设计（insertTenant Step 5 / M15_27 Step 3）：
--   admin_user_id = 租户管理员账号，专属 platform_admin 克隆角色；
--   boss 克隆角色只属于「非 admin_user_id」的门店老板账号（人员管理赋权）。
-- 修复逻辑（全部幂等，可重复执行）：
--   Step 1: 非 admin 用户持有 platform_admin 者先补挂 boss 克隆角色（避免中间态无角色）
--   Step 2: 再删除这些非 admin 用户的 platform_admin 关联
--   Step 3: admin_user_id 误挂的 boss 克隆角色移除（v1.0 存量修复）
--   Step 4: admin_user_id 回补 platform_admin 克隆角色（兜底）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 非 admin 用户持有 platform_admin 者补挂 boss 克隆角色
-- =====================================================================
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT ur.`user_id`, br.`role_id`
FROM `sys_user_role` ur
JOIN `sys_role` pr ON pr.`role_id` = ur.`role_id`
  AND pr.`role_key` = 'platform_admin' AND pr.`del_flag` = '0'
JOIN `sys_user` u ON u.`user_id` = ur.`user_id` AND u.`del_flag` = '0'
JOIN `sys_tenant` t ON t.`id` = u.`tenant_id` AND t.`tenant_type` = 'micro'
JOIN `sys_role` br ON br.`tenant_id` = t.`id`
  AND br.`role_key` = 'boss' AND br.`del_flag` = '0'
WHERE t.`admin_user_id` IS NOT NULL
  AND ur.`user_id` <> t.`admin_user_id`
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` x
      WHERE x.`user_id` = ur.`user_id` AND x.`role_id` = br.`role_id`
  );

-- =====================================================================
-- Step 2: 删除非 admin 用户的 platform_admin 关联
-- =====================================================================
DELETE ur FROM `sys_user_role` ur
JOIN `sys_role` pr ON pr.`role_id` = ur.`role_id`
  AND pr.`role_key` = 'platform_admin' AND pr.`del_flag` = '0'
JOIN `sys_user` u ON u.`user_id` = ur.`user_id`
JOIN `sys_tenant` t ON t.`id` = u.`tenant_id` AND t.`tenant_type` = 'micro'
WHERE t.`admin_user_id` IS NOT NULL
  AND ur.`user_id` <> t.`admin_user_id`;

-- =====================================================================
-- Step 3: admin_user_id 误挂的 boss 克隆角色移除（v1.0 存量修复）
-- =====================================================================
DELETE ur FROM `sys_user_role` ur
JOIN `sys_role` r ON r.`role_id` = ur.`role_id`
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id` AND t.`tenant_type` = 'micro'
WHERE t.`admin_user_id` IS NOT NULL
  AND ur.`user_id` = t.`admin_user_id`;

-- =====================================================================
-- Step 4: admin_user_id 回补 platform_admin 克隆角色（兜底）
-- =====================================================================
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT t.`admin_user_id`, r.`role_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id`
  AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND t.`admin_user_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` ur
      WHERE ur.`user_id` = t.`admin_user_id` AND ur.`role_id` = r.`role_id`
  );

-- =====================================================================
-- 验证查询
-- =====================================================================
-- A. 租户管理员仅持 platform_admin（预期每租户 1 行 role_key=platform_admin）
-- SELECT t.company_name, u.user_name, r.role_key
--   FROM sys_tenant t
--   JOIN sys_user u ON u.user_id = t.admin_user_id
--   JOIN sys_user_role ur ON ur.user_id = u.user_id
--   JOIN sys_role r ON r.role_id = ur.role_id
--  WHERE t.tenant_type = 'micro' AND t.admin_user_id IS NOT NULL;
--
-- B. 错配链接清零：非 admin_user_id 持有 platform_admin 的记录数（预期 0）
-- SELECT COUNT(*) FROM sys_user_role ur
--   JOIN sys_role r ON r.role_id = ur.role_id AND r.role_key = 'platform_admin'
--   JOIN sys_user u ON u.user_id = ur.user_id
--   JOIN sys_tenant t ON t.id = u.tenant_id AND t.tenant_type = 'micro'
--  WHERE t.admin_user_id IS NOT NULL AND ur.user_id <> t.admin_user_id;
--
-- C. 模拟 selectMenuTreeByUserId 验证租户管理员菜单树（替换用户名）
-- SELECT m.menu_id, m.menu_name, m.menu_type
--   FROM sys_menu m
--   JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
--   JOIN sys_user_role sur ON rm.role_id = sur.role_id
--   JOIN sys_user u ON sur.user_id = u.user_id
--  WHERE u.user_name = 'jjadmin'
--    AND m.menu_type IN ('M', 'C') AND m.status = '0';
-- 预期：仅 7500 租户管理菜单及其子页面，【不含】1300 资金平台管理菜单 / 7100 小微运营

-- ★修复后请退出并重新登录，刷新 Sa-Token 权限快照！
