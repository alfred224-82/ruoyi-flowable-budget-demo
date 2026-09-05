-- =====================================================================
-- M1_04 sys_menu 全量菜单脚本（T1.3）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《各角色菜单权限文档》（v2.1）第二节 2.2~2.5
-- 规范：
--   1. 先删后插（按 menu_id 精确删除），脚本可重复执行
--   2. 新增页面菜单全部挂既有目录 费用管控(menu_id=3000)（实施说明第1条）
--   3. 权限标识(perms)与文档完全一致；菜单与版本解耦，授权见 M2_02 模板
--   4. 既有 2000/3000 系列菜单沿用不动；3114/3124/3125 为文档 2.1/2.3 定义的隐藏页
-- =====================================================================

DELETE FROM `sys_menu` WHERE `menu_id` IN (
    3114, 3124, 3125,
    3201, 3202, 3203, 3204, 3205, 3206, 3207, 3208, 3209, 3210,
    3211, 3212, 3213, 3214, 3215, 3216, 3217, 3218, 3219, 3220,
    3221, 3222, 3223, 3224, 3225, 3226,
    3231, 3232, 3233,
    3241, 3242, 3243, 3244, 3245, 3246, 3247, 3248, 3249, 3250,
    3251, 3252, 3253, 3254, 3255, 3256, 3257, 3258, 3259, 3260,
    3261, 3262, 3263, 3264, 3265, 3266, 3267, 3268, 3269, 3270,
    3271, 3272, 3273, 3274, 3275, 3276, 3277, 3278, 3279, 3280,
    3281, 3282, 3283, 3284, 3285, 3286, 3287, 3288, 3289, 3290,
    3291, 3292, 3293, 3294, 3295, 3296, 3297, 3298, 3299,
    6006, 6007, 6021
);

-- ----------------------------
-- 消息中心（2.1：基础版+，全角色，导航栏铃铛入口，隐藏）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3114, '消息中心', 0, 99, 'message', 'system/message/index', NULL, 1, 0, 'C', '1', '0', 'system:message:list', 'message', 'admin', NOW(), '', NULL, '消息中心（隐藏，导航栏铃铛入口）');

-- ----------------------------
-- 2.2 新增页面菜单（C 类，挂费用管控目录 3000）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3201, '项目管理',     3000, 20, 'fee/project',         'fee/project/index',           NULL, 1, 0, 'C', '0', '0', 'fee:project:list',        'tree',    'admin', NOW(), '', NULL, '基础版'),
(3202, '收入确认',     3000, 21, 'fee/income',          'fee/project/income',          NULL, 1, 0, 'C', '0', '0', 'fee:income:list',         'money',   'admin', NOW(), '', NULL, '基础版'),
(3203, '合同变更',     3000, 22, 'fee/change',          'fee/project/change',          NULL, 1, 0, 'C', '0', '0', 'fee:change:list',         'edit',    'admin', NOW(), '', NULL, '基础版'),
(3204, '分成管理',     3000, 23, 'fee/sharing',         'fee/project/sharing',         NULL, 1, 0, 'C', '0', '0', 'fee:sharing:list',        'chart',   'admin', NOW(), '', NULL, '基础版'),
(3205, '资产折旧',     3000, 24, 'fee/asset',           'fee/project/asset',           NULL, 1, 0, 'C', '0', '0', 'fee:asset:list',          'component', 'admin', NOW(), '', NULL, '基础版'),
(3206, '付款单管理',   3000, 25, 'fee/payment',         'fee/payment/index',           NULL, 1, 0, 'C', '0', '0', 'fee:payment:list',        'form',    'admin', NOW(), '', NULL, '基础版'),
(3207, '收款单管理',   3000, 26, 'fee/receipt',         'fee/receipt/index',           NULL, 1, 0, 'C', '0', '0', 'fee:receipt:list',        'money',   'admin', NOW(), '', NULL, '基础版'),
(3208, '烂账管理',     3000, 27, 'fee/baddebt',         'fee/baddebt/index',           NULL, 1, 0, 'C', '0', '0', 'fee:baddebt:list',        'monitor', 'admin', NOW(), '', NULL, '基础版'),
(3209, '凭证导出',     3000, 28, 'fee/export',          'fee/export/index',            NULL, 1, 0, 'C', '0', '0', 'fee:export:list',         'download', 'admin', NOW(), '', NULL, '基础版(专业版页内增加二维导出)'),
(3210, '收入核验',     3000, 29, 'fee/incomeVerify',    'fee/income/verify',           NULL, 1, 0, 'C', '0', '0', 'fee:income:verify',       'checkbox', 'admin', NOW(), '', NULL, '基础版'),
(3211, '薪资社保',     3000, 30, 'fee/salary',          'fee/config/salary',           NULL, 1, 0, 'C', '0', '0', 'fee:staffSalary:list',    'peoples', 'admin', NOW(), '', NULL, '基础版'),
(3212, '折旧规则',     3000, 31, 'fee/depreciationRule','fee/config/depreciation',     NULL, 1, 0, 'C', '0', '0', 'fee:depreciationRule:list', 'edit',  'admin', NOW(), '', NULL, '基础版'),
(3213, '贷款利率',     3000, 32, 'fee/loan',            'fee/config/loan',             NULL, 1, 0, 'C', '0', '0', 'fee:loan:list',           'money',   'admin', NOW(), '', NULL, '基础版'),
(3214, '分成规则配置', 3000, 33, 'fee/sharingRule',     'fee/config/sharing',          NULL, 1, 0, 'C', '0', '0', 'fee:sharingRule:list',    'edit',    'admin', NOW(), '', NULL, '基础版(老板定比例)'),
(3215, '老板看板',     3000, 34, 'fee/dashboard',       'fee/dashboard/index',         NULL, 1, 0, 'C', '0', '0', 'fee:dashboard:query',     'dashboard', 'admin', NOW(), '', NULL, '基础版(专业版页内新增团队人力分布卡片)'),
(3216, '项目关联规则', 3000, 35, 'fee/linkRule',        'fee/config/linkRule',         NULL, 1, 0, 'C', '0', '0', 'fee:linkRule:list',       'link',    'admin', NOW(), '', NULL, '基础版(高级版解锁自定义开关项)'),
(3217, '部门预算编制', 3000, 36, 'fee/deptBudget',      'fee/budget/deptPreparation',  NULL, 1, 0, 'C', '0', '0', 'fee:deptBudget:list',     'tree',    'admin', NOW(), '', NULL, '高级版 FEAT_DEPT_BUDGET'),
(3218, '项目利润报表', 3000, 37, 'fee/profitReport',    'fee/report/profitReport',     NULL, 1, 0, 'C', '0', '0', 'fee:profitReport:list',   'chart',   'admin', NOW(), '', NULL, '高级版 FEAT_PROFIT_REPORT'),
(3219, '打卡配置',     3000, 38, 'fee/clockConfig',     'fee/config/clock',            NULL, 1, 0, 'C', '0', '0', 'fee:clock:config',        'edit',    'admin', NOW(), '', NULL, '高级版 FEAT_TIME_CLOCK'),
(3220, '打卡记录',     3000, 39, 'fee/clockRecord',     'fee/clock/index',             NULL, 1, 0, 'C', '0', '0', 'fee:clock:list',          'time-range', 'admin', NOW(), '', NULL, '高级版 FEAT_TIME_CLOCK'),
(3221, '信用规则',     3000, 40, 'fee/creditRule',      'fee/config/creditRule',       NULL, 1, 0, 'C', '0', '0', 'fee:creditRule:list',     'edit',    'admin', NOW(), '', NULL, '高级版 FEAT_CREDIT_SCORE'),
(3222, '员工信用分',   3000, 41, 'fee/creditScore',     'fee/credit/score',            NULL, 1, 0, 'C', '0', '0', 'fee:credit:list',         'star',    'admin', NOW(), '', NULL, '高级版 FEAT_CREDIT_SCORE'),
(3223, '自定义模板管理', 3000, 42, 'fee/exportTemplate','fee/config/exportTemplate',   NULL, 1, 0, 'C', '0', '0', 'fee:exportTemplate:list', 'form',    'admin', NOW(), '', NULL, '高级版 FEAT_EXPORT_TEMPLATE_CUSTOM'),
(3224, '组织管理',     3000, 43, 'fee/org',             'fee/org/index',               NULL, 1, 0, 'C', '0', '0', 'fee:org:list',            'tree',    'admin', NOW(), '', NULL, '专业版 FEAT_MULTI_ORG'),
(3225, '组织预算编制', 3000, 44, 'fee/orgBudget',       'fee/budget/orgPreparation',   NULL, 1, 0, 'C', '0', '0', 'fee:orgBudget:list',      'tree-table', 'admin', NOW(), '', NULL, '专业版 FEAT_MULTI_ORG+FEAT_ORG_SNAPSHOT'),
(3226, '人脸核身配置', 3000, 45, 'fee/faceConfig',      'fee/config/face',             NULL, 1, 0, 'C', '0', '0', 'fee:face:config',         'user',    'admin', NOW(), '', NULL, '专业版 FEAT_FACE_RECOGNITION');

-- ----------------------------
-- 2.3 隐藏子页面（C 类，visible=1，仅路由注册）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3231, '项目详情', 3201, 1, 'detail',  'fee/project/detail',       NULL, 1, 0, 'C', '1', '0', 'fee:project:query', '#', 'admin', NOW(), '', NULL, '基础版隐藏页'),
(3232, '新建项目', 3201, 2, 'create',  'fee/project/create',       NULL, 1, 0, 'C', '1', '0', 'fee:project:add',   '#', 'admin', NOW(), '', NULL, '基础版隐藏页'),
(3233, '项目成员', 3201, 3, 'members', 'fee/project/members',      NULL, 1, 0, 'C', '1', '0', 'fee:project:edit',  '#', 'admin', NOW(), '', NULL, '基础版隐藏页(专业版页内显示跨项目负载提示)'),
(3124, '报销单详情', 3004, 10, 'detail', 'system/fee/expense/detail', NULL, 1, 0, 'C', '1', '0', 'fee:expense:query', '#', 'admin', NOW(), '', NULL, '基础版隐藏页'),
(3125, '报销单表单', 3004, 11, 'form',   'system/fee/expense/form',   NULL, 1, 0, 'C', '1', '0', 'fee:expense:add',   '#', 'admin', NOW(), '', NULL, '基础版隐藏页'),
(6021, '租户详情', 6001, 5, 'detail', 'system/tenant/detail', NULL, 1, 0, 'C', '1', '0', 'system:tenant:query', '#', 'admin', NOW(), '', NULL, '平台级隐藏页：用量统计/版本变更日志/功能配置');

-- ----------------------------
-- 2.4 按钮权限（F 类）—— 基础版
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
-- 项目管理（3201）
(3251, '项目查询', 3201, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:project:query',   '#', 'admin', NOW(), '', NULL, ''),
(3252, '项目新增', 3201, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:project:add',     '#', 'admin', NOW(), '', NULL, ''),
(3253, '项目编辑', 3201, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:project:edit',    '#', 'admin', NOW(), '', NULL, ''),
(3254, '项目审批', 3201, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:project:approve', '#', 'admin', NOW(), '', NULL, ''),
(3255, '关闭项目', 3201, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:project:close',   '#', 'admin', NOW(), '', NULL, ''),
-- 收入确认（3202）
(3256, '收入确认查询', 3202, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:income:query', '#', 'admin', NOW(), '', NULL, ''),
(3257, '收入确认新增', 3202, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:income:add',   '#', 'admin', NOW(), '', NULL, ''),
-- 合同变更（3203）
(3258, '变更查询', 3203, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:change:query',   '#', 'admin', NOW(), '', NULL, ''),
(3259, '发起变更', 3203, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:change:add',     '#', 'admin', NOW(), '', NULL, ''),
(3260, '变更审批', 3203, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:change:approve', '#', 'admin', NOW(), '', NULL, ''),
(3261, '收款确认', 3203, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:change:confirm', '#', 'admin', NOW(), '', NULL, ''),
-- 分成管理（3204）
(3262, '分成查询', 3204, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:sharing:query', '#', 'admin', NOW(), '', NULL, ''),
(3263, '分成配置', 3204, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:sharing:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 资产折旧（3205）
(3264, '资产查询', 3205, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:asset:query', '#', 'admin', NOW(), '', NULL, ''),
(3265, '资产维护', 3205, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:asset:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 付款单管理（3206）
(3266, '付款单查询', 3206, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:payment:query',   '#', 'admin', NOW(), '', NULL, ''),
(3267, '付款单新增', 3206, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:payment:add',     '#', 'admin', NOW(), '', NULL, ''),
(3268, '付款单审批', 3206, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:payment:approve', '#', 'admin', NOW(), '', NULL, ''),
(3269, '付款单驳回', 3206, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:payment:reject',  '#', 'admin', NOW(), '', NULL, ''),
-- 收款单管理（3207）
(3270, '收款单查询', 3207, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:receipt:query',   '#', 'admin', NOW(), '', NULL, ''),
(3271, '收款单新增', 3207, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:receipt:add',     '#', 'admin', NOW(), '', NULL, ''),
(3272, '收款单审核', 3207, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:receipt:approve', '#', 'admin', NOW(), '', NULL, ''),
-- 烂账管理（3208）
(3273, '烂账查询', 3208, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:baddebt:query',  '#', 'admin', NOW(), '', NULL, ''),
(3274, '烂账标记', 3208, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:baddebt:mark',   '#', 'admin', NOW(), '', NULL, ''),
(3275, '烂账跟进', 3208, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:baddebt:follow', '#', 'admin', NOW(), '', NULL, ''),
-- 凭证导出（3209）
(3276, '导出查询', 3209, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:export:query', '#', 'admin', NOW(), '', NULL, ''),
(3277, '执行导出', 3209, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:export:run',   '#', 'admin', NOW(), '', NULL, ''),
-- 收入核验（3210）
(3278, '收入核验通过', 3210, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:income:verify', '#', 'admin', NOW(), '', NULL, ''),
(3279, '收入核验驳回', 3210, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:income:reject', '#', 'admin', NOW(), '', NULL, ''),
-- 薪资社保（3211）
(3280, '薪资查询', 3211, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:staffSalary:query', '#', 'admin', NOW(), '', NULL, ''),
(3281, '薪资维护', 3211, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:staffSalary:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 折旧规则（3212）
(3282, '折旧规则查询', 3212, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:depreciationRule:query', '#', 'admin', NOW(), '', NULL, ''),
(3283, '折旧规则配置', 3212, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:depreciationRule:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 贷款利率（3213）
(3284, '贷款查询', 3213, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:loan:query', '#', 'admin', NOW(), '', NULL, ''),
(3285, '贷款维护', 3213, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:loan:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 分成规则配置（3214）
(3286, '分成规则查询', 3214, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:sharingRule:query', '#', 'admin', NOW(), '', NULL, ''),
(3287, '分成规则配置', 3214, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:sharingRule:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 老板看板（3215）
(3288, '看板查询', 3215, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:dashboard:query', '#', 'admin', NOW(), '', NULL, ''),
-- 项目关联规则（3216）
(3289, '关联规则查询', 3216, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:linkRule:query', '#', 'admin', NOW(), '', NULL, ''),
(3290, '关联规则配置', 3216, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:linkRule:edit',  '#', 'admin', NOW(), '', NULL, '高级版解锁大类强制/项目严格模式');

-- ----------------------------
-- 2.4 按钮权限（F 类）—— 高级版增量
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3241, '打卡规则维护', 3219, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:clock:edit',        '#', 'admin', NOW(), '', NULL, '高级版'),
(3242, '打卡记录查询', 3220, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:clock:query',       '#', 'admin', NOW(), '', NULL, '高级版'),
(3243, '信用规则查询', 3221, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:creditRule:query',  '#', 'admin', NOW(), '', NULL, '高级版'),
(3244, '信用规则配置', 3221, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:creditRule:edit',   '#', 'admin', NOW(), '', NULL, '高级版'),
(3245, '信用分查询',   3222, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:credit:query',      '#', 'admin', NOW(), '', NULL, '高级版'),
(3246, '信用分调整',   3222, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:credit:edit',       '#', 'admin', NOW(), '', NULL, '高级版'),
(3247, '模板查询',     3223, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:exportTemplate:query', '#', 'admin', NOW(), '', NULL, '高级版'),
(3248, '模板维护',     3223, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:exportTemplate:edit',  '#', 'admin', NOW(), '', NULL, '高级版'),
(3291, '部门预算查询', 3217, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:deptBudget:query',  '#', 'admin', NOW(), '', NULL, '高级版'),
(3292, '部门预算编制', 3217, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:deptBudget:edit',   '#', 'admin', NOW(), '', NULL, '高级版'),
(3293, '利润报表查询', 3218, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:profitReport:query', '#', 'admin', NOW(), '', NULL, '高级版'),
(3296, '影响分析',     3003, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:mappingRule:impact',  '#', 'admin', NOW(), '', NULL, '高级版 FEAT_RULE_ENGINE_CUSTOM'),
(3297, '规则发布',     3003, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:mappingRule:publish', '#', 'admin', NOW(), '', NULL, '高级版(灰度发布=专业版)');

-- ----------------------------
-- 2.4 按钮权限（F 类）—— 专业版增量
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3249, '组织维护',     3224, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:org:edit',           '#', 'admin', NOW(), '', NULL, '专业版'),
(3250, '组织预算查询', 3225, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:orgBudget:query',    '#', 'admin', NOW(), '', NULL, '专业版'),
(3294, '团队人力分布', 3215, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:dashboard:memberLoad', '#', 'admin', NOW(), '', NULL, '专业版 FEAT_MEMBER_LOAD'),
(3295, '二维导出',     3209, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:export:run2d',       '#', 'admin', NOW(), '', NULL, '专业版 FEAT_EXPORT_2D'),
(3298, '组织快照管理', 3225, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:orgBudget:snapshot', '#', 'admin', NOW(), '', NULL, '专业版 FEAT_ORG_SNAPSHOT'),
(3299, '人脸配置维护', 3226, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:face:edit',          '#', 'admin', NOW(), '', NULL, '专业版');

-- ----------------------------
-- 2.5 管理员专属菜单细分（平台级，自 6004 细分）
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(6006, '版本分配',     6001, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenant:assign', '#', 'admin', NOW(), '', NULL, '选择BASIC/ADVANCED/PROFESSIONAL，自动记录版本变更日志'),
(6007, '使用天数配置', 6001, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenant:period', '#', 'admin', NOW(), '', NULL, '授权类型TRIAL/FORMAL+天数→换算写入expire_date');
