-- =====================================================================
-- M15_06 sys_menu.client_type 扩展 + 小微版全量菜单脚本（T15.6，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第五章（端×角色菜单清单）/ 第六章（单小程序架构）
--       《资金管控系统开发任务文档》v1.13 T15.6
--       《多业态SaaS平台需求规格说明书》v1.0 第 14 章（单小程序架构与 client_type 菜单过滤）
-- 前置依赖：M1_04_sys_menu.sql（sys_menu 已建）
-- 规范：
--   1. client_type 补列走 INFORMATION_SCHEMA 条件判断（幂等）；存量菜单回填 'pc'
--   2. 小微版菜单先删后插（menu_id 占用 7000~7499 独立区段，与企业版 2000~6179 不冲突）
--   3. client_type 取值：pc(PC 后台)/mini(微信小程序)/both(两端共用)；为菜单端过滤唯一依据
--   4. perms 前缀统一 micro:*（admin 行业/角色模板管理用 system:industry:* / system:roleTemplate:*）
--   5. ★V1 零支付：顾客端【无 micro:customer:pay 菜单】，仅有 micro:customer:payInfo（收款方式提示）
--   6. 老板小程序菜单仅含「报表+定价+提醒」(client_type='mini')，看不到结算/用户管理
--   7. 全部 micro 菜单授予超管 role_id=1 便于 admin 管理（业务角色授权由 role_template 驱动，见 M15_07）
-- =====================================================================

-- ----------------------------
-- 1. sys_menu 增加 client_type 列（幂等条件 ALTER；默认 both，存量回填 pc）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_06_add_client_type`;
DELIMITER $$
CREATE PROCEDURE `m15_06_add_client_type`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu' AND COLUMN_NAME = 'client_type') THEN
        ALTER TABLE `sys_menu`
            ADD COLUMN `client_type` VARCHAR(10) NOT NULL DEFAULT 'both'
                COMMENT '菜单端类型：pc(PC后台)/mini(微信小程序)/both(两端共用)' AFTER `perms`;
        -- 存量菜单全部为 PC 后台菜单，回填 pc（共用小程序引导态由 perms 驱动，不依赖菜单树）
        UPDATE `sys_menu` SET `client_type` = 'pc' WHERE `menu_id` < 7000;
    END IF;
END$$
DELIMITER ;
CALL m15_06_add_client_type();
DROP PROCEDURE IF EXISTS `m15_06_add_client_type`;

-- ----------------------------
-- 2. 小微版菜单（先删后插幂等，menu_id 7000~7499）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7000 AND 7499;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 7000 AND 7499;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- ===== 2.1 admin 行业模板/角色模板管理（7000~7009，PC，挂系统管理下）=====
(7000, '行业模板管理', 1, 20, 'industry', 'micro/industry/index', NULL, 1, 0, 'C', '0', '0', 'system:industry:list', 'pc', 'tree-table', 'admin', NOW(), '小微版专属：行业模板 CRUD + 角色映射'),
(7001, '新建模板',     7000, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:industry:add', 'pc', '#', 'admin', NOW(), ''),
(7002, '编辑模板',     7000, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:industry:edit', 'pc', '#', 'admin', NOW(), ''),
(7003, '配置角色映射', 7000, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:industry:roleMap', 'pc', '#', 'admin', NOW(), '业务角色→标准角色(助教→salesman)'),
(7005, '角色模板管理', 1, 21, 'roleTemplate', 'micro/roleTemplate/index', NULL, 1, 0, 'C', '0', '0', 'system:roleTemplate:list', 'pc', 'list', 'admin', NOW(), '菜单+区块配方(version_type×role_code)'),
(7006, '新建模板',     7005, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:roleTemplate:add', 'pc', '#', 'admin', NOW(), ''),
(7007, '绑定菜单',     7005, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:roleTemplate:bindMenu', 'pc', '#', 'admin', NOW(), ''),

-- ===== 2.2 平台管理员 PC：店铺配置（7010~7062，client_type=pc）=====
(7010, '店铺配置', 0, 30, 'microConfig', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'system', 'admin', NOW(), '平台管理员：用户/角色/服务定价/基础数据/系统设置'),
(7011, '用户管理', 7010, 1, 'user', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'user', 'admin', NOW(), ''),
(7012, '用户列表', 7011, 1, 'list', 'micro/user/index', NULL, 1, 0, 'C', '0', '0', 'micro:user:list', 'pc', '#', 'admin', NOW(), ''),
(7013, '新建用户', 7012, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:user:add', 'pc', '#', 'admin', NOW(), ''),
(7014, '编辑用户', 7012, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:user:edit', 'pc', '#', 'admin', NOW(), ''),
(7015, '重置密码', 7012, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:user:resetPwd', 'pc', '#', 'admin', NOW(), ''),
(7016, '禁用用户', 7012, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:user:disable', 'pc', '#', 'admin', NOW(), ''),
(7017, '角色分配', 7011, 2, 'role', 'micro/userRole/index', NULL, 1, 0, 'C', '0', '0', 'micro:userRole:list', 'pc', '#', 'admin', NOW(), '用户→业务角色(写 user_tenant_role)'),
(7018, '分配角色', 7017, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:userRole:assign', 'pc', '#', 'admin', NOW(), ''),
(7019, '取消分配', 7017, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:userRole:remove', 'pc', '#', 'admin', NOW(), ''),
(7020, '业务角色管理', 7010, 2, 'tenantRole', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'peoples', 'admin', NOW(), '角色映射：挑选+改名+启停(写 tenant_role_mapping)'),
(7021, '角色列表', 7020, 1, 'list', 'micro/tenantRole/index', NULL, 1, 0, 'C', '0', '0', 'micro:tenantRole:list', 'pc', '#', 'admin', NOW(), ''),
(7022, '修改角色名称', 7021, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:tenantRole:rename', 'pc', '#', 'admin', NOW(), '助教→主持人'),
(7023, '启用/禁用角色', 7021, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:tenantRole:toggle', 'pc', '#', 'admin', NOW(), ''),
(7030, '服务与定价', 7010, 3, 'pricing', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'money', 'admin', NOW(), '核心配置'),
(7031, '服务分类管理', 7030, 1, 'category', 'micro/category/index', NULL, 1, 0, 'C', '0', '0', 'micro:category:list', 'pc', '#', 'admin', NOW(), '房间/助教/蔬菜/肉菜'),
(7032, '新建分类', 7031, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:category:add', 'pc', '#', 'admin', NOW(), ''),
(7033, '编辑分类', 7031, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:category:edit', 'pc', '#', 'admin', NOW(), ''),
(7034, '删除分类', 7031, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:category:remove', 'pc', '#', 'admin', NOW(), ''),
(7035, '服务项目管理', 7030, 2, 'service', 'micro/service/index', NULL, 1, 0, 'C', '0', '0', 'micro:service:list', 'pc', '#', 'admin', NOW(), '名称+价格+单位+计价'),
(7036, '新建服务项目', 7035, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:service:add', 'pc', '#', 'admin', NOW(), ''),
(7037, '编辑服务项目', 7035, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:service:edit', 'pc', '#', 'admin', NOW(), ''),
(7038, '停用/启用', 7035, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:service:toggle', 'pc', '#', 'admin', NOW(), ''),
(7039, '删除服务项目', 7035, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:service:remove', 'pc', '#', 'admin', NOW(), ''),
(7040, '套餐管理', 7030, 3, 'package', 'micro/package/index', NULL, 1, 0, 'C', '0', '0', 'micro:package:list', 'pc', '#', 'admin', NOW(), '组合项目'),
(7041, '新建套餐', 7040, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:package:add', 'pc', '#', 'admin', NOW(), ''),
(7042, '编辑套餐', 7040, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:package:edit', 'pc', '#', 'admin', NOW(), ''),
(7043, '删除套餐', 7040, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:package:remove', 'pc', '#', 'admin', NOW(), ''),
(7044, '价格调整记录', 7030, 4, 'priceLog', 'micro/priceLog/index', NULL, 1, 0, 'C', '0', '0', 'micro:priceLog:list', 'pc', '#', 'admin', NOW(), '改价历史'),
(7050, '基础数据配置', 7010, 4, 'basicData', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'edit', 'admin', NOW(), ''),
(7051, '员工档案', 7050, 1, 'staff', 'micro/staff/index', NULL, 1, 0, 'C', '0', '0', 'micro:staff:list', 'pc', '#', 'admin', NOW(), ''),
(7052, '新建员工', 7051, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:add', 'pc', '#', 'admin', NOW(), ''),
(7053, '编辑员工', 7051, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:edit', 'pc', '#', 'admin', NOW(), ''),
(7054, '停用员工', 7051, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:disable', 'pc', '#', 'admin', NOW(), ''),
(7055, '结算规则设置', 7050, 2, 'settleRule', 'micro/settleRule/index', NULL, 1, 0, 'C', '0', '0', 'micro:settleRule:list', 'pc', '#', 'admin', NOW(), '按单/按时/按周期'),
(7056, '新建规则', 7055, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:settleRule:add', 'pc', '#', 'admin', NOW(), ''),
(7057, '编辑规则', 7055, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:settleRule:edit', 'pc', '#', 'admin', NOW(), ''),
(7058, '店铺信息设置', 7050, 3, 'shop', 'micro/shop/index', NULL, 1, 0, 'C', '0', '0', 'micro:shop:edit', 'pc', '#', 'admin', NOW(), ''),
(7060, '系统设置', 7010, 5, 'setting', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'tool', 'admin', NOW(), ''),
(7061, '收款方式说明', 7060, 1, 'payInfo', 'micro/payInfo/index', NULL, 1, 0, 'C', '0', '0', 'micro:shop:payInfo', 'pc', '#', 'admin', NOW(), '到店付/老板收款码提示文案(V1不绑定支付账户)'),
(7062, '通知方式设置', 7060, 2, 'notify', 'micro/notify/index', NULL, 1, 0, 'C', '0', '0', 'micro:notify:config', 'pc', '#', 'admin', NOW(), ''),

-- ===== 2.3 老板 PC：小微运营（7100~7162，client_type=pc）=====
(7100, '小微运营', 0, 31, 'microOps', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'chart', 'admin', NOW(), '老板 PC：概览/预约/结算/收支/报表/推广/店铺'),
(7101, '经营概览', 7100, 1, 'dashboard', 'micro/dashboard/index', NULL, 1, 0, 'C', '0', '0', 'micro:dashboard:view', 'pc', 'dashboard', 'admin', NOW(), '首页看板'),
(7110, '预约/订单管理', 7100, 2, 'booking', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'date', 'admin', NOW(), ''),
(7111, '预约列表', 7110, 1, 'list', 'micro/booking/index', NULL, 1, 0, 'C', '0', '0', 'micro:booking:list', 'pc', '#', 'admin', NOW(), ''),
(7112, '新建预约', 7111, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:booking:add', 'pc', '#', 'admin', NOW(), '帮顾客约'),
(7113, '编辑预约', 7111, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:booking:edit', 'pc', '#', 'admin', NOW(), ''),
(7114, '取消预约', 7111, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:booking:cancel', 'pc', '#', 'admin', NOW(), ''),
(7115, '完成服务', 7111, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:booking:complete', 'pc', '#', 'admin', NOW(), ''),
(7116, '订单详情', 7110, 2, 'detail', 'micro/order/detail', NULL, 1, 0, 'C', '0', '0', 'micro:order:detail', 'pc', '#', 'admin', NOW(), ''),
(7120, '人员结算', 7100, 3, 'settle', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'money', 'admin', NOW(), 'PC重点·只记账不打款'),
(7121, '发起结算', 7120, 1, 'list', 'micro/settle/index', NULL, 1, 0, 'C', '0', '0', 'micro:settle:list', 'pc', '#', 'admin', NOW(), ''),
(7122, '选择员工+周期', 7121, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:settle:create', 'pc', '#', 'admin', NOW(), '自动汇总单量→算应发金额'),
(7123, '生成结算单', 7121, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:settle:generate', 'pc', '#', 'admin', NOW(), '形成「待发放」结算单'),
(7124, '标记已线下发放', 7121, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:settle:markPaid', 'pc', '#', 'admin', NOW(), '★线下转账后点此→改状态，不调任何支付/转账接口'),
(7125, '查看结算明细', 7121, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:settle:detail', 'pc', '#', 'admin', NOW(), ''),
(7126, '结算历史', 7120, 2, 'history', 'micro/settle/history', NULL, 1, 0, 'C', '0', '0', 'micro:settle:history', 'pc', '#', 'admin', NOW(), '含「已发/待发」状态'),
(7130, '收支记录', 7100, 4, 'cashflow', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'form', 'admin', NOW(), ''),
(7131, '收入流水', 7130, 1, 'income', 'micro/income/index', NULL, 1, 0, 'C', '0', '0', 'micro:income:list', 'pc', '#', 'admin', NOW(), ''),
(7132, '手动补录收入', 7131, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:income:add', 'pc', '#', 'admin', NOW(), ''),
(7133, '支出流水', 7130, 2, 'expense', 'micro/expense/index', NULL, 1, 0, 'C', '0', '0', 'micro:expense:list', 'pc', '#', 'admin', NOW(), '线下已付记账(工资/房租/水电)'),
(7134, '手动补录支出', 7133, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:expense:add', 'pc', '#', 'admin', NOW(), ''),
(7135, '支出预警设置', 7130, 3, 'alert', 'micro/budgetAlert/index', NULL, 1, 0, 'C', '0', '0', 'micro:budgetAlert:edit', 'pc', '#', 'admin', NOW(), '月度上限提醒'),
(7140, '报表中心', 7100, 5, 'report', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'chart', 'admin', NOW(), ''),
(7141, '经营周报', 7140, 1, 'weekly', 'micro/report/weekly', NULL, 1, 0, 'C', '0', '0', 'micro:report:weekly', 'pc', '#', 'admin', NOW(), '自动生成'),
(7142, '经营月报', 7140, 2, 'monthly', 'micro/report/monthly', NULL, 1, 0, 'C', '0', '0', 'micro:report:monthly', 'pc', '#', 'admin', NOW(), ''),
(7143, '利润报表', 7140, 3, 'profit', 'micro/report/profit', NULL, 1, 0, 'C', '0', '0', 'micro:report:profit', 'pc', '#', 'admin', NOW(), ''),
(7144, '收支明细导出', 7140, 4, 'export', 'micro/report/export', NULL, 1, 0, 'C', '0', '0', 'micro:report:export', 'pc', '#', 'admin', NOW(), ''),
(7145, '资金水位', 7140, 5, 'cashFlow', 'micro/report/cashFlow', NULL, 1, 0, 'C', '0', '0', 'micro:report:cashFlow', 'pc', '#', 'admin', NOW(), ''),
(7150, '推广发布', 7100, 6, 'promo', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'guide', 'admin', NOW(), ''),
(7151, '活动管理', 7150, 1, 'list', 'micro/promo/index', NULL, 1, 0, 'C', '0', '0', 'micro:promo:list', 'pc', '#', 'admin', NOW(), ''),
(7152, '发布活动', 7151, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:promo:add', 'pc', '#', 'admin', NOW(), ''),
(7153, '编辑活动', 7151, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:promo:edit', 'pc', '#', 'admin', NOW(), ''),
(7154, '删除活动', 7151, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:promo:remove', 'pc', '#', 'admin', NOW(), ''),
(7155, '生成分享海报', 7150, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:promo:poster', 'pc', '#', 'admin', NOW(), '海报带租户号二维码(写 share_scene)'),
(7160, '店铺管理', 7100, 7, 'shopMgr', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'shopping', 'admin', NOW(), ''),
(7161, '服务项目上下架', 7160, 1, 'toggle', 'micro/shopMgr/toggle', NULL, 1, 0, 'C', '0', '0', 'micro:service:toggle', 'pc', '#', 'admin', NOW(), ''),
(7162, '营业时间设置', 7160, 2, 'hours', 'micro/shopMgr/hours', NULL, 1, 0, 'C', '0', '0', 'micro:shop:hours', 'pc', '#', 'admin', NOW(), ''),

-- ===== 2.4 老板小程序（7200~7220，client_type=mini，只看报表+改价+提醒）=====
(7200, '小程序·老板', 0, 32, 'miniBoss', NULL, NULL, 1, 0, 'M', '0', '0', '', 'mini', 'phone', 'admin', NOW(), '老板小程序：报表+定价快改+提醒(轻量)'),
(7201, '经营报表', 7200, 1, 'report', 'miniBoss/report', NULL, 1, 0, 'C', '0', '0', 'micro:report:weekly', 'mini', '#', 'admin', NOW(), '重点①：看数据'),
(7202, '今日概览', 7201, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:dashboard:today', 'mini', '#', 'admin', NOW(), '营收/净利实时'),
(7203, '周报', 7201, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:report:weekly', 'mini', '#', 'admin', NOW(), ''),
(7204, '月报', 7201, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:report:monthly', 'mini', '#', 'admin', NOW(), ''),
(7205, '资金水位', 7201, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:report:cashFlow', 'mini', '#', 'admin', NOW(), ''),
(7210, '定价管理(快改)', 7200, 2, 'pricing', 'miniBoss/pricing', NULL, 1, 0, 'C', '0', '0', 'micro:service:list', 'mini', '#', 'admin', NOW(), '重点②：改报价'),
(7211, '服务项目列表', 7210, 1, 'list', 'miniBoss/serviceList', NULL, 1, 0, 'C', '0', '0', 'micro:service:list', 'mini', '#', 'admin', NOW(), '简版'),
(7212, '改价格', 7210, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:service:edit', 'mini', '#', 'admin', NOW(), '★只暴露"价格"字段'),
(7213, '改价确认', 7210, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:service:editConfirm', 'mini', '#', 'admin', NOW(), '记录到 price_change_log'),
(7220, '消息提醒', 7200, 3, 'notify', 'miniBoss/notify', NULL, 1, 0, 'C', '0', '0', 'micro:notify:list', 'mini', '#', 'admin', NOW(), '结算提醒/预警'),

-- ===== 2.5 业务员小程序（7300~7321，client_type=mini）=====
(7300, '小程序·业务员', 0, 33, 'miniStaff', NULL, NULL, 1, 0, 'M', '0', '0', '', 'mini', 'phone', 'admin', NOW(), '业务员小程序：排班/收入/打卡'),
(7301, '我的排班', 7300, 1, 'schedule', 'miniStaff/schedule', NULL, 1, 0, 'C', '0', '0', 'micro:staff:schedule', 'mini', '#', 'admin', NOW(), ''),
(7302, '排班详情', 7301, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:scheduleDetail', 'mini', '#', 'admin', NOW(), ''),
(7310, '我的收入', 7300, 2, 'income', 'miniStaff/income', NULL, 1, 0, 'C', '0', '0', 'micro:staff:income', 'mini', '#', 'admin', NOW(), ''),
(7311, '本周收入', 7310, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:incomeWeek', 'mini', '#', 'admin', NOW(), ''),
(7312, '本月收入', 7310, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:incomeMonth', 'mini', '#', 'admin', NOW(), ''),
(7313, '每单明细', 7310, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:incomeDetail', 'mini', '#', 'admin', NOW(), ''),
(7320, '打卡', 7300, 3, 'checkin', 'miniStaff/checkin', NULL, 1, 0, 'C', '0', '0', 'micro:staff:checkin', 'mini', '#', 'admin', NOW(), ''),
(7321, '确认完成服务', 7320, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:staff:checkinSubmit', 'mini', '#', 'admin', NOW(), ''),

-- ===== 2.6 顾客小程序（7400~7422，client_type=mini，★无 micro:customer:pay）=====
(7400, '小程序·顾客', 0, 34, 'miniCustomer', NULL, NULL, 1, 0, 'M', '0', '0', '', 'mini', 'phone', 'admin', NOW(), '顾客小程序：浏览/预约/到店付(V1无在线支付)'),
(7401, '浏览店铺', 7400, 1, 'shop', 'miniCustomer/shop', NULL, 1, 0, 'C', '0', '0', 'micro:customer:shop', 'mini', '#', 'admin', NOW(), ''),
(7410, '预约服务', 7400, 2, 'book', 'miniCustomer/book', NULL, 1, 0, 'C', '0', '0', 'micro:customer:book', 'mini', '#', 'admin', NOW(), ''),
(7411, '选择服务项目', 7410, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:selectService', 'mini', '#', 'admin', NOW(), ''),
(7412, '选择时间', 7410, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:selectTime', 'mini', '#', 'admin', NOW(), ''),
(7413, '提交预约', 7410, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:submitBook', 'mini', '#', 'admin', NOW(), '生成「待到店付」订单'),
(7420, '我的预约', 7400, 3, 'myBook', 'miniCustomer/myBook', NULL, 1, 0, 'C', '0', '0', 'micro:customer:myBook', 'mini', '#', 'admin', NOW(), ''),
(7421, '取消预约', 7420, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:cancelBook', 'mini', '#', 'admin', NOW(), ''),
(7422, '查看收款方式', 7420, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:customer:payInfo', 'mini', '#', 'admin', NOW(), '展示「到店付/老板收款码」提示，★不调支付接口');

-- ----------------------------
-- 3. 授予超管 role_id=1（便于 admin 管理；业务角色授权由 role_template 驱动，见 M15_07）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7000 AND 7499;

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 client_type 列存在且默认 both
-- SELECT COLUMN_NAME, COLUMN_DEFAULT FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_menu' AND COLUMN_NAME='client_type';
--
-- 4.2 存量菜单回填 pc（menu_id<7000 应无 mini）
-- SELECT client_type, COUNT(*) FROM sys_menu WHERE menu_id < 7000 GROUP BY client_type;  -- 预期全 pc
--
-- 4.3 小微版菜单按端分布
-- SELECT client_type, COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7000 AND 7499 GROUP BY client_type;
-- 预期：pc(平台管理员+老板PC+admin) / mini(老板+业务员+顾客小程序)
--
-- 4.4 ★V1 零支付：顾客端无 micro:customer:pay（在线支付）菜单
-- SELECT COUNT(*) FROM sys_menu WHERE perms = 'micro:customer:pay';  -- 预期 0
-- SELECT menu_id, perms FROM sys_menu WHERE perms = 'micro:customer:payInfo';  -- 预期 7422（仅提示）
--
-- 4.5 ★老板小程序仅报表+定价+提醒（client_type=mini 且 parent 链属 7200）
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 7200 AND 7299 AND menu_type IN ('C','F');
