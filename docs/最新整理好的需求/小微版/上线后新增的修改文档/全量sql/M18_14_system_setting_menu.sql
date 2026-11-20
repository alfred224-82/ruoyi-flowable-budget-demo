-- =====================================================================
-- M18_14 租户管理员系统设置菜单脚本（T18.11，v1.35 新增）
-- 版本：v1.0  日期：2026-09-21
-- 依据：《资金管控系统开发任务文档（小微版）》v1.34 M18
--       T18.11 租户管理员系统设置（BE+PC）
-- 概述：T18.11 后端（MicroSystemSettingController）+ 前端（systemSetting/index.vue）
--       已交付，但菜单 SQL 与 role_template 权限缺失，本脚本补齐。
-- 前置依赖：M15_06（micro 菜单 client_type）+ M15_17（7500 租户管理员目录）
-- 规范：
--   1. T18.11 无新建表（复用 sys_tenant），仅需菜单 + role_template
--   2. menu_id 7695~7697（避让 7680~7690 合规/敏感词区段）
--   3. 先删后插幂等
--   4. role_template platform_admin 追加 micro:systemSetting:list/edit
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 菜单 SQL（租户管理员 PC，menu_id 7695~7697）
--   ★先删后插幂等
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7695 AND 7697;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7695 AND 7697;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7695, '系统设置', 7500, 30, 'systemSetting', 'micro/systemSetting/index', NULL, 1, 0, 'C', '0', '0', 'micro:systemSetting:list', 'pc', 'setting', 'admin', NOW(), '★租户管理员：企业信息编辑 + 安全设置'),
(7696, '企业信息保存', 7695, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:systemSetting:edit', 'pc', '#', 'admin', NOW(), '★企业信息保存按钮（对齐 MicroSystemSettingController.saveCompany 的 @SaCheckPermission）');

-- ----------------------------
-- 2. role_template 更新：platform_admin 补入系统设置权限
--   ★幂等：JSON_CONTAINS 守卫
-- ----------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
        `menu_keys`,
        '$', 'micro:systemSetting:list'
    ),
    '$', 'micro:systemSetting:edit'
)
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:systemSetting:list"');

-- ★补丁：对已含 systemSetting:list 但缺 edit 的存量 role_template 单独追加（幂等）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:systemSetting:edit')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND JSON_CONTAINS(`menu_keys`, '"micro:systemSetting:list"')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:systemSetting:edit"');

-- ----------------------------
-- 3. sys_role_menu 补录：存量租户 platform_admin 克隆角色
--   ★INSERT IGNORE 幂等
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin'
  AND m.menu_id BETWEEN 7695 AND 7697;

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1) 菜单落库（7695~7696）
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 7695 AND 7697;
--   -- 预期 2 行：7695(micro:systemSetting:list) + 7696(micro:systemSetting:edit)
--
-- 2) role_template 含系统设置权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:systemSetting:list"') AS has_list,
--        JSON_CONTAINS(menu_keys, '"micro:systemSetting:edit"') AS has_edit
-- FROM role_template WHERE version_type IN ('micro_pro','micro_lite') AND role_code = 'platform_admin';
--   -- 预期：1, 1
--
-- 3) sys_role_menu 关联完整
-- SELECT m.menu_id, m.menu_name, COUNT(rm.role_id) AS role_count
-- FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
-- WHERE m.menu_id BETWEEN 7695 AND 7697
-- GROUP BY m.menu_id, m.menu_name;
--   -- 预期：每行 role_count >= 1
