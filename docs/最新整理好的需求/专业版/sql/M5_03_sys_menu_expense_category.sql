-- =====================================================================
-- M5_03 费用大类管理菜单脚本（T5.13）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统开发任务文档》T5.13（涉及菜单 2003 同级、内置配置）
--       《资金管控系统设计文档》1.3 科目三层 / 2.2 #3
-- 规范：
--   1. 先删后插，脚本可重复执行
--   2. 菜单挂既有目录 预算管理(menu_id=2000)，与 2003 预算科目同级
--   3. menu_id 顺延占用 3310~3315（3301/3302 已被 M6_01 批量审核占用）
--   4. 同步登记三版本授权模板与既有五角色授权
-- =====================================================================

DELETE FROM `sys_menu` WHERE `menu_id` IN (3310, 3311, 3312, 3313, 3314, 3315);

-- ----------------------------
-- 页面菜单（C 类，挂预算管理目录 2000）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3310, '费用大类管理', 2000, 10, 'fee/category', 'fee/category/index', NULL, 1, 0, 'C', '0', '0', 'fee:category:list', 'component', 'admin', NOW(), '', NULL, '基础版(T5.13 大类主数据+默认拆分比例)');

-- ----------------------------
-- 按钮权限（F 类）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3311, '大类查询',     3310, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:category:query',     '#', 'admin', NOW(), '', NULL, ''),
(3312, '大类新增',     3310, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:category:add',       '#', 'admin', NOW(), '', NULL, ''),
(3313, '大类编辑',     3310, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:category:edit',      '#', 'admin', NOW(), '', NULL, '内置大类编码不可改，仅可改名/启停'),
(3314, '大类删除',     3310, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:category:remove',    '#', 'admin', NOW(), '', NULL, '内置大类不可删'),
(3315, '拆分比例设置', 3310, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:categoryRatio:edit', '#', 'admin', NOW(), '', NULL, '财务设置默认拆分比例(合计=100%)，支撑T5.14编制拆分');

-- ----------------------------
-- 三版本授权模板登记（基础版能力，三版本均含）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` IN (3310, 3311, 3312, 3313, 3314, 3315);
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('BASIC', 3310), ('BASIC', 3311), ('BASIC', 3312), ('BASIC', 3313), ('BASIC', 3314), ('BASIC', 3315),
('ADVANCED', 3310), ('ADVANCED', 3311), ('ADVANCED', 3312), ('ADVANCED', 3313), ('ADVANCED', 3314), ('ADVANCED', 3315),
('PROFESSIONAL', 3310), ('PROFESSIONAL', 3311), ('PROFESSIONAL', 3312), ('PROFESSIONAL', 3313), ('PROFESSIONAL', 3314), ('PROFESSIONAL', 3315);

-- ----------------------------
-- 既有五角色授权补充（幂等：先删后插）
-- 财务(9202)：完整维护（大类CRUD+拆分比例）；老板(9201)：只读（编制时按大类录额度需可见）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE (`role_id` = 9202 AND `menu_id` IN (3310, 3311, 3312, 3313, 3314, 3315))
   OR (`role_id` = 9201 AND `menu_id` IN (3310, 3311));
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9202, 3310), (9202, 3311), (9202, 3312), (9202, 3313), (9202, 3314), (9202, 3315),
(9201, 3310), (9201, 3311);

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3310 AND 3315;   -- 预期 6 行
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template WHERE menu_id BETWEEN 3310 AND 3315 GROUP BY edition;  -- BASIC/ADVANCED/PROFESSIONAL 各 6
-- SELECT role_id, COUNT(*) FROM sys_role_menu WHERE menu_id BETWEEN 3310 AND 3315 GROUP BY role_id;              -- 9202=6, 9201=2
