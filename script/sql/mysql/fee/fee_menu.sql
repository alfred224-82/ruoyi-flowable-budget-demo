-- =============================================
-- 费用管控菜单初始化SQL
-- 版本：v1.0
-- 日期：2026-07-13
-- 说明：menu_id从3000开始分配，避免与现有菜单冲突
-- =============================================

-- ----------------------------
-- 费用管控一级菜单 (menu_id: 3000)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
VALUES (3000, '费用管控', 0, 10, 'fee', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), '', NULL, '费用管控目录');

-- ----------------------------
-- 二级菜单 (menu_id: 3001-3008)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3001, '责任中心管理', 3000, 1, 'costCenter', 'system/fee/costCenter/index', '', 1, 0, 'C', '0', '0', 'fee:costCenter:list', 'tree', 'admin', sysdate(), '', NULL, '责任中心管理菜单'),
(3002, '用途标签管理', 3000, 2, 'purposeTag', 'system/fee/purposeTag/index', '', 1, 0, 'C', '0', '0', 'fee:purposeTag:list', 'tag', 'admin', sysdate(), '', NULL, '用途标签管理菜单'),
(3003, '映射规则管理', 3000, 3, 'mappingRule', 'system/fee/mappingRule/index', '', 1, 0, 'C', '0', '0', 'fee:mappingRule:list', 'edit', 'admin', sysdate(), '', NULL, '映射规则管理菜单'),
(3004, '报销单管理', 3000, 4, 'expense', 'system/fee/expense/index', '', 1, 0, 'C', '0', '0', 'fee:expense:list', 'form', 'admin', sysdate(), '', NULL, '报销单管理菜单'),
(3005, '超预算审批', 3000, 5, 'approval', 'system/fee/approval/index', '', 1, 0, 'C', '0', '0', 'fee:approval:list', 'checkbox', 'admin', sysdate(), '', NULL, '超预算审批菜单'),
(3006, '盈利分析表', 3000, 6, 'report/profitAnalysis', 'system/fee/report/profitAnalysis', '', 1, 0, 'C', '0', '0', 'fee:report:query', 'chart', 'admin', sysdate(), '', NULL, '盈利分析表菜单'),
(3007, '异常监控表', 3000, 7, 'report/anomalyMonitor', 'system/fee/report/anomalyMonitor', '', 1, 0, 'C', '0', '0', 'fee:report:query', 'monitor', 'admin', sysdate(), '', NULL, '异常监控表菜单'),
(3008, '费用分摊管理', 3000, 8, 'allocation', 'system/fee/allocation/index', '', 1, 0, 'C', '0', '0', 'fee:allocation:list', 'swap', 'admin', sysdate(), '', NULL, '费用分摊管理菜单');

-- ----------------------------
-- 按钮权限 - 责任中心管理 (menu_id: 3011-3014)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3011, '责任中心查询', 3001, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:query', '#', 'admin', sysdate(), '', NULL, ''),
(3012, '责任中心新增', 3001, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:add', '#', 'admin', sysdate(), '', NULL, ''),
(3013, '责任中心修改', 3001, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3014, '责任中心删除', 3001, 4, '#', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 用途标签管理 (menu_id: 3021-3024)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3021, '用途标签查询', 3002, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:query', '#', 'admin', sysdate(), '', NULL, ''),
(3022, '用途标签新增', 3002, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:add', '#', 'admin', sysdate(), '', NULL, ''),
(3023, '用途标签修改', 3002, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3024, '用途标签删除', 3002, 4, '#', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:remove', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 映射规则管理 (menu_id: 3031-3035)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3031, '映射规则查询', 3003, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:query', '#', 'admin', sysdate(), '', NULL, ''),
(3032, '映射规则新增', 3003, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:add', '#', 'admin', sysdate(), '', NULL, ''),
(3033, '映射规则修改', 3003, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3034, '映射规则删除', 3003, 4, '#', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:remove', '#', 'admin', sysdate(), '', NULL, ''),
(3035, '映射规则导入', 3003, 5, '#', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:import', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 报销单管理 (menu_id: 3041-3043)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3041, '报销单查询', 3004, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expense:query', '#', 'admin', sysdate(), '', NULL, ''),
(3042, '报销单新增', 3004, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expense:add', '#', 'admin', sysdate(), '', NULL, ''),
(3043, '报销单提交', 3004, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expense:submit', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 超预算审批 (menu_id: 3051-3052)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3051, '超预算审批查询', 3005, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:approval:query', '#', 'admin', sysdate(), '', NULL, ''),
(3052, '超预算审批处理', 3005, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:approval:process', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 报表分析 (menu_id: 3061)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3061, '报表查询', 3006, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:report:query', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 费用分摊 (menu_id: 3071-3072)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3071, '分摊规则查询', 3008, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:allocation:query', '#', 'admin', sysdate(), '', NULL, ''),
(3072, '分摊规则配置', 3008, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:allocation:config', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 二级菜单 - 新增模块 (menu_id: 3009, 3010, 3015)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3009, '预留预算管理', 3000, 9, 'reservedBudget', 'system/fee/reserved-budget/overview', '', 1, 0, 'C', '0', '0', 'fee:reservedBudget:list', 'money', 'admin', sysdate(), '', NULL, '预留预算管理菜单'),
(3010, '预算调整申请', 3000, 10, 'budgetAdjustment', 'system/fee/budget-adjustment/approval', '', 1, 0, 'C', '0', '0', 'fee:budgetAdjustment:list', 'edit', 'admin', sysdate(), '', NULL, '预算调整申请菜单'),
(3015, '操作日志', 3000, 11, 'operationLog', 'system/fee/operation-log/index', '', 1, 0, 'C', '0', '0', 'fee:operationLog:list', 'log', 'admin', sysdate(), '', NULL, '费用操作日志菜单');

-- ----------------------------
-- 补充按钮权限 - 报销单管理 (menu_id: 3044-3046)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3044, '报销单删除', 3004, 4, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expense:remove', '#', 'admin', sysdate(), '', NULL, ''),
(3045, '报销单审批', 3004, 5, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expense:approve', '#', 'admin', sysdate(), '', NULL, ''),
(3046, '报销单修改', 3004, 6, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expense:edit', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 补充按钮权限 - 超预算审批 (menu_id: 3053)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3053, '超预算审批操作', 3005, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:approval:edit', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 预留预算管理 (menu_id: 3081-3085)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3081, '预留预算查询', 3009, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:reservedBudget:query', '#', 'admin', sysdate(), '', NULL, ''),
(3082, '预留预算新增', 3009, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:reservedBudget:add', '#', 'admin', sysdate(), '', NULL, ''),
(3083, '预留预算修改', 3009, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:reservedBudget:edit', '#', 'admin', sysdate(), '', NULL, ''),
(3084, '预留预算删除', 3009, 4, '#', '', '', 1, 0, 'F', '0', '0', 'fee:reservedBudget:remove', '#', 'admin', sysdate(), '', NULL, ''),
(3085, '预留预算总览', 3009, 5, '#', '', '', 1, 0, 'F', '0', '0', 'fee:reservedBudget:overview', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 预算调整申请 (menu_id: 3091-3095)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3091, '预算调整查询', 3010, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjustment:query', '#', 'admin', sysdate(), '', NULL, ''),
(3092, '预算调整申请', 3010, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjustment:apply', '#', 'admin', sysdate(), '', NULL, ''),
(3093, '预算调整审批', 3010, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjustment:approve', '#', 'admin', sysdate(), '', NULL, ''),
(3094, '预算调整驳回', 3010, 4, '#', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjustment:reject', '#', 'admin', sysdate(), '', NULL, ''),
(3095, '预算调整转审', 3010, 5, '#', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjustment:escalate', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 按钮权限 - 操作日志 (menu_id: 3101-3102)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3101, '操作日志查询', 3015, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:operationLog:query', '#', 'admin', sysdate(), '', NULL, ''),
(3102, '操作日志列表', 3015, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:operationLog:list', '#', 'admin', sysdate(), '', NULL, '');

-- ----------------------------
-- 二级菜单 - 报销单审批 (menu_id: 3016)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3016, '报销单审批', 3000, 12, 'expenseApproval', 'system/fee/expense-approval/index', '', 1, 0, 'C', '0', '0', 'fee:expenseApproval:list', 'checkbox', 'admin', sysdate(), '', NULL, '报销单审批菜单（部门领导查看待审批报销单）');

-- ----------------------------
-- 按钮权限 - 报销单审批 (menu_id: 3111-3113)
-- ----------------------------
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark) VALUES
(3111, '报销单审批查询', 3016, 1, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expenseApproval:query', '#', 'admin', sysdate(), '', NULL, ''),
(3112, '报销单审批通过', 3016, 2, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expenseApproval:approve', '#', 'admin', sysdate(), '', NULL, ''),
(3113, '报销单审批驳回', 3016, 3, '#', '', '', 1, 0, 'F', '0', '0', 'fee:expenseApproval:reject', '#', 'admin', sysdate(), '', NULL, '');
