-- =====================================================================
-- 紧急修复：myadmin (租户 2098669032436273154 "小微测试店家") 无菜单问题
-- 日期：2026-09-12
-- 根因诊断：
--   1. sys_role 中该租户无任何克隆角色（M15_15 Step 1 可能未成功执行）
--   2. sys_user_role 残留 role_id=2098669033539375106 指向不存在的 sys_role
--   3. selectMenuTreeByUserId JOIN 链断裂 → 返回空 → 侧边栏空白
-- 修复逻辑（全部幂等）：
--   Step 0: 清理 sys_user_role 中指向不存在角色的脏数据
--   Step 1: 补建 5 个克隆角色（boss/finance/manager/employee/platform_admin）
--   Step 2: 补写 sys_role_menu（从 role_template 配方）
--   Step 3: 将 myadmin 关联到 platform_admin 克隆角色
-- 前置：M15_16 已执行（role_template 有 platform_admin 条目）
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 0: 清理 sys_user_role 中指向不存在 sys_role 的脏数据
-- =====================================================================
DELETE sur FROM `sys_user_role` sur
LEFT JOIN `sys_role` r ON r.role_id = sur.role_id
WHERE r.role_id IS NULL;

-- =====================================================================
-- Step 1: 为所有小微版租户补建 5 个缺失的克隆角色（幂等 NOT EXISTS）
-- =====================================================================
-- boss (role_id = 8900100000 + rn*100 + role_sort，显式生成避免雪花ID缺失)
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 8900100000 + ROW_NUMBER() OVER (ORDER BY t.`id`) * 100 + 10,
  t.`id`, '老板', 'boss', 10, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('紧急修复：租户[', t.`id`, ']克隆预设角色，来源:9201')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'boss');

-- finance
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 8900100000 + ROW_NUMBER() OVER (ORDER BY t.`id`) * 100 + 11,
  t.`id`, '财务', 'finance', 11, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('紧急修复：租户[', t.`id`, ']克隆预设角色，来源:9202')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'finance');

-- manager
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 8900100000 + ROW_NUMBER() OVER (ORDER BY t.`id`) * 100 + 12,
  t.`id`, '项目经理', 'manager', 12, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('紧急修复：租户[', t.`id`, ']克隆预设角色，来源:9203')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'manager');

-- employee
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 8900100000 + ROW_NUMBER() OVER (ORDER BY t.`id`) * 100 + 13,
  t.`id`, '报销人', 'employee', 13, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('紧急修复：租户[', t.`id`, ']克隆预设角色，来源:9204')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'employee');

-- platform_admin
INSERT INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`, `data_scope`,
  `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
SELECT 8900100000 + ROW_NUMBER() OVER (ORDER BY t.`id`) * 100 + 1,
  t.`id`, '租户组织管理员', 'platform_admin', 1, '1', 1, 1, '0', '0', 'system-fix', NOW(), '', NULL,
  CONCAT('紧急修复：租户[', t.`id`, ']克隆预设角色，来源:9205')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role` r WHERE r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin');

-- 1.5 防御性校正：确保克隆角色的 tenant_type_scope 与预设角色一致
UPDATE `sys_role` r
JOIN `sys_role` src ON src.`role_key` = r.`role_key` AND src.`tenant_id` IS NULL
SET r.`tenant_type_scope` = src.`tenant_type_scope`
WHERE r.`tenant_id` IS NOT NULL
  AND r.`role_key` IN ('boss', 'finance', 'manager', 'employee', 'platform_admin')
  AND (r.`tenant_type_scope` IS NULL OR r.`tenant_type_scope` = '');

-- =====================================================================
-- Step 2: 按 role_template 配方补写 sys_role_menu（幂等 INSERT IGNORE）
-- =====================================================================
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
-- Step 4: 同步修复全局共享预设角色 9205 (platform_admin) 的 sys_role_menu
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9205, m.`menu_id`
FROM `sys_menu` m
WHERE m.`perms` IN (
    'system:tenant:list',
    'system:role:query',
    'system:industry:list',
    'system:menu:query',
    'system:roleTemplate:list',
    'system:edition:list',
    'system:license:query',
    'system:license:manage'
)
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
     WHERE rm.`role_id` = 9205 AND rm.`menu_id` = m.`menu_id`
);

-- =====================================================================
-- 验收查询（执行后务必核对）
-- =====================================================================
-- A. 该租户克隆角色完整性（应有 5 个）
-- SELECT role_id, role_name, role_key, status, del_flag
--   FROM sys_role WHERE tenant_id = '2098669032436273154';
-- 预期：5 行（boss/finance/manager/employee/platform_admin），del_flag='0'
--
-- B. myadmin 用户角色关联
-- SELECT u.user_name, r.role_key, r.role_name
--   FROM sys_user u
--   JOIN sys_user_role ur ON ur.user_id = u.user_id
--   JOIN sys_role r ON r.role_id = ur.role_id
--  WHERE u.user_name = 'myadmin';
-- 预期：1 行，role_key = 'platform_admin'
--
-- C. 各角色菜单数 > 0
-- SELECT r.role_key, COUNT(rm.menu_id) AS menu_count
--   FROM sys_role r
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.tenant_id = '2098669032436273154'
--  GROUP BY r.role_key ORDER BY r.role_key;
-- 预期：全部 > 0
--
-- D. ★模拟 selectMenuTreeByUserId（与实际路由查询等价）
-- SELECT COUNT(*) AS menu_count FROM (
--   SELECT DISTINCT m.menu_id
--     FROM sys_menu m
--     JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
--     JOIN sys_user_role sur ON rm.role_id = sur.role_id
--     JOIN sys_role ro ON sur.role_id = ro.role_id
--     JOIN sys_user u ON sur.user_id = u.user_id
--    WHERE u.user_name = 'myadmin'
--      AND m.menu_type IN ('M', 'C')
--      AND m.status = '0'
--      AND ro.status = '0'
-- ) sub;
-- 预期：> 0（platform_admin 应有 8+ 菜单）
--
-- ★修复后请退出并重新登录，刷新 Sa-Token 权限快照！
