-- ----------------------------
-- 预算管理菜单SQL
-- 说明：menu_type: M=目录, C=菜单, F=按钮
-- ----------------------------

-- ========== 一级目录：预算管理 ==========
INSERT INTO sys_menu VALUES (2000, '预算管理', '0', '6', 'budget', NULL, '', 1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), '', NULL, '预算管理目录');

-- ========== 二级菜单：预算编制 ==========
INSERT INTO sys_menu VALUES (2001, '预算编制', '2000', '1', 'preparation', 'system/preparation/index', '', 1, 0, 'C', '0', '0', 'system:preparation:list', 'edit', 'admin', sysdate(), '', NULL, '预算编制菜单');

-- 预算编制按钮权限
INSERT INTO sys_menu VALUES (2011, '编制查询', '2001', '1', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:query',  '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2012, '编制新增', '2001', '2', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:add',   '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2013, '编制修改', '2001', '3', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:edit',  '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2014, '编制删除', '2001', '4', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2015, '编制导出', '2001', '5', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:export', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2016, '编制提交', '2001', '6', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:submit', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2017, '编制审批', '2001', '7', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:approve', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2018, '编制驳回', '2001', '8', '', '', '', 1, 0, 'F', '0', '0', 'system:preparation:reject', '#', 'admin', sysdate(), '', NULL, '');

-- 隐藏菜单：预算编制向导（不显示在菜单中，通过按钮跳转）
INSERT INTO sys_menu VALUES (2019, '编制向导', '2001', '9', 'wizard', 'system/preparation/wizard', '', 1, 0, 'C', '1', '0', 'system:preparation:edit', '#', 'admin', sysdate(), '', NULL, '预算编制向导页面');

-- ========== 二级菜单：预算审核 ==========
INSERT INTO sys_menu VALUES (2002, '预算审核', '2000', '2', 'approval', 'system/preparation/approval', '', 1, 0, 'C', '0', '0', 'system:preparation:list', 'guide', 'admin', sysdate(), '', NULL, '预算审核菜单');

-- 隐藏菜单：预算审核详情（不显示在菜单中，通过按钮跳转）
INSERT INTO sys_menu VALUES (2020, '审核详情', '2002', '1', 'approvalDetail', 'system/preparation/approvalDetail', '', 1, 0, 'C', '1', '0', 'system:preparation:query', '#', 'admin', sysdate(), '', NULL, '预算审核详情页面');

-- ========== 二级菜单：预算科目 ==========
INSERT INTO sys_menu VALUES (2003, '预算科目', '2000', '3', 'subject', 'system/subject/index', '', 1, 0, 'C', '0', '0', 'system:subject:list', 'tree', 'admin', sysdate(), '', NULL, '预算科目菜单');

-- 预算科目按钮权限
INSERT INTO sys_menu VALUES (2031, '科目查询', '2003', '1', '', '', '', 1, 0, 'F', '0', '0', 'system:subject:query',  '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2032, '科目新增', '2003', '2', '', '', '', 1, 0, 'F', '0', '0', 'system:subject:add',    '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2033, '科目修改', '2003', '3', '', '', '', 1, 0, 'F', '0', '0', 'system:subject:edit',   '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2034, '科目删除', '2003', '4', '', '', '', 1, 0, 'F', '0', '0', 'system:subject:remove', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2035, '科目导出', '2003', '5', '', '', '', 1, 0, 'F', '0', '0', 'system:subject:export', '#', 'admin', sysdate(), '', NULL, '');
