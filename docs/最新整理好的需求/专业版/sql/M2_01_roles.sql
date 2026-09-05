-- =====================================================================
-- M2_01 五角色初始化脚本（T2.1）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《各角色菜单权限文档》（v2.2）第一节角色体系 / 3.1~3.5
-- 规范：先删后插（按 role_key 精确删除），脚本可重复执行
-- 说明：
--   1. 角色 ID 采用固定 9201~9205 段，避免与雪花 ID/既有角色冲突
--   2. platform_admin 为租户内组织管理员，仅持有用户管理+部门管理菜单（文档 v2.2 3.5）
--   3. 数据范围：老板/财务=全部数据(1)；项目经理=按 fee_project_member
--      过滤（T2.3 数据范围组件在后端实现，角色层先给全部）；报销人=本人
-- =====================================================================

DELETE FROM `sys_role` WHERE `role_key` IN ('boss', 'finance', 'manager', 'employee', 'platform_admin');

INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(9201, '老板',         'boss',           10, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '定总额、建项目、审批项目/付款/变更、看板；主用小程序'),
(9202, '财务',         'finance',        11, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '编制微调、审核报销、核验收入、导出凭证、隐形成本配置；主用PC'),
(9203, '项目经理',     'manager',        12, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '确认收入(三态)、盯利润、发起合同变更；数据范围限本人项目(后端过滤)'),
(9204, '报销人',       'employee',       13, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '填报销单、选项目、补票；主用小程序'),
(9205, '租户组织管理员', 'platform_admin',  1, '1', 1, 1, '0', '0', 'admin', NOW(), '', NULL, '租户内组织管理员：用户管理+部门管理；不持有业务菜单与租户管理菜单');
