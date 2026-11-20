-- =====================================================================
-- 紧急修复：platform_admin 登录无菜单 + 权限隔离修正
-- 版本：v1.36  日期：2026-09-18
-- 根因诊断（三个问题）：
--   1. role_template 中 platform_admin 的 menu_keys 错误包含 admin 级别 perms
--      （system:tenant:list 等），违反 admin 与 platform_admin 菜单隔离原则；
--   2. M 类型目录菜单 7500 的 perms 为空，不会通过 role_template.menu_keys →
--      sys_menu.perms 匹配进入 sys_role_menu，导致 selectMenuTreeByUserId
--      找不到根节点，子菜单 7501~7508 全部成为孤儿，侧边栏空白；
--   3. BizCopyController 的 /map、/map-all、/langs 三个 i18n 只读端点错误加了
--      @SaCheckPermission("system:bizCopy:query")，platform_admin 无此权限，
--      登录时 GetInfo → bizCopy/switchBizCode → loadAllLangs 报权限不足。
--      → 此问题已在 BizCopyController.java 代码层面修复（移除只读端点权限校验）。
-- 修复逻辑（全部幂等）：
--   Step 1: 修正 role_template（移除 admin 级别 perms，仅保留 micro:platform:*）
--   Step 2: 清理存量 platform_admin 克隆角色中错误的 admin 菜单关联
--   Step 3: 补写正确的 sys_role_menu（三件套 C/F 类型 + M 类型目录 7500）
--   Step 4: 同步修复预设角色 9205 的 sys_role_menu
--   Step 5: 补录 M18 新增菜单（7620~7639，platform_admin 相关）
-- 前置：M15_06（小微版菜单）、M15_10（role_template 分档）、M15_17（三件套菜单 7500~7509）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 修正 role_template（移除 admin 级别 perms）
-- =====================================================================
DELETE FROM `role_template`
WHERE `version_type` IN ('micro_lite', 'micro_pro')
  AND `role_code` = 'platform_admin';

INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
('micro_lite', 'platform_admin',
 JSON_ARRAY(
   'micro:platform:upgrade', 'micro:platform:upgrade:query', 'micro:platform:upgrade:do',
   'micro:platform:roleMapping', 'micro:platform:roleMapping:query', 'micro:platform:roleMapping:edit',
   'micro:platform:userManage',
   'micro:platform:theme'
 ),
 JSON_ARRAY(),
 '0', 'admin', 'micro_lite platform_admin：租户管理员三件套 + 主题选择（与 admin 完全隔离）'),
('micro_pro', 'platform_admin',
 JSON_ARRAY(
   'micro:platform:upgrade', 'micro:platform:upgrade:query', 'micro:platform:upgrade:do',
   'micro:platform:roleMapping', 'micro:platform:roleMapping:query', 'micro:platform:roleMapping:edit',
   'micro:platform:userManage',
   'micro:platform:theme'
 ),
 JSON_ARRAY(),
 '0', 'admin', 'micro_pro platform_admin：同 micro_lite（平台管理功能不随业务档位变化）');

-- =====================================================================
-- Step 2: 清理存量 platform_admin 克隆角色中错误的 admin 菜单关联
-- =====================================================================
-- 删除 platform_admin 克隆角色中挂在 admin 菜单（1300~1359, 6000~6099）下的关联
DELETE FROM `sys_role_menu`
WHERE `menu_id` IN (
    SELECT m.menu_id FROM `sys_menu` m
    WHERE m.perms LIKE 'system:tenant:%'
       OR m.perms LIKE 'system:role:%'
       OR m.perms LIKE 'system:industry:%'
       OR m.perms LIKE 'system:menu:%'
       OR m.perms LIKE 'system:roleTemplate:%'
       OR m.perms LIKE 'system:edition:%'
       OR m.perms LIKE 'system:license:%'
)
AND `role_id` IN (
    SELECT r.role_id FROM `sys_role` r
    JOIN `sys_tenant` t ON r.tenant_id = t.id
    WHERE r.role_key = 'platform_admin' AND t.tenant_type = 'micro'
);

-- =====================================================================
-- Step 3: 补写正确的 sys_role_menu
-- =====================================================================
-- 3.1 通过 role_template.menu_keys → sys_menu.perms 匹配（C/F 类型菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type`
  AND rt.`role_code` = 'platform_admin' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`);

-- 3.2 ★关键补录：M 类型目录 7500 的 perms 为空，不会被 menu_keys→perms 匹配，
--     但 selectMenuTreeByUserId 依赖 sys_role_menu 找到根节点才能组装完整树。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7500
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 7500);

-- =====================================================================
-- Step 4: 同步修复预设角色 9205 (platform_admin) 的 sys_role_menu
-- =====================================================================
-- 清理 9205 中错误的 admin 菜单关联
DELETE FROM `sys_role_menu`
WHERE `role_id` = 9205
  AND `menu_id` IN (
    SELECT m.menu_id FROM `sys_menu` m
    WHERE m.perms LIKE 'system:tenant:%'
       OR m.perms LIKE 'system:role:%'
       OR m.perms LIKE 'system:industry:%'
       OR m.perms LIKE 'system:menu:%'
       OR m.perms LIKE 'system:roleTemplate:%'
       OR m.perms LIKE 'system:edition:%'
       OR m.perms LIKE 'system:license:%'
  );

-- 补入正确的三件套菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9205, m.`menu_id`
FROM `sys_menu` m
WHERE m.`perms` IN (
    'micro:platform:upgrade', 'micro:platform:upgrade:query', 'micro:platform:upgrade:do',
    'micro:platform:roleMapping', 'micro:platform:roleMapping:query', 'micro:platform:roleMapping:edit',
    'micro:platform:userManage',
    'micro:platform:theme'
)
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = 9205 AND rm.`menu_id` = m.`menu_id`
);

-- 补入 M 类型目录 7500
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (9205, 7500);

-- =====================================================================
-- Step 5: 补录 M18 新增菜单（7620~7639，platform_admin 相关）
-- =====================================================================
-- 5.1 固定支出配置菜单（7620~7629）→ platform_admin 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin'
  AND m.menu_id BETWEEN 7620 AND 7629;

-- 5.2 预约策略配置菜单（7630~7639）→ platform_admin 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin'
  AND m.menu_id BETWEEN 7630 AND 7639;

-- =====================================================================
-- 验收查询（执行后务必核对）
-- =====================================================================
-- A. role_template 已修正（不再有 admin 级别 perms）
-- SELECT version_type, JSON_LENGTH(menu_keys) AS perms_count,
--        JSON_CONTAINS(menu_keys, '"system:tenant:list"') AS has_admin_perm,
--        JSON_CONTAINS(menu_keys, '"micro:platform:upgrade"') AS has_micro_perm
--   FROM role_template WHERE role_code = 'platform_admin';
-- 预期：micro_lite/micro_pro 各 1 行，has_admin_perm=0，has_micro_perm=1
--
-- B. platform_admin 克隆角色菜单数 > 0
-- SELECT t.id, t.company_name, r.role_key, COUNT(rm.menu_id) AS menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin'
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, r.role_key;
-- 预期：menu_count >= 8（7500 目录 + 7501~7508 四菜单+按钮）
--
-- C. ★模拟 selectMenuTreeByUserId
-- SELECT COUNT(*) AS menu_count FROM (
--   SELECT DISTINCT m.menu_id
--     FROM sys_menu m
--     JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
--     JOIN sys_user_role sur ON rm.role_id = sur.role_id
--     JOIN sys_role ro ON sur.role_id = ro.role_id
--     JOIN sys_user u ON sur.user_id = u.user_id
--    WHERE u.user_name = '你的platform_admin用户名'
--      AND m.menu_type IN ('M', 'C')
--      AND m.status = '0'
--      AND ro.status = '0'
-- ) sub;
-- 预期：> 0
--
-- D. 确认无 admin 菜单泄漏
-- SELECT COUNT(*) AS admin_menu_leak
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id
--   JOIN sys_tenant t ON r.tenant_id = t.id
--  WHERE r.role_key = 'platform_admin' AND t.tenant_type = 'micro'
--    AND rm.menu_id IN (SELECT menu_id FROM sys_menu WHERE perms LIKE 'system:tenant:%');
-- 预期：0
--
-- ★修复后请退出并重新登录，刷新 Sa-Token 权限快照！
