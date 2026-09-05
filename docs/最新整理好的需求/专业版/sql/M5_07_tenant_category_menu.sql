-- =====================================================================
-- M5_07 费用大类管理菜单注册（T5.13 三层角色分工）
-- 版本：v1.1  日期：2026-08-30
-- 说明：
--   1. 新增菜单 3323（费用大类管理页）+ 3324（保存按钮）
--   2. platform_admin 角色（9205 及其租户克隆角色）授权
--   3. 先删后插，幂等可重复执行
-- v1.1 变更：菜单名称从'租户大类配置'改为'费用大类管理'（修复乱码显示）
-- =====================================================================

-- ----------------------------
-- 1. 菜单注册（挂在预算管理 2000 目录下）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (3323, 3324);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
(3323, '费用大类管理', 2000, 10, 'tenantCategory', 'fee/tenantCategory/index', 'C', '0', '0', 'fee:tenantCategory:list', 'checkbox', 'admin', NOW()),
(3324, '保存配置', 3323, 1, '', NULL, 'F', '0', '0', 'fee:tenantCategory:edit', '#', 'admin', NOW());

-- ----------------------------
-- 2. platform_admin 共享角色（9205）授权
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9205, 3323),
(9205, 3324);

-- ----------------------------
-- 3. 三版本模板登记（PLATFORM_ADMIN_BASIC/ADVANCED/PROFESSIONAL 均含此菜单）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` IN (3323, 3324);
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_BASIC', 3323),
('PLATFORM_ADMIN_BASIC', 3324),
('PLATFORM_ADMIN_ADVANCED', 3323),
('PLATFORM_ADMIN_ADVANCED', 3324),
('PLATFORM_ADMIN_PROFESSIONAL', 3323),
('PLATFORM_ADMIN_PROFESSIONAL', 3324);
