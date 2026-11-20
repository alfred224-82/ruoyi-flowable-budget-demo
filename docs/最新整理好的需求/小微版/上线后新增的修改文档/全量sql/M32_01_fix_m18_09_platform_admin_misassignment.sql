-- =====================================================================
-- M32_01 修复 M18_09 Step 2 缺陷：platform_admin 误挂非 admin 用户
-- 版本：v1.0  日期：2026-10-18
-- 根因：M18_09 Step 2 原逻辑给所有无角色的小微版用户分配 platform_admin，
--       导致老板账号（如李杰/boss）被误挂租户管理员角色，
--       侧边栏泄漏「租户管理菜单」（platform_admin 专属菜单 7500~7509/7510~7516/7620~7639）。
-- 正确设计：admin_user_id 仅持有 platform_admin；boss 属于非 admin 的店主账号。
-- 修复逻辑（全部幂等，可重复执行）：
--   Step 1: 清理非 admin_user_id 用户的 platform_admin 关联（sys_user_role + user_tenant_role）
--   Step 2: 为 admin_user_id 补建 platform_admin 关联（sys_user_role + user_tenant_role）
--   Step 3: 同步 sys_user_role 与 user_tenant_role 保持一致
-- ★前置：M18_09（platform_admin 克隆角色 + 菜单授权）
-- ★幂等：DELETE WHERE + INSERT IGNORE + NOT EXISTS，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 清理非 admin_user_id 用户的 platform_admin 关联
-- =====================================================================
-- 1.1 清理 sys_user_role 中非 admin 用户的 platform_admin 关联
--     条件：用户属于小微版租户 + 用户 ≠ admin_user_id + 持有 platform_admin 角色
DELETE sur FROM `sys_user_role` sur
JOIN `sys_user` u ON sur.`user_id` = u.`user_id`
JOIN `sys_tenant` t ON u.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
JOIN `sys_role` r ON sur.`role_id` = r.`role_id`
  AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
WHERE u.`user_id` <> t.`admin_user_id`;

-- 1.2 清理 user_tenant_role 中非 admin 用户的 platform_admin 关联
DELETE utr FROM `user_tenant_role` utr
JOIN `sys_user` u ON utr.`user_id` = u.`user_id`
JOIN `sys_tenant` t ON utr.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE utr.`role_code` = 'platform_admin'
  AND u.`user_id` <> t.`admin_user_id`;

-- =====================================================================
-- Step 2: 为 admin_user_id 补建 platform_admin 关联
-- =====================================================================
-- 2.1 sys_user_role：admin_user_id → platform_admin 克隆角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT t.`admin_user_id`, r.`role_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id`
  AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND t.`admin_user_id` IS NOT NULL;

-- 2.2 user_tenant_role：admin_user_id → platform_admin
INSERT IGNORE INTO `user_tenant_role` (`user_id`, `tenant_id`, `role_code`, `status`, `is_primary`)
SELECT t.`admin_user_id`, t.`id`, 'platform_admin', '0', 1
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND t.`admin_user_id` IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM `user_tenant_role` utr
    WHERE utr.`user_id` = t.`admin_user_id`
      AND utr.`tenant_id` = t.`id`
      AND utr.`role_code` = 'platform_admin'
  );

-- =====================================================================
-- Step 3: 同步 sys_user_role 与 user_tenant_role 保持一致
--         （M18_09 只写了 sys_user_role，user_tenant_role 可能缺失）
-- =====================================================================
INSERT IGNORE INTO `user_tenant_role` (`user_id`, `tenant_id`, `role_code`, `status`, `is_primary`)
SELECT sur.`user_id`, r.`tenant_id`, r.`role_key`, '0',
  CASE WHEN r.`role_key` = 'platform_admin' THEN 1 ELSE 0 END
FROM `sys_user_role` sur
JOIN `sys_role` r ON sur.`role_id` = r.`role_id` AND r.`del_flag` = '0'
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` IN ('platform_admin', 'boss')
  AND NOT EXISTS (
    SELECT 1 FROM `user_tenant_role` utr
    WHERE utr.`user_id` = sur.`user_id`
      AND utr.`tenant_id` = r.`tenant_id`
      AND utr.`role_code` = r.`role_key`
  );

-- =====================================================================
-- 验收查询（执行后务必核对）
-- =====================================================================

-- A. 非 admin 用户不再持有 platform_admin（预期 0 行）
-- SELECT u.user_name, t.admin_user_id, r.role_key
--   FROM sys_user_role sur
--   JOIN sys_user u ON sur.user_id = u.user_id
--   JOIN sys_tenant t ON u.tenant_id = t.id AND t.tenant_type = 'micro'
--   JOIN sys_role r ON sur.role_id = r.role_id AND r.role_key = 'platform_admin'
--  WHERE u.user_id <> t.admin_user_id;
-- 预期：0 行

-- B. admin_user_id 已持有 platform_admin（每租户 1 行）
-- SELECT t.id, t.company_name, t.admin_user_id, u.user_name, sur.role_id, r.role_key
--   FROM sys_tenant t
--   JOIN sys_user u ON t.admin_user_id = u.user_id
--   JOIN sys_user_role sur ON sur.user_id = u.user_id
--   JOIN sys_role r ON sur.role_id = r.role_id AND r.role_key = 'platform_admin'
--  WHERE t.tenant_type = 'micro';
-- 预期：每租户 1 行

-- C. 李杰（boss）不再持有 platform_admin（预期 0 行）
-- SELECT u.user_name, r.role_key
--   FROM sys_user_role sur
--   JOIN sys_user u ON sur.user_id = u.user_id
--   JOIN sys_role r ON sur.role_id = r.role_id
--  WHERE u.user_name = '李杰' AND r.role_key = 'platform_admin';
-- 预期：0 行

-- D. jjadmin 已持有 platform_admin（预期 1 行）
-- SELECT u.user_name, r.role_key, r.role_id
--   FROM sys_user_role sur
--   JOIN sys_user u ON sur.user_id = u.user_id
--   JOIN sys_role r ON sur.role_id = r.role_id
--  WHERE u.user_name = 'jjadmin' AND r.role_key = 'platform_admin';
-- 预期：1 行

-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
