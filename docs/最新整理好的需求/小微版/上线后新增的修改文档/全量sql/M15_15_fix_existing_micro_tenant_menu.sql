-- =====================================================================
-- M15_15 存量小微版租户全量修复脚本（T15.29 补漏，v1.1 重写）
-- 版本：v1.1  日期：2026-09-12
-- 依据：SysTenantServiceImpl.clonePresetRoles() 此前未适配小微版 role_template 路径，
--       导致小微版租户：①克隆角色可能缺失 ②sys_role_menu 为 0 条 ③sys_user_role 未关联
--       租户管理员登录后无角色、无菜单。
-- 修复逻辑（三步，全部幂等）：
--   Step 1: 补建缺失的克隆预设角色（boss/finance/manager/employee/platform_admin）
--   Step 2: 按 role_template(version_type×role_code) 补写 sys_role_menu
--   Step 3: 将租户管理员用户关联到 platform_admin 克隆角色
-- 前置：M15_01（role_template 表）、M15_06（sys_menu 7000~7499）、M15_07（micro 配方）
--       M15_09（tenant_type_scope 列）、M15_10（version_type 列）
-- =====================================================================

-- =====================================================================
-- Step 1: 补建缺失的克隆预设角色（幂等：NOT EXISTS 判断）
-- =====================================================================
-- 1.1 诊断：查看哪些小微版租户缺少克隆角色（执行前）
-- SELECT t.id, t.company_name, t.tenant_type, t.version_type,
--        GROUP_CONCAT(CASE WHEN r.role_id IS NULL THEN preset.role_key END) AS missing_roles
--   FROM sys_tenant t
--   CROSS JOIN (SELECT 'boss' role_key UNION ALL SELECT 'finance' UNION ALL SELECT 'manager'
--               UNION ALL SELECT 'employee' UNION ALL SELECT 'platform_admin') preset
--   LEFT JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = preset.role_key
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.tenant_type, t.version_type
--  HAVING missing_roles IS NOT NULL;

-- 1.2 补建：为每个小微版租户补建缺失的预设角色克隆
-- 逻辑：sys_tenant(micro) × 5 预设角色 → NOT EXISTS 则 INSERT
-- 克隆源：全局共享预设角色 9201~9205（M2_01 建）

-- boss (来源 9201)
INSERT INTO `sys_role` (`tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT t.`id`, '老板', 'boss', 10, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('系统修复补建：租户[', t.`id`, ']克隆预设角色，来源:9201')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'boss');

-- finance (来源 9202)
INSERT INTO `sys_role` (`tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT t.`id`, '财务', 'finance', 11, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('系统修复补建：租户[', t.`id`, ']克隆预设角色，来源:9202')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'finance');

-- manager (来源 9203)
INSERT INTO `sys_role` (`tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT t.`id`, '项目经理', 'manager', 12, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('系统修复补建：租户[', t.`id`, ']克隆预设角色，来源:9203')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'manager');

-- employee (来源 9204)
INSERT INTO `sys_role` (`tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT t.`id`, '报销人', 'employee', 13, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('系统修复补建：租户[', t.`id`, ']克隆预设角色，来源:9204')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'employee');

-- platform_admin (来源 9205)
INSERT INTO `sys_role` (`tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT t.`id`, '租户组织管理员', 'platform_admin', 1, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('系统修复补建：租户[', t.`id`, ']克隆预设角色，来源:9205')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin');

-- 1.3 防御性校正：确保克隆角色的 tenant_type_scope 与预设角色一致
--    （M15_09 已回填预设角色，克隆角色需同步）
SET SQL_SAFE_UPDATES = 0;
UPDATE `sys_role` r
JOIN `sys_role` src ON src.`role_key` = r.`role_key` AND src.`tenant_id` IS NULL
SET r.`tenant_type_scope` = src.`tenant_type_scope`
WHERE r.`tenant_id` IS NOT NULL
  AND r.`role_key` IN ('boss', 'finance', 'manager', 'employee', 'platform_admin')
  AND (r.`tenant_type_scope` IS NULL OR r.`tenant_type_scope` = '');
SET SQL_SAFE_UPDATES = 1;

-- =====================================================================
-- Step 2: 按 role_template 配方补写 sys_role_menu（幂等：INSERT IGNORE）
-- =====================================================================
-- 逻辑：sys_tenant(micro) → sys_role(克隆预设) → role_template(version_type×role_code)
--       → JSON_TABLE(menu_keys) → sys_menu(perms) → INSERT IGNORE sys_role_menu
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT
    r.`role_id`,
    m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r
  ON r.`tenant_id` = t.`id`
 AND r.`role_key` IN ('boss', 'finance', 'manager', 'employee', 'platform_admin')
JOIN `role_template` rt
  ON rt.`version_type` = t.`version_type`
 AND rt.`role_code` = r.`role_key`
 AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(
    rt.`menu_keys`,
    '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')
) AS j
JOIN `sys_menu` m
  ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL
  AND t.`version_type` != ''
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
       WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- =====================================================================
-- Step 3: 将租户管理员用户关联到 platform_admin 克隆角色（幂等）
-- =====================================================================
-- 逻辑：sys_tenant.admin_user_id → sys_user_role（关联 platform_admin 克隆角色）
-- 前提：sys_tenant.admin_user_id 已填写（insertTenant 时写入）
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT
    t.`admin_user_id`,
    r.`role_id`
FROM `sys_tenant` t
JOIN `sys_role` r
  ON r.`tenant_id` = t.`id`
 AND r.`role_key` = 'platform_admin'
WHERE t.`tenant_type` = 'micro'
  AND t.`admin_user_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` ur
       WHERE ur.`user_id` = t.`admin_user_id` AND ur.`role_id` = r.`role_id`
  );

-- =====================================================================
-- 验收查询（执行后人工核对）
-- =====================================================================
-- A. 小微版租户克隆角色完整性（5 个角色应全部存在）
-- SELECT t.id, t.company_name, t.version_type,
--        GROUP_CONCAT(r.role_key ORDER BY r.role_sort) AS cloned_roles
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key IN ('boss','finance','manager','employee','platform_admin')
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type;
-- 预期：cloned_roles = 'platform_admin,boss,finance,manager,employee'（5 个）
--
-- B. 各克隆角色菜单数 > 0
-- SELECT r.role_key, COUNT(rm.menu_id) AS menu_count
--   FROM sys_role r
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.tenant_id = (SELECT id FROM sys_tenant WHERE tenant_type='micro' LIMIT 1)
--    AND r.role_key IN ('boss','finance','manager','employee','platform_admin')
--  GROUP BY r.role_key ORDER BY r.role_key;
-- 预期：全部 > 0（boss 最多 ~30+，platform_admin ~10+）
--
-- C. ★管理员用户角色关联（修复前为空的症状）
-- SELECT u.user_name, r.role_key, r.role_name
--   FROM sys_user u
--   JOIN sys_user_role ur ON ur.user_id = u.user_id
--   JOIN sys_role r ON r.role_id = ur.role_id
--  WHERE u.user_name = 'zymfadmin';
-- 预期：1 行，role_key = 'platform_admin'
--
-- D. boss 核心菜单验证
-- SELECT m.menu_id, m.menu_name, m.perms
--   FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE r.tenant_id = (SELECT id FROM sys_tenant WHERE tenant_type='micro' LIMIT 1)
--    AND r.role_key = 'boss'
--    AND m.perms IN ('micro:dashboard:view','micro:booking:list','micro:settle:list','micro:promo:list')
--  ORDER BY m.menu_id;
-- 预期：4 行
