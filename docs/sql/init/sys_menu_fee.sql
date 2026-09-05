-- ============================================================
-- 费用管理模块菜单SQL（3个版本）
-- 菜单ID范围：2060-2076（基础版核心）、2080-2102（高级版）、2110-2118（企业版）、2120-2137（预算编制）
-- 执行前请确认sys_menu表中不存在相同menu_id的记录
-- ============================================================

-- ============================================================
-- 版本一：基础版（核心费用管理）
-- 包含：预算编制、预算审核、预算科目、费用报销、预算调整、资金池管理、系统初始化
-- ============================================================

-- 费用管理目录
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2060, '费用管理', 0, 8, 'fee', NULL, '', 1, 0, 'M', '0', '0', '', 'shopping', 'admin', NOW(), '', NULL, '费用管理目录');

-- 预算编制
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2120, '预算编制', 2060, 0, 'preparation', 'system/preparation/index', '', 1, 0, 'C', '0', '0', 'system:preparation:list', 'edit', 'admin', NOW(), '', NULL, '预算编制菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2121, '编制查询', 2120, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2122, '编制新增', 2120, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2123, '编制修改', 2120, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2124, '编制删除', 2120, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2125, '编制导出', 2120, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:export', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2126, '编制提交', 2120, 6, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:submit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2127, '编制审批', 2120, 7, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:approve', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2128, '编制驳回', 2120, 8, '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:reject', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2129, '编制向导', 2120, 9, 'wizard', 'system/preparation/wizard', '', 1, 0, 'C', '1', '0', 'system:preparation:edit', '#', 'admin', NOW(), '', NULL, '预算编制向导页面');

-- 预算审核
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2130, '预算审核', 2060, 1, 'budgetApproval', 'system/preparation/approval', '', 1, 0, 'C', '0', '0', 'system:preparation:list', 'guide', 'admin', NOW(), '', NULL, '预算审核菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2131, '审核详情', 2130, 1, 'approvalDetail', 'system/preparation/approvalDetail', '', 1, 0, 'C', '1', '0', 'system:preparation:query', '#', 'admin', NOW(), '', NULL, '预算审核详情页面');

-- 预算科目
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2132, '预算科目', 2060, 2, 'subject', 'system/subject/index', '', 1, 0, 'C', '0', '0', 'system:subject:list', 'tree', 'admin', NOW(), '', NULL, '预算科目菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2133, '科目查询', 2132, 1, '', '', '', 1, 0, 'F', '0', '0', 'system:subject:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2134, '科目新增', 2132, 2, '', '', '', 1, 0, 'F', '0', '0', 'system:subject:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2135, '科目修改', 2132, 3, '', '', '', 1, 0, 'F', '0', '0', 'system:subject:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2136, '科目删除', 2132, 4, '', '', '', 1, 0, 'F', '0', '0', 'system:subject:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2137, '科目导出', 2132, 5, '', '', '', 1, 0, 'F', '0', '0', 'system:subject:export', '#', 'admin', NOW(), '', NULL, '');

-- 费用报销
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2061, '费用报销', 2060, 3, 'expense', 'system/fee/expense/index', '', 1, 0, 'C', '0', '0', 'fee:expense:list', 'money', 'admin', NOW(), '', NULL, '费用报销菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2062, '报销查询', 2061, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:expense:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2063, '报销新增', 2061, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:expense:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2064, '报销修改', 2061, 3, '', '', '', 1, 0, 'F', '0', '0', 'fee:expense:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2065, '报销删除', 2061, 4, '', '', '', 1, 0, 'F', '0', '0', 'fee:expense:remove', '#', 'admin', NOW(), '', NULL, '');

-- 预算调整申请
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2066, '预算调整', 2060, 4, 'budgetAdjustment', 'system/fee/budget-adjustment/index', '', 1, 0, 'C', '0', '0', 'fee:budgetAdjust:list', 'swap', 'admin', NOW(), '', NULL, '预算调整申请菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2067, '调整查询', 2066, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjust:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2068, '调整申请', 2066, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjust:apply', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2069, '调整审批', 2066, 3, '', '', '', 1, 0, 'F', '0', '0', 'fee:budgetAdjust:approve', '#', 'admin', NOW(), '', NULL, '');

-- 资金池管理
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2070, '资金池管理', 2060, 5, 'reservedBudget', 'system/fee/reserved-budget/index', '', 1, 0, 'C', '0', '0', 'fee:reserved:list', 'wallet', 'admin', NOW(), '', NULL, '资金池管理菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2071, '资金池查询', 2070, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:reserved:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2072, '资金池新增', 2070, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:reserved:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2073, '资金池修改', 2070, 3, '', '', '', 1, 0, 'F', '0', '0', 'fee:reserved:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2074, '资金池删除', 2070, 4, '', '', '', 1, 0, 'F', '0', '0', 'fee:reserved:remove', '#', 'admin', NOW(), '', NULL, '');

-- 系统初始化配置
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2075, '系统初始化', 2060, 99, 'init', 'system/fee/init/index', '', 1, 0, 'C', '0', '0', 'fee:init:list', 'edit', 'admin', NOW(), '', NULL, '系统初始化配置菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2076, '初始化配置编辑', 2075, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:init:edit', '#', 'admin', NOW(), '', NULL, '');

-- ============================================================
-- 版本二：高级版（在基础版上增加：报销审批、超预算审批、成本中心、映射规则、用途标签、操作日志）
-- 执行基础版SQL后，再执行以下SQL
-- ============================================================

-- 报销审批
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2080, '报销审批', 2060, 4, 'expenseApproval', 'system/fee/expense-approval/index', '', 1, 0, 'C', '0', '0', 'fee:expenseApproval:list', 'guide', 'admin', NOW(), '', NULL, '报销审批菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2081, '审批查询', 2080, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:expenseApproval:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2082, '审批操作', 2080, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:expenseApproval:approve', '#', 'admin', NOW(), '', NULL, '');

-- 超预算审批
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2083, '超预算审批', 2060, 5, 'overBudget', 'system/fee/over-budget/index', '', 1, 0, 'C', '0', '0', 'fee:overBudget:list', 'warning', 'admin', NOW(), '', NULL, '超预算审批菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2084, '超预算查询', 2083, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:overBudget:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2085, '超预算审批操作', 2083, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:overBudget:approve', '#', 'admin', NOW(), '', NULL, '');

-- 成本中心
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2086, '成本中心', 2060, 6, 'costCenter', 'system/fee/costCenter/index', '', 1, 0, 'C', '0', '0', 'fee:costCenter:list', 'tree', 'admin', NOW(), '', NULL, '成本中心菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2087, '成本中心查询', 2086, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2088, '成本中心新增', 2086, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2089, '成本中心修改', 2086, 3, '', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2090, '成本中心删除', 2086, 4, '', '', '', 1, 0, 'F', '0', '0', 'fee:costCenter:remove', '#', 'admin', NOW(), '', NULL, '');

-- 映射规则
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2091, '映射规则', 2060, 7, 'mappingRule', 'system/fee/mappingRule/index', '', 1, 0, 'C', '0', '0', 'fee:mappingRule:list', 'link', 'admin', NOW(), '', NULL, '映射规则菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2092, '映射规则查询', 2091, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2093, '映射规则新增', 2091, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2094, '映射规则修改', 2091, 3, '', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2095, '映射规则删除', 2091, 4, '', '', '', 1, 0, 'F', '0', '0', 'fee:mappingRule:remove', '#', 'admin', NOW(), '', NULL, '');

-- 用途标签
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2096, '用途标签', 2060, 8, 'purposeTag', 'system/fee/purposeTag/index', '', 1, 0, 'C', '0', '0', 'fee:purposeTag:list', 'tag', 'admin', NOW(), '', NULL, '用途标签菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2097, '标签查询', 2096, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2098, '标签新增', 2096, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2099, '标签修改', 2096, 3, '', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2100, '标签删除', 2096, 4, '', '', '', 1, 0, 'F', '0', '0', 'fee:purposeTag:remove', '#', 'admin', NOW(), '', NULL, '');

-- 操作日志
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2101, '操作日志', 2060, 9, 'operationLog', 'system/fee/operation-log/index', '', 1, 0, 'C', '0', '0', 'fee:operationLog:list', 'log', 'admin', NOW(), '', NULL, '费用操作日志菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2102, '日志查询', 2101, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:operationLog:query', '#', 'admin', NOW(), '', NULL, '');

-- ============================================================
-- 版本三：企业版（在高级版上增加：报表分析、AI提示词配置、预算审批）
-- 执行基础版+高级版SQL后，再执行以下SQL
-- ============================================================

-- 报表分析
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2110, '报表分析', 2060, 10, 'report', 'system/fee/report/index', '', 1, 0, 'C', '0', '0', 'fee:report:list', 'chart', 'admin', NOW(), '', NULL, '费用报表分析菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2111, '报表查询', 2110, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:report:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2112, '报表导出', 2110, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:report:export', '#', 'admin', NOW(), '', NULL, '');

-- AI提示词配置
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2113, 'AI提示词', 2060, 11, 'aiPrompt', 'system/fee/ai-prompt/index', '', 1, 0, 'C', '0', '0', 'fee:aiPrompt:list', 'message', 'admin', NOW(), '', NULL, 'AI提示词配置菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2114, 'AI提示词查询', 2113, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:aiPrompt:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2115, 'AI提示词编辑', 2113, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:aiPrompt:edit', '#', 'admin', NOW(), '', NULL, '');

-- 预算审批（费用模块内的审批入口）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2116, '预算审批', 2060, 12, 'approval', 'system/fee/approval/index', '', 1, 0, 'C', '0', '0', 'fee:approval:list', 'guide', 'admin', NOW(), '', NULL, '预算审批菜单');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2117, '审批查询', 2116, 1, '', '', '', 1, 0, 'F', '0', '0', 'fee:approval:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (2118, '审批操作', 2116, 2, '', '', '', 1, 0, 'F', '0', '0', 'fee:approval:approve', '#', 'admin', NOW(), '', NULL, '');
