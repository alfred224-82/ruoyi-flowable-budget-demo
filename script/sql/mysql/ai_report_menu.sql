-- ----------------------------
-- AI智能报表菜单SQL
-- 说明：menu_type: M=目录, C=菜单, F=按钮
-- 挂在 AI应用(2040) 目录下
-- ----------------------------

-- ========== 二级菜单：AI智能报表 ==========
INSERT INTO sys_menu VALUES (2046, 'AI智能报表', '2040', '2', 'report', 'system/ai/report', '', 1, 0, 'C', '0', '0', 'system:preparation:query', 'chart', 'admin', sysdate(), '', NULL, 'AI智能报表生成菜单');

-- 按钮权限
INSERT INTO sys_menu VALUES (2047, '报表生成', '2046', '1', '', '', '', 1, 0, 'F', '0', '0', 'ai:report:generate', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2048, '报表历史', '2046', '2', '', '', '', 1, 0, 'F', '0', '0', 'ai:report:history',  '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2049, '报表详情', '2046', '3', '', '', '', 1, 0, 'F', '0', '0', 'ai:report:detail',  '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2050, '报表删除', '2046', '4', '', '', '', 1, 0, 'F', '0', '0', 'ai:report:remove',  '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2051, '报表类型', '2046', '5', '', '', '', 1, 0, 'F', '0', '0', 'ai:report:types',   '#', 'admin', sysdate(), '', NULL, '');
