-- =====================================================================
-- M15_27 小微版租户 boss 等克隆角色补建 + 用户角色错配修复（v1.44，2026-09-19）
-- =====================================================================
-- 根因诊断（实测 DB：tenant=2100599326265475074 AIA桌游店，user=lijie）：
--   1. 存量小微版租户 sys_role 仅有 platform_admin 克隆角色（M18_09 补建），
--      boss/salesman/customer 克隆角色从未创建（clonePresetRoles 仅在租户创建时执行，
--      存量租户未回放），导致租户内"唯一可选角色"就是 platform_admin；
--   2. M18_09 Step 2 的兜底逻辑会把"无角色的小微租户用户"统一挂到 platform_admin，
--      老板账号（lijie）因此拿到租户管理员角色 → 登录后菜单/首页全是租户管理员的
--      （侧边栏=租户管理菜单，首页=企业概览/租户管理员工作台）；
--   3. v1.43 _fix_boss_role_assignment.sql Step 2 依赖 r.role_key='boss' 克隆角色存在，
--      该租户无 boss 克隆角色 → INSERT 0 行；Step 1 反而删掉了 admin_user_id（aia）
--      的 platform_admin 关联 → aia 变成无角色用户；
--   4. ★与 selectMenuPermsByUserId 是否带 tenant_id 无关：sys_user/sys_role/sys_menu/
--      sys_user_role/sys_role_menu 均在 MybatisPlusConfig.IGNORE_TENANT_TABLES 中，
--      隔离靠 role_id 雪花唯一性 + sys_user_role 链接，本 bug 纯属角色数据链路问题。
-- 修复逻辑（全部幂等，可重复执行）：
--   Step 1: 按 role_template 配方（version_type × role_code）为小微版租户补建缺失的
--           克隆角色（boss/salesman/customer/finance/manager/employee），克隆源为
--           全局预设角色 9201~9207；role_id = tenant_id + 5e18 + 偏移（M18_09 同款约定，
--           platform_admin=0 已被 M18_09 占用，boss=1/finance=2/manager=3/employee=4/
--           salesman=5/customer=6）
--   Step 2: 为所有小微版克隆角色按 role_template.menu_keys → sys_menu.perms 补写
--           sys_role_menu，递归 CTE 补全 M 类型祖先目录（与 copyMenuTemplateForMicro 同逻辑）
--   Step 3: 用户角色错配修复：
--           3a. admin_user_id 回补 platform_admin 克隆角色（恢复租户管理员账号，
--               与 insertTenant Step 5 设计一致）；
--           3b. 非 admin_user_id 却持有 platform_admin 的用户（如 lijie）改挂 boss
--               克隆角色（先插 boss 再删 platform_admin，避免中间态无角色）
-- 前置依赖：[21]M15_06（菜单 7000~7499）+ [48]M18_09（platform_admin 克隆）
--           + [60]_fix_boss_role_assignment（无强依赖，仅说明时序）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Step 1: 补建缺失的小微版克隆角色（按 role_template 配方驱动，无配方的角色不建）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`,
    `data_scope`, `menu_check_strictly`, `dept_check_strictly`,
    `status`, `del_flag`, `create_by`, `create_time`, `remark`)
SELECT t.`id` + 5000000000000000000 + CASE rt.`role_code`
         WHEN 'platform_admin' THEN 0 WHEN 'boss' THEN 1 WHEN 'finance' THEN 2
         WHEN 'manager' THEN 3 WHEN 'employee' THEN 4 WHEN 'salesman' THEN 5
         WHEN 'customer' THEN 6 END,
       t.`id`,
       p.`role_name`, p.`role_key`, p.`role_sort`, p.`data_scope`,
       p.`menu_check_strictly`, p.`dept_check_strictly`,
       '0', '0', 'system', NOW(),
       CONCAT('租户[', t.`id`, ']克隆', p.`role_key`, '（M15_27补建）')
FROM `sys_tenant` t
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`status` = '0'
JOIN `sys_role` p ON p.`role_key` = rt.`role_code` AND p.`role_id` BETWEEN 9201 AND 9207
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role` r
      WHERE r.`tenant_id` = t.`id`
        AND r.`role_key` = rt.`role_code`
        AND r.`del_flag` = '0'
  );

-- ---------------------------------------------------------------------
-- Step 2: 为所有小微版克隆角色补写 sys_role_menu（perms 匹配 + M 类型祖先目录递归补全）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
WITH RECURSIVE `base` AS (
    SELECT r.`role_id`, m.`menu_id`
    FROM `sys_tenant` t
    JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`del_flag` = '0'
      AND r.`role_key` IN ('boss', 'finance', 'manager', 'employee', 'platform_admin', 'salesman', 'customer')
    JOIN `role_template` rt ON rt.`version_type` = t.`version_type`
      AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
    CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
    JOIN `sys_menu` m ON m.`perms` = j.`perm`
    WHERE t.`tenant_type` = 'micro'
      AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
), `anc` AS (
    SELECT `role_id`, `menu_id` FROM `base`
    UNION
    SELECT a.`role_id`, p.`menu_id`
    FROM `anc` a
    JOIN `sys_menu` c ON c.`menu_id` = a.`menu_id` AND c.`parent_id` <> 0
    JOIN `sys_menu` p ON p.`menu_id` = c.`parent_id` AND p.`menu_type` = 'M'
)
SELECT `role_id`, `menu_id` FROM `anc`;

-- ---------------------------------------------------------------------
-- Step 3a: admin_user_id 回补 platform_admin 克隆角色（租户管理员账号恢复）
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- Step 3b: 非 admin_user_id 却持有 platform_admin 的用户改挂 boss 克隆角色
--          （platform_admin 为租户管理员账号专属，与 insertTenant Step 5 设计一致）
-- ---------------------------------------------------------------------
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

DELETE ur FROM `sys_user_role` ur
JOIN `sys_role` pr ON pr.`role_id` = ur.`role_id`
  AND pr.`role_key` = 'platform_admin' AND pr.`del_flag` = '0'
JOIN `sys_user` u ON u.`user_id` = ur.`user_id`
JOIN `sys_tenant` t ON t.`id` = u.`tenant_id` AND t.`tenant_type` = 'micro'
WHERE t.`admin_user_id` IS NOT NULL
  AND ur.`user_id` <> t.`admin_user_id`;

-- ---------------------------------------------------------------------
-- 验收查询（执行后务必核对）
-- ---------------------------------------------------------------------
-- A. 每个小微版租户均有 boss 克隆角色（且菜单数 >= 10，含 7100/7101）
-- SELECT t.id, t.company_name, t.version_type, r.role_id, r.role_key,
--        (SELECT COUNT(*) FROM sys_role_menu rm WHERE rm.role_id = r.role_id) AS menu_cnt
--   FROM sys_tenant t
--   LEFT JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--  WHERE t.tenant_type = 'micro';
-- 预期：每行 role_id 非 NULL；micro_lite menu_cnt >= 12（11 perms + 祖先目录）
--
-- B. ★模拟 selectMenuTreeByUserId：boss 用户应看到小微运营/经营概览（替换用户名）
-- SELECT m.menu_id, m.menu_name, m.menu_type, m.perms
--   FROM sys_menu m
--   JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
--   JOIN sys_user_role sur ON rm.role_id = sur.role_id
--   JOIN sys_role ro ON sur.role_id = ro.role_id
--   JOIN sys_user u ON sur.user_id = u.user_id
--  WHERE u.user_name = 'lijie'
--    AND m.menu_type IN ('M', 'C') AND m.status = '0' AND ro.status = '0';
-- 预期：含 7100 小微运营 / 7101 经营概览，且【不含】7500 租户管理菜单
--
-- C. 错配链接清零：非 admin_user_id 持有 platform_admin 的记录数
-- SELECT COUNT(*) FROM sys_user_role ur
--   JOIN sys_role r ON r.role_id = ur.role_id AND r.role_key = 'platform_admin'
--   JOIN sys_user u ON u.user_id = ur.user_id
--   JOIN sys_tenant t ON t.id = u.tenant_id AND t.tenant_type = 'micro'
--  WHERE t.admin_user_id IS NOT NULL AND ur.user_id <> t.admin_user_id;
-- 预期：0
--
-- D. admin_user_id 均已挂 platform_admin
-- SELECT t.id, t.company_name, t.admin_user_id,
--        EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
--               WHERE ur.user_id = t.admin_user_id AND r.role_key = 'platform_admin') AS has_pa
--   FROM sys_tenant t WHERE t.tenant_type = 'micro';
-- 预期：has_pa 全为 1
--
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
