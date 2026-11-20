-- =====================================================================
-- M10_02 平台运营菜单补建（单店覆盖 / 访问通道 / 计费合规收纳）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档》v1.16 §5.4 / §5.5 / §5.7
-- 背景：
--   管理员操作手册第五章描述了三个平台运营功能，但菜单缺失或未收纳：
--   1. §5.4 单店覆盖（tenant_biz_config）：后端 Controller 已交付（perms
--      system:tenantBizConfig:*），表由 M1_05 建立，但无任何菜单入口。
--   2. §5.5 访问通道管理（access_channel）：消息 Provider 三通道已交付
--      （M13_01 biz_message_log），但系统管理部署路径无管理菜单。
--   3. §5.7 计费套餐管理（M14）：M14_01 已建 11 张表 + 菜单 6120~6167，
--      但菜单 parent_id=0（顶级），未收纳到「资金平台管理菜单」(1300) 下。
-- 本脚本：
--   1. 新建 单店覆盖 菜单（1340~1342，挂 1300 下）
--   2. 新建 访问通道管理 菜单（1343~1345，挂 1300 下）
--   3. Re-parent 计费菜单 6120 + 合规菜单 6160 到 1300（UPDATE 幂等）
--   4. 全部新菜单授权超管 role_id=1
-- 前置依赖：M15_12（1300 目录已建）、M14_01（6120/6160 菜单已建）
-- =====================================================================

-- ----------------------------
-- 1. 单店覆盖管理（§5.4，tenant_biz_config，T10.4）
--    后端：TenantBizConfigController（/system/tenantBizConfig）
--    perms：system:tenantBizConfig:query / system:tenantBizConfig:edit
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1340, '单店覆盖',     1300, 7, 'tenantBizConfig', 'system/tenantBizConfig/index', NULL, 1, 0, 'C', '0', '0', 'system:tenantBizConfig:query', 'pc', 'edit', 'admin', NOW(), '租户业态单店覆盖配置（module_overrides JSON；合并优先级=租户覆盖>业态包默认）'),
(1341, '覆盖查询',     1340, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenantBizConfig:query', 'pc', '#', 'admin', NOW(), ''),
(1342, '覆盖编辑',     1340, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenantBizConfig:edit',  'pc', '#', 'admin', NOW(), '保存前 JSON Schema 校验（非法 JSON 拦截）');

-- ----------------------------
-- 2. 访问通道管理（§5.5，access_channel，T13.1~T13.6）
--    后端：MessageController（消息记录查询）+ IMessageProvider 三通道分发
--    perms：system:message:list / system:message:query
-- ----------------------------
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1343, '访问通道管理', 1300, 8, 'channel', 'system/message/index', NULL, 1, 0, 'C', '0', '0', 'system:message:list', 'pc', 'connection', 'admin', NOW(), '消息通道管理（wechat/wecom/open_api 三通道；消息记录/收款配置）'),
(1344, '消息查询',     1343, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:query',  'pc', '#', 'admin', NOW(), ''),
(1345, '消息详情',     1343, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:message:detail', 'pc', '#', 'admin', NOW(), '');

-- ----------------------------
-- 3. Re-parent 计费菜单 + 合规菜单到「资金平台管理菜单」(1300)
--    M14_01 建的 6120(平台计费) 和 6160(合规管理) 原 parent_id=0，
--    按 M15_12 收纳规则统一归入 1300 下（UPDATE 幂等：仅当 parent_id<>1300 时更新）
-- ----------------------------
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 9  WHERE `menu_id` = 6120 AND `parent_id` <> 1300;
UPDATE `sys_menu` SET `parent_id` = 1300, `order_num` = 10 WHERE `menu_id` = 6160 AND `parent_id` <> 1300;

-- ----------------------------
-- 4. 授予超管 role_id=1（新增菜单 + re-parent 后的菜单）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(1, 1340), (1, 1341), (1, 1342),
(1, 1343), (1, 1344), (1, 1345),
(1, 6120), (1, 6160);

-- ----------------------------
-- 5. 验收查询
-- ----------------------------
-- 5.1 新增菜单已创建
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu
--  WHERE menu_id IN (1340,1341,1342,1343,1344,1345);
-- 预期：6 行，parent_id 均 = 1300

-- 5.2 计费/合规菜单已收纳到 1300 下
-- SELECT menu_id, menu_name, parent_id, order_num FROM sys_menu
--  WHERE menu_id IN (6120, 6160);
-- 预期：parent_id=1300，order_num=9/10

-- 5.3 资金平台管理菜单完整清单（预期 12+ 项）
-- SELECT menu_id, menu_name, parent_id, order_num, perms FROM sys_menu
--  WHERE parent_id = 1300 ORDER BY order_num;
-- 预期含：租户管理/标准角色/行业模板/菜单管理/角色模板/版本档位/
--        单店覆盖/访问通道/平台计费/合规管理

-- 5.4 超管授权完整
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1
--  AND menu_id IN (1340,1341,1342,1343,1344,1345,6120,6160);
-- 预期：8
