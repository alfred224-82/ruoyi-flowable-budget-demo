-- ----------------------------
-- AI对话式报表生成菜单SQL
-- 说明：menu_type: M=目录, C=菜单, F=按钮
-- ----------------------------

-- ========== 一级目录：AI应用 ==========
INSERT INTO sys_menu VALUES (2040, 'AI应用', '0', '7', 'ai', NULL, '', 1, 0, 'M', '0', '0', '', 'chat', 'admin', sysdate(), '', NULL, 'AI应用目录');

-- ========== 二级菜单：AI对话 ==========
INSERT INTO sys_menu VALUES (2041, 'AI对话', '2040', '1', 'chat', 'system/ai/chat', '', 1, 0, 'C', '0', '0', 'ai:chat:send', 'message', 'admin', sysdate(), '', NULL, 'AI对话式报表生成菜单');

-- AI对话按钮权限
INSERT INTO sys_menu VALUES (2042, '发送对话', '2041', '1', '', '', '', 1, 0, 'F', '0', '0', 'ai:chat:send',   '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2043, '流式对话', '2041', '2', '', '', '', 1, 0, 'F', '0', '0', 'ai:chat:stream', '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2044, '清除会话', '2041', '3', '', '', '', 1, 0, 'F', '0', '0', 'ai:chat:clear',  '#', 'admin', sysdate(), '', NULL, '');
INSERT INTO sys_menu VALUES (2045, '状态检查', '2041', '4', '', '', '', 1, 0, 'F', '0', '0', 'ai:chat:status', '#', 'admin', sysdate(), '', NULL, '');
