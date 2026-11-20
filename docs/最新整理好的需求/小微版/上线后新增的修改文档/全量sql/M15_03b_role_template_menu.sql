-- =====================================================================
-- M15_03 角色模板管理菜单脚本（系统管理包补建，修复 edition 页「前往角色模板管理维护」404）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档》v1.21 T15.7 / T15.26 / T15.29
-- 背景：
--   角色模板管理菜单（menu_id=7005~7007）原仅在小微版 M15_06 中定义，
--   系统管理包（企业版部署路径）无此菜单记录，导致 edition 页「前往角色模板管理维护」按钮跳转 404。
--   角色模板（role_template）为系统级共享表（无 tenant_id），admin 定义总体角色配方，
--   租户管理员自行定义映射角色，新建租户表单不展示角色模板预览。
-- 本脚本：
--   在资金平台管理菜单(1300)下新建角色模板管理菜单(7005~7007)，
--   授予超管 role_id=1，幂等安全（先删后插）。
-- 前置依赖：M15_12（资金平台管理菜单目录 1300 已建）、M1_04（client_type 列已补）
-- =====================================================================

-- ----------------------------
-- 1. 角色模板管理菜单（7005~7007，先删后插幂等，挂资金平台管理菜单 1300 下）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7005, 7006, 7007);
DELETE FROM `sys_menu` WHERE `menu_id` IN (7005, 7006, 7007);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7005, '角色模板管理', 1300, 5, 'roleTemplate', 'micro/roleTemplate/index', NULL, 1, 0, 'C', '0', '0', 'system:roleTemplate:list', 'pc', 'list', 'admin', NOW(), '菜单+区块配方(version_type×role_code)，admin 定义总体角色配方'),
(7006, '新建模板',     7005, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:roleTemplate:add', 'pc', '#', 'admin', NOW(), ''),
(7007, '绑定菜单',     7005, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:roleTemplate:bindMenu', 'pc', '#', 'admin', NOW(), '');

-- ----------------------------
-- 2. 授予超管 role_id=1
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 7005), (1, 7006), (1, 7007);

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- 3.1 角色模板管理菜单存在且挂 1300 下
-- SELECT menu_id, menu_name, parent_id, perms, menu_type FROM sys_menu WHERE menu_id IN (7005, 7006, 7007);
-- 预期：3 行，parent_id=1300（7005）/ 7005（7006,7007）
--
-- 3.2 超管已授权
-- SELECT role_id, menu_id FROM sys_role_menu WHERE role_id = 1 AND menu_id IN (7005, 7006, 7007);
-- 预期：3 行
