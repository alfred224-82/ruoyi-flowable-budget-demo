-- =====================================================================
-- M40_01 platform_admin 标签/周期规则/员工提成 按钮权限全面补建
-- 版本：v2.0  日期：2026-10-24
-- 根因诊断：
--   ① M37_01 Step 1.3 删除旧 7620~7629（含标签/周期规则全部 F 按钮），
--      Step 4 仅重建 C 类型 7547/7550（perms 仅 :list），未补建 F 按钮。
--   ② M29_03 创建 7350~7354（员工提成 C+F），M37_01 新建 7551 替代 7350
--      但未清理旧菜单 7350~7354，导致 sys_menu 存在重复菜单。
--   ③ M15_17 旧菜单 7508（小程序主题）M37_01 新建 7552（小程序主题设置）
--      替代，但未清理 7508，导致侧边栏出现两个主题菜单。
--   ④ M37_01 Step 8 仅追加 micro:staffCommission:list，遗漏 add/edit/remove/export。
--   → Controller @SaCheckPermission 找不到匹配权限 → 403
-- 修复逻辑（全部幂等，可重复执行）：
--   Step 1: 清理旧菜单 7350~7354（M29_03 遗留，已被 7551 替代）
--           + 清理旧菜单 7508（M15_17 遗留，已被 7552 替代）
--   Step 2: 新建 F 按钮菜单
--     7548(标签编辑,tagConfig:edit) / 7549(标签新增,tagConfig:add) 挂 7547
--     7553(规则新增,periodRule:add) / 7554(规则编辑,periodRule:edit) 挂 7550
--     7557(提成新增,staffCommission:add) / 7558(提成编辑,staffCommission:edit)
--       / 7559(提成删除导出,staffCommission:remove+export) 挂 7551
--   Step 3: role_template platform_admin 追加 7 个按钮级权限键（JSON_CONTAINS 守卫）
--   Step 4: 存量 platform_admin 克隆角色补写 sys_role_menu（INSERT IGNORE）
--   Step 5: 超管 role_id=1 补写 sys_role_menu
-- 配套代码变更：
--   MicroTagPeriodRuleController.java:
--     removeTag() @SaCheckPermission tagConfig:remove → tagConfig:edit
--     removeRule() @SaCheckPermission periodRule:remove → periodRule:edit
--   MicroStaffCommissionController.java:
--     export() @SaCheckPermission staffCommission:export → staffCommission:remove
--   （编辑/删除权限合并，与 F 按钮合并设计一致）
-- 前置依赖：[157]M37_01（菜单层级重构）+ [140]M29_03（员工提成原始菜单）
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 清理 M29_03 遗留旧菜单 7350~7354（已被 7551 替代）
--         + 清理 M15_17 遗留旧菜单 7508（已被 7552 小程序主题设置 替代）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7350, 7351, 7352, 7353, 7354, 7508);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7350, 7351, 7352, 7353, 7354, 7508);

-- =====================================================================
-- Step 2: 新建 F 按钮菜单（先删后插幂等）
-- =====================================================================
-- 2.1 清理可能存在的旧记录（幂等）
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7548, 7549, 7553, 7554, 7555, 7557, 7558, 7559);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7548, 7549, 7553, 7554, 7555, 7557, 7558, 7559);

-- 2.2 标签管理 F 按钮（挂 7547 支出标签管理下）
--     ★编辑权限涵盖删除（Controller removeTag 已改为 tagConfig:edit）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7548, '标签编辑', 7547, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:tagConfig:edit', 'pc', '#', 'admin', NOW(), '★标签修改+删除合并为编辑权限（M37_01 重构后补建）'),
(7549, '标签新增', 7547, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:tagConfig:add', 'pc', '#', 'admin', NOW(), '★标签新增（M37_01 重构后补建）');

-- 2.3 周期规则 F 按钮（挂 7550 分摊周期规则下）
--     ★编辑权限涵盖删除（Controller removeRule 已改为 periodRule:edit）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7553, '规则新增', 7550, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:periodRule:add', 'pc', '#', 'admin', NOW(), '★周期规则新增（M37_01 重构后补建）'),
(7554, '规则编辑', 7550, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:periodRule:edit', 'pc', '#', 'admin', NOW(), '★周期规则修改+删除合并为编辑权限（M37_01 重构后补建）');

-- 2.4 员工提成 F 按钮（挂 7551 员工提成管理下）
--     ★M29_03 原有 7351~7354 被 M37_01 误删（7350 未清理），此处重建
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7557, '提成新增', 7551, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staffCommission:add', 'pc', '#', 'admin', NOW(), '★员工提成新增（M37_01 重构后补建，替代旧 7351）'),
(7558, '提成编辑', 7551, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staffCommission:edit', 'pc', '#', 'admin', NOW(), '★员工提成修改（M37_01 重构后补建，替代旧 7352）'),
(7559, '提成删除导出', 7551, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staffCommission:remove', 'pc', '#', 'admin', NOW(), '★员工提成删除+导出合并（M37_01 重构后补建，替代旧 7353/7354）');

-- =====================================================================
-- Step 3: role_template platform_admin 追加按钮级权限键
-- =====================================================================
-- 3.1 追加 micro:tagConfig:add（JSON_CONTAINS 守卫，幂等）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:tagConfig:add')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:tagConfig:add"');

-- 3.2 追加 micro:tagConfig:edit
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:tagConfig:edit')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:tagConfig:edit"');

-- 3.3 追加 micro:periodRule:add
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:periodRule:add')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:periodRule:add"');

-- 3.4 追加 micro:periodRule:edit
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:periodRule:edit')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:periodRule:edit"');

-- 3.5 追加 micro:staffCommission:add
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:add')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:add"');

-- 3.6 追加 micro:staffCommission:edit
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:edit')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:edit"');

-- 3.7 追加 micro:staffCommission:remove（涵盖删除+导出）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:remove')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:remove"');

-- =====================================================================
-- Step 4: 存量 platform_admin 克隆角色补写 sys_role_menu（INSERT IGNORE）
-- =====================================================================
-- 4.1 通过 role_template.menu_keys → sys_menu.perms 匹配（C/F 类型菜单）
--     ★覆盖全部新建 F 按钮 + M37_01 新建 C 菜单
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
  AND m.`menu_id` IN (7547, 7548, 7549, 7550, 7551, 7552, 7553, 7554, 7557, 7558, 7559)
  AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
    WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- =====================================================================
-- Step 5: 超管 role_id=1 补写 sys_role_menu
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu`
WHERE menu_id IN (7548, 7549, 7553, 7554, 7557, 7558, 7559);

-- =====================================================================
-- Step 6: 清理 M37_01 遗留的 7555（periodRule:remove，Controller 已改用 :edit）
-- =====================================================================
-- 7555 在 M40_01 v1.0 中创建，但 Controller removeRule 已改为 periodRule:edit，
-- 7555(periodRule:remove) 不再需要。若存在则清理。
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7555;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7555;

-- =====================================================================
-- 验收查询（执行后务必核对）
-- =====================================================================

-- A. F 按钮菜单已落库（预期 7 行，全部 menu_type='F'）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms
--   FROM sys_menu WHERE menu_id IN (7548, 7549, 7553, 7554, 7557, 7558, 7559) ORDER BY menu_id;
-- 预期：
--   7548 | 标签编辑     | 7547 | F | micro:tagConfig:edit
--   7549 | 标签新增     | 7547 | F | micro:tagConfig:add
--   7553 | 规则新增     | 7550 | F | micro:periodRule:add
--   7554 | 规则编辑     | 7550 | F | micro:periodRule:edit
--   7557 | 提成新增     | 7551 | F | micro:staffCommission:add
--   7558 | 提成编辑     | 7551 | F | micro:staffCommission:edit
--   7559 | 提成删除导出 | 7551 | F | micro:staffCommission:remove

-- B. 旧菜单 7350~7354/7508/7555 已清理（预期 0 行）
-- SELECT menu_id FROM sys_menu WHERE menu_id IN (7350,7351,7352,7353,7354,7508,7555);

-- C. role_template platform_admin 含全部按钮权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys, '"micro:tagConfig:list"')         AS has_tag_list,
--        JSON_CONTAINS(menu_keys, '"micro:tagConfig:add"')          AS has_tag_add,
--        JSON_CONTAINS(menu_keys, '"micro:tagConfig:edit"')         AS has_tag_edit,
--        JSON_CONTAINS(menu_keys, '"micro:periodRule:list"')        AS has_rule_list,
--        JSON_CONTAINS(menu_keys, '"micro:periodRule:add"')         AS has_rule_add,
--        JSON_CONTAINS(menu_keys, '"micro:periodRule:edit"')        AS has_rule_edit,
--        JSON_CONTAINS(menu_keys, '"micro:staffCommission:list"')   AS has_comm_list,
--        JSON_CONTAINS(menu_keys, '"micro:staffCommission:add"')    AS has_comm_add,
--        JSON_CONTAINS(menu_keys, '"micro:staffCommission:edit"')   AS has_comm_edit,
--        JSON_CONTAINS(menu_keys, '"micro:staffCommission:remove"') AS has_comm_remove
--   FROM role_template
--  WHERE role_code = 'platform_admin' AND status = '0';
-- 预期：micro_pro / micro_lite 各 1 行，全部 1

-- D. 存量 platform_admin 克隆角色已关联新按钮
-- SELECT t.id, t.company_name, r.role_id,
--        SUM(CASE WHEN rm.menu_id = 7548 THEN 1 ELSE 0 END) AS has_tag_edit,
--        SUM(CASE WHEN rm.menu_id = 7549 THEN 1 ELSE 0 END) AS has_tag_add,
--        SUM(CASE WHEN rm.menu_id = 7553 THEN 1 ELSE 0 END) AS has_rule_add,
--        SUM(CASE WHEN rm.menu_id = 7554 THEN 1 ELSE 0 END) AS has_rule_edit,
--        SUM(CASE WHEN rm.menu_id = 7557 THEN 1 ELSE 0 END) AS has_comm_add,
--        SUM(CASE WHEN rm.menu_id = 7558 THEN 1 ELSE 0 END) AS has_comm_edit,
--        SUM(CASE WHEN rm.menu_id = 7559 THEN 1 ELSE 0 END) AS has_comm_remove
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--    AND rm.menu_id IN (7548, 7549, 7553, 7554, 7557, 7558, 7559)
--  GROUP BY t.id, t.company_name, r.role_id;
-- 预期：每个小微版租户 1 行，全部 has_* = 1

-- E. 超管 role_id=1 已关联（预期 7 行）
-- SELECT menu_id FROM sys_role_menu WHERE role_id = 1
--   AND menu_id IN (7548, 7549, 7553, 7554, 7557, 7558, 7559);

-- F. 旧 7350~7354/7508 无残留角色关联（预期 0 行）
-- SELECT COUNT(*) AS orphan FROM sys_role_menu WHERE menu_id IN (7350,7351,7352,7353,7354,7508);

-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照！
