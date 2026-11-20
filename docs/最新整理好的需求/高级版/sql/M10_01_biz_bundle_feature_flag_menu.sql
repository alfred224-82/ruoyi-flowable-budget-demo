-- =====================================================================
-- M10_01 业态包管理 + 功能开关管理菜单脚本（T10.2 / T10.3，v1.12 平台底座）
-- 版本：v1.0  日期：2026-09-10
-- 依据：《资金管控系统开发任务文档》v1.16 T10.2 / T10.3
-- 前置依赖：M1_05_platform_foundation_tables.sql（biz_bundle / feature_flag 建表）
--           M15_12_fund_platform_menu_edition.sql（资金平台管理菜单目录 1300）
-- menu_id 区段：1320~1339（紧跟 M15_12 的 1310~1314，不冲突）
-- 规范：
--   1. 新增「业态包管理」(system:bizBundle:*) 与「功能开关管理」(system:featureFlag:*)
--   2. 统一收纳在「资金平台管理菜单」(1300) 目录下
--   3. 全部新菜单授予超管 role_id=1
-- =====================================================================

-- ----------------------------
-- 1. 业态包管理 + 功能开关管理（先删后插幂等）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1320 AND 1339;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 1320 AND 1339;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 业态包管理（T10.2 多业态平台底座）
(1320, '业态包管理', 1300, 7, 'bizBundle', 'system/bizBundle/index', NULL, 1, 0, 'C', '0', '0', 'system:bizBundle:list', 'pc', 'shopping', 'admin', NOW(), 'biz_bundle 系统级共享表，维护业态包开关/文案/区块/引擎策略'),
(1321, '查询业态包', 1320, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:bizBundle:query',  'pc', '#', 'admin', NOW(), ''),
(1322, '编辑业态包', 1320, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:bizBundle:edit',  'pc', '#', 'admin', NOW(), ''),
-- 功能开关管理（T10.3 表驱动开关）
(1330, '功能开关管理', 1300, 8, 'featureFlag', 'system/featureFlag/index', NULL, 1, 0, 'C', '0', '0', 'system:featureFlag:list', 'pc', 'lock', 'admin', NOW(), 'feature_flag 表驱动开关，启停即时生效，不可改版本归属'),
(1331, '查询开关',   1330, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:featureFlag:query', 'pc', '#', 'admin', NOW(), ''),
(1332, '启停开关',   1330, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:featureFlag:edit',  'pc', '#', 'admin', NOW(), '');

-- ----------------------------
-- 2. 授予超管 role_id=1
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 1320 AND 1339;

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- 3.1 业态包管理 + 功能开关管理存在
-- SELECT menu_id, menu_name, parent_id, perms, menu_type FROM sys_menu WHERE menu_id BETWEEN 1320 AND 1339 ORDER BY menu_id;
-- 预期：1320(业态包管理) + 1321~1322(2个按钮) + 1330(功能开关管理) + 1331~1332(2个按钮) = 6 条
--
-- 3.2 已授权 admin(role_id=1)
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1320 AND 1339;
-- 预期：6
--
-- 3.3 menu_id 无冲突
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 1320 AND 1339;
-- 预期：6
