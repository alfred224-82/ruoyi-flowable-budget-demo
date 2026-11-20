-- =====================================================================
-- M25_04 租户管理员菜单隔离修复 + 合规协议签署守卫权限（v1.82 M25 补充）
-- 版本：v1.0  日期：2026-10-12
-- ---------------------------------------------------------------------
-- 【问题背景】
--   1. 租户管理员（role_key=platform_admin）登录后侧边栏出现
--      「资金平台管理菜单 > 投诉监管（平台监管台/合规协议/活动留痕）」：
--      M25_01 v1.0 误把平台侧监管菜单（7260 目录 + 7265/7266/7267/7268/7270/7271）
--      与 7000 授给 platform_admin 角色（role_template 配方 + 存量 sys_role_menu），
--      叠加历史修复脚本曾向 tenant admin 角色补录 menu 1300，导致整棵平台菜单树可见。
--      ★根因：M25_01 混淆了「平台 Admin（超管 role_id=1）」与角色键 platform_admin（租户管理员）。
--      ★设计原则（T15.33/系统管理规范12条）：租户管理员仅见「租户管理菜单」(7500) 目录；
--        监管菜单（7260~7271）仅平台 Admin（超管）可见（M25_01 §7.3 / M25_02 已授 role_id=1）。
--   2. T25.5 合规协议签署弹窗不出现：后端 /micro/complianceAgreement/current|check|sign
--      使用 @SaCheckPermission("micro:complianceAgreement:view"/":sign")，
--      但 sys_menu 无对应 F 按钮行 → 任何角色（除超管 *:*:*）都取不到权限 → 租户管理员调用 403。
--
-- 【修复内容】（全部幂等，可重复执行）
--   Step 1：新建 F 按钮 7520 合规协议查看 / 7521 合规协议签署（挂 7500 租户管理菜单）
--   Step 2：role_template 移除 platform_admin 误加的 6 个 system:supervision:* 权限键
--   Step 3：role_template 为 platform_admin（micro/micro_pro/micro_lite）追加
--           micro:complianceAgreement:view / :sign
--   Step 4：存量 platform_admin 角色 sys_role_menu 移除平台侧菜单（1300/6000 整棵子树）
--   Step 5：存量 platform_admin 角色 + role_id=1 补授 7520/7521
--
-- 【执行后必做】
--   1. 重启后端 ruoyi-admin（清除 Sa-Token 权限快照）
--   2. 租户管理员退出重新登录（刷新菜单树与权限快照）
-- =====================================================================

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- ---------------------------------------------------------------------
-- Step 1: 新建合规协议守卫 F 按钮（7520/7521，挂 7500 租户管理菜单）
--   ★先删后插幂等（与 M25_01 §5 同模式）
-- ---------------------------------------------------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7520, 7521);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7520, '合规协议查看', 7500, 5, '', NULL, 1, 0, 'F', '0', '0', 'micro:complianceAgreement:view', 'pc', '#', 'admin', NOW(), 'T25.5 登录守卫：查询当前协议版本与本租户签署状态'),
(7521, '合规协议签署', 7500, 6, '', NULL, 1, 0, 'F', '0', '0', 'micro:complianceAgreement:sign', 'pc', '#', 'admin', NOW(), 'T25.5 租户 Admin 首次登录强制签署弹窗');

-- ---------------------------------------------------------------------
-- Step 2: role_template 移除 platform_admin 误加的 system:supervision:* 权限键
--   ★监管权限仅超管 role_id=1 持有（不走 role_template 配方）
-- ---------------------------------------------------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:supervision:view')))
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:supervision:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:supervision:stopActivity')))
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:supervision:stopActivity"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:supervision:stopTenant')))
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:supervision:stopTenant"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:supervision:issueLicense')))
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:supervision:issueLicense"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:supervision:agreement')))
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:supervision:agreement"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:supervision:activityTrace')))
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:supervision:activityTrace"');

-- ---------------------------------------------------------------------
-- Step 3: role_template 为 platform_admin 追加合规协议守卫权限（全小微版本档）
-- ---------------------------------------------------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complianceAgreement:view')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complianceAgreement:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:complianceAgreement:sign')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:complianceAgreement:sign"');

-- ---------------------------------------------------------------------
-- Step 4: 存量 platform_admin 角色移除平台侧菜单授权
--   ★递归 CTE 覆盖 1300/6000 整棵子树（含 7260~7271/7000/7005/6001/1310~1359/6120/6160 等），
--     防止遗留 perms 进入租户管理员权限快照造成接口越权
-- ---------------------------------------------------------------------
WITH RECURSIVE `pm` AS (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_id` IN (1300, 6000)
    UNION ALL
    SELECT m.`menu_id` FROM `sys_menu` m JOIN `pm` ON m.`parent_id` = `pm`.`menu_id`
)
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id` AND t.`tenant_type` = 'micro'
JOIN `pm` ON `pm`.`menu_id` = rm.`menu_id`
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0';

-- ---------------------------------------------------------------------
-- Step 5: 补授 7520/7521（存量 platform_admin 角色 + 超管 role_id=1）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7520, 7521);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, 7520), (1, 7521);

SET SQL_SAFE_UPDATES = 1;

-- =====================================================================
-- 验收查询（执行后人工核对）
-- =====================================================================
-- V1. 7520/7521 已建（预期 2 行，F 类型，parent_id=7500）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu WHERE menu_id IN (7520, 7521);
--
-- V2. role_template platform_admin 不再含 supervision 权限、已含协议守卫权限（预期 0/0/.../1/1）
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys, '"system:supervision:view"')         AS bad_view,
--        JSON_CONTAINS(menu_keys, '"system:supervision:agreement"')    AS bad_agreement,
--        JSON_CONTAINS(menu_keys, '"micro:complianceAgreement:view"')  AS has_view,
--        JSON_CONTAINS(menu_keys, '"micro:complianceAgreement:sign"')  AS has_sign
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
--
-- V3. 租户管理员角色不再持有平台侧菜单（预期 0 行）
-- SELECT COUNT(*) FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id AND r.role_key = 'platform_admin'
--   JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (1300, 6000, 6001, 7000, 7005, 7260, 7261, 7262, 7265, 7266, 7267, 7268, 7270, 7271);
--
-- V4. 租户管理员角色已持有 7520/7521 与 7500 目录（预期每租户 3 行以上）
-- SELECT r.tenant_id, rm.menu_id FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id AND r.role_key = 'platform_admin'
--   JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (7500, 7520, 7521) ORDER BY r.tenant_id, rm.menu_id;
--
-- ★执行后请重启后端 + 租户管理员退出重新登录刷新权限快照
-- =====================================================================
