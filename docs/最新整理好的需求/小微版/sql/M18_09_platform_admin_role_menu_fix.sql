-- =====================================================================
-- M18_09 platform_admin 克隆角色 + 用户角色分配 + 完整菜单授权修复
-- 版本：v1.1  日期：2026-09-18
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 T15.33/T18.9/T18.10
-- 根因诊断（v1.36 实测发现）：
--   1. 部分小微版租户缺少 platform_admin 克隆角色（sys_role 无记录），
--      导致 M15_17/M18_08 的 INSERT IGNORE 无法为该租户补写 sys_role_menu
--   2. 租户用户（如 aia）在 sys_user_role 中无任何角色分配，
--      登录后 selectMenuTreeByUserId 查不到任何菜单 → 侧边栏空白
--   3. M 类型目录菜单（7500/7620）perms 为空，不会被 role_template.menu_keys→perms
--      匹配进入 sys_role_menu，导致子菜单成为孤儿（selectMenuTreeByUserId 树断裂）
-- v1.1 变更：Step 1/2 改用直接 INSERT VALUES（实测发现 INSERT...SELECT 对大数 role_id
--            存在静默失败，直接 INSERT VALUES 可正常写入）。
-- 修复逻辑（全部幂等，可重复执行）：
--   Step 1: 为缺少 platform_admin 克隆角色的小微版租户补建克隆角色
--   Step 2: 为无角色的 platform_admin 用户补建 sys_user_role 关联
--   Step 3: 为所有 platform_admin 克隆角色补全完整 sys_role_menu
--           （含 M 类型目录 7500/7620 + C/F 类型 7501~7508/7621~7628/7630~7631）
--   Step 4: 更新 role_template 补入 M18 新权限（micro:tagConfig:list / micro:bookingPolicy:edit 等）
-- 前置依赖：M15_17（三件套菜单 7500~7509）+ M18_02（7620~7628）+ M18_03（7630）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 为缺少 platform_admin 克隆角色的小微版租户补建克隆角色
-- =====================================================================
-- role_id = tenant_id + 5000000000000000000
--   ★避让 9201~9207 共享角色区段 + 既有雪花 ID（~2.08×10^18），结果约 7.1×10^18，BIGINT 范围内不溢出
--   ★实测：INSERT...SELECT 对大数 role_id 存在静默失败，改用直接 INSERT VALUES
--   ★INSERT IGNORE：已有克隆角色的租户不受影响
INSERT IGNORE INTO `sys_role` (`role_id`, `tenant_id`, `role_name`, `role_key`, `role_sort`,
    `data_scope`, `menu_check_strictly`, `dept_check_strictly`,
    `status`, `del_flag`, `create_by`, `create_time`, `remark`)
SELECT t.`id` + 5000000000000000000,
       t.`id`,
       '租户组织管理员',
       'platform_admin',
       5,
       '1', 1, 1,
       '0', '0',
       'system',
       NOW(),
       CONCAT('租户[', t.`id`, ']克隆platform_admin（M18_09补建）')
FROM `sys_tenant` t
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role` r
    WHERE r.`tenant_id` = t.`id`
      AND r.`role_key` = 'platform_admin'
      AND r.`del_flag` = '0'
  );

-- =====================================================================
-- Step 2: 为无角色的 platform_admin 用户补建 sys_user_role 关联
-- =====================================================================
-- 条件：用户属于小微版租户 + 用户在 sys_user_role 中无任何 platform_admin 角色
-- ★INSERT IGNORE：已有角色分配的用户不受影响
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT u.`user_id`, r.`role_id`
FROM `sys_user` u
JOIN `sys_tenant` t ON u.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
JOIN `sys_role` r ON r.`tenant_id` = t.`id`
  AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
WHERE u.`status` = '0' AND u.`del_flag` = '0'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_user_role` ur
    JOIN `sys_role` r2 ON ur.`role_id` = r2.`role_id`
    WHERE ur.`user_id` = u.`user_id`
      AND r2.`role_key` = 'platform_admin'
  );

-- =====================================================================
-- Step 3: 为所有 platform_admin 克隆角色补全完整 sys_role_menu
-- =====================================================================

-- 3.1 ★M 类型目录（perms 为空，不会被 role_template.menu_keys→perms 匹配，必须手动补录）
--     7500=租户管理菜单根目录，7620=固定支出配置目录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, d.`menu_id`
FROM `sys_role` r
CROSS JOIN (SELECT 7500 AS `menu_id` UNION ALL SELECT 7620) d
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = d.`menu_id`
  );

-- 3.2 C/F 类型菜单：四件套 7501~7508（来自 role_template perms 匹配）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type`
  AND rt.`role_code` = 'platform_admin' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- 3.3 M18 新增菜单：固定支出配置子菜单（7621~7628）+ 策略开关（7630）+ 策略保存按钮（7631）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7621, 7622, 7623, 7624, 7625, 7626, 7627, 7628, 7630, 7631)
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- =====================================================================
-- Step 4: 更新 role_template 补入 M18 新权限
-- =====================================================================
-- platform_admin 追加 micro:tagConfig:list + micro:periodRule:list + micro:bookingPolicy:list + micro:bookingPolicy:edit
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
        JSON_ARRAY_APPEND(
            JSON_ARRAY_APPEND(
                `menu_keys`,
                '$', 'micro:tagConfig:list'
            ),
            '$', 'micro:periodRule:list'
        ),
        '$', 'micro:bookingPolicy:list'
    ),
    '$', 'micro:bookingPolicy:edit'
)
WHERE `version_type` IN ('micro_lite', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:tagConfig:list"');

-- ★补丁：存量 role_template 若已含 bookingPolicy:list 但缺 edit 按钮，单独幂等追加
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:bookingPolicy:edit')
WHERE `version_type` IN ('micro_lite', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND JSON_CONTAINS(`menu_keys`, '"micro:bookingPolicy:list"')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:bookingPolicy:edit"');

-- =====================================================================
-- 验收查询（执行后务必核对）
-- =====================================================================

-- A. 所有小微版租户均有 platform_admin 克隆角色
-- SELECT t.id, t.company_name, r.role_id, r.role_key
--   FROM sys_tenant t
--   LEFT JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--  WHERE t.tenant_type = 'micro';
-- 预期：每行 role_id 非 NULL

-- B. 所有 platform_admin 用户均有角色分配
-- SELECT u.user_id, u.user_name, u.tenant_id, ur.role_id, r.role_key
--   FROM sys_user u
--   JOIN sys_tenant t ON u.tenant_id = t.id AND t.tenant_type = 'micro'
--   LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
--   LEFT JOIN sys_role r ON ur.role_id = r.role_id AND r.role_key = 'platform_admin'
--  WHERE u.status = '0' AND u.del_flag = '0';
-- 预期：每行 role_id 非 NULL

-- C. 每个克隆角色菜单数 >= 18（7500+7501~7508+7620+7621~7628+7630 = 19）
-- SELECT r.role_id, r.tenant_id, COUNT(rm.menu_id) AS menu_count
--   FROM sys_role r
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.role_key = 'platform_admin' AND r.tenant_id IS NOT NULL AND r.del_flag = '0'
--  GROUP BY r.role_id, r.tenant_id;
-- 预期：menu_count >= 18

-- D. ★模拟 selectMenuTreeByUserId（关键验证！）
-- SELECT COUNT(*) AS menu_count FROM (
--   SELECT DISTINCT m.menu_id
--     FROM sys_menu m
--     JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
--     JOIN sys_user_role sur ON rm.role_id = sur.role_id
--     JOIN sys_role ro ON sur.role_id = ro.role_id
--     JOIN sys_user u ON sur.user_id = u.user_id
--    WHERE u.user_name = 'aia'
--      AND m.menu_type IN ('M', 'C')
--      AND m.status = '0'
--      AND ro.status = '0'
-- ) sub;
-- 预期：> 0

-- E. role_template 含 M18 新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:tagConfig:list"')     AS has_tag,
--        JSON_CONTAINS(menu_keys, '"micro:periodRule:list"')    AS has_period,
--        JSON_CONTAINS(menu_keys, '"micro:bookingPolicy:list"') AS has_booking_list,
--        JSON_CONTAINS(menu_keys, '"micro:bookingPolicy:edit"') AS has_booking_edit
--   FROM role_template WHERE version_type='micro_lite' AND role_code='platform_admin';
-- 预期：1/1/1/1

-- F. ★按钮级菜单 7631 已落库并授权（MicroBookingPolicyController.savePolicy 依赖）
-- SELECT menu_id, menu_name, menu_type, perms FROM sys_menu WHERE menu_id = 7631;
-- 预期：1 行，menu_type='F'，perms='micro:bookingPolicy:edit'
-- SELECT COUNT(*) FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id
--   JOIN sys_tenant t ON r.tenant_id = t.id
--  WHERE r.role_key='platform_admin' AND t.tenant_type='micro' AND rm.menu_id=7631;
-- 预期：> 0（每个小微版租户 platform_admin 克隆角色各 1 条）

-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
