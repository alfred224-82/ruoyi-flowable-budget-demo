-- =====================================================================
-- M14_01 商业化计费与分账结算建表脚本（T14.1 / T14.2 / T14.3）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 M14（商业化计费与合规）
--       《多业态SaaS平台需求规格说明书》v1.0 §4.2（定价与计费：7天试用/首月299/月付499/包年4790）/ §7.1（收款分离）
--       《资金管控系统设计文档》v1.3 §1.5（定价）
-- 前置依赖：
--   M3_05_license_tenant_association.sql（sys_license.tenant_id）
--   M13_01_access_channel.sql（tenant_payment_config 收款模式：self_merchant/platform_proxy）
-- 规范：
--   1. 先删后建 / 先删后插，脚本可重复执行（幂等）
--   2. sys_billing_plan / 为【系统级共享表】(无 tenant_id)，须纳入 IGNORE_TENANT_TABLES
--   3. 其余表含 tenant_id + 租户索引；平台超管(admin,tenantId 为空)跨租户可见，租户自动隔离仅见本租户
--   4. 【严禁自动扣款】：试用/套餐到期只停用不扣款（T14.2）；首月特惠同租户仅一次（first_only 校验）
--   5. 金额统一 DECIMAL(12,2)；分账金额合计 = 订单金额（platform_amount + tenant_amount = amount）
-- =====================================================================

-- ----------------------------
-- 1. 计费套餐表 sys_billing_plan（系统级共享；四档套餐）
-- ----------------------------
DROP TABLE IF EXISTS `sys_billing_plan`;
CREATE TABLE `sys_billing_plan` (
  `plan_code` VARCHAR(40) NOT NULL COMMENT '套餐编码：trial/first_month/monthly/yearly',
  `name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '价格(元)',
  `days` INT NOT NULL DEFAULT 0 COMMENT '有效天数(NULL天数用 expire_date 永久；trial=7,monthly=30,yearly=365)',
  `license_type` VARCHAR(20) NOT NULL DEFAULT 'FORMAL' COMMENT '授权类型：TRIAL/FORMAL',
  `edition` VARCHAR(20) DEFAULT 'BASIC' COMMENT '默认授予版本：BASIC/ADVANCED/PROFESSIONAL',
  `first_only` TINYINT DEFAULT 0 COMMENT '是否仅首次可用(0否 1是；首月特惠同租户仅一次)',
  `need_pay` TINYINT DEFAULT 1 COMMENT '是否需支付(0否 1是；试用不绑卡不扣款=0)',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0上架 1下架）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`plan_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计费套餐表(系统级共享,IGNORE_TENANT_TABLES)';

-- ----------------------------
-- 2. 计费订单表 sys_billing_order（T14.1；下单写 license_type + expire_date，与 License 一一对应）
-- ----------------------------
DROP TABLE IF EXISTS `sys_billing_order`;
CREATE TABLE `sys_billing_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `plan_code` VARCHAR(40) NOT NULL COMMENT '套餐编码(sys_billing_plan.plan_code)',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额(元)',
  `license_type` VARCHAR(20) DEFAULT 'FORMAL' COMMENT '授权类型：TRIAL/FORMAL',
  `edition` VARCHAR(20) DEFAULT 'BASIC' COMMENT '授予版本',
  `period_start` DATETIME DEFAULT NULL COMMENT '服务开始时间',
  `period_end` DATETIME DEFAULT NULL COMMENT '服务到期时间(写入 sys_tenant.expire_date)',
  `pay_channel` VARCHAR(30) DEFAULT NULL COMMENT '支付渠道：wechat/alipay/offline/manual',
  `pay_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '支付状态：PENDING/PAID/REFUNDED/CANCELLED',
  `transaction_id` VARCHAR(100) DEFAULT NULL COMMENT '第三方支付流水号',
  `license_id` VARCHAR(64) DEFAULT NULL COMMENT '关联签发的 License 编号(sys_license.license_id)',
  `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_plan` (`tenant_id`, `plan_code`),
  KEY `idx_pay_status` (`pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计费订单表(与License一一对应)';

-- ----------------------------
-- 3. 虚拟账户表 sys_virtual_account（T14.3 每租户一个虚拟账户；平台代收模式分账入账）
-- ----------------------------
DROP TABLE IF EXISTS `sys_virtual_account`;
CREATE TABLE `sys_virtual_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `balance` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额(元)',
  `frozen_balance` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额(提现审核中)',
  `total_income` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '累计入账',
  `total_withdrawn` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '累计提现',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1冻结）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户虚拟账户表(分账入账/提现)';

-- ----------------------------
-- 4. 分账规则表 sys_profit_sharing_rule（T14.3 平台服务费 + 租户应得，可配置）
-- ----------------------------
DROP TABLE IF EXISTS `sys_profit_sharing_rule`;
CREATE TABLE `sys_profit_sharing_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `platform_rate` DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '平台服务费率(0~1,如 0.0060=0.6%)',
  `tenant_rate` DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '租户应得率(0~1；platform_rate+tenant_rate=1)',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账规则表(平台服务费+租户应得)';

-- ----------------------------
-- 5. 分账记录表 sys_profit_sharing_record（T14.3 每笔收款正确入账；分账金额合计=订单金额）
-- ----------------------------
DROP TABLE IF EXISTS `sys_profit_sharing_record`;
CREATE TABLE `sys_profit_sharing_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `order_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `platform_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '平台服务费金额',
  `tenant_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '租户应得金额(入账虚拟账户)',
  `platform_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '分账时平台费率快照',
  `status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED/REVERSED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账记录表';

-- ----------------------------
-- 6. 提现申请表 sys_withdrawal_request（T14.3 提现申请与人工审核）
-- ----------------------------
DROP TABLE IF EXISTS `sys_withdrawal_request`;
CREATE TABLE `sys_withdrawal_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `withdraw_no` VARCHAR(64) NOT NULL COMMENT '提现单号',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额',
  `account_info` VARCHAR(300) DEFAULT NULL COMMENT '收款账户信息(脱敏)',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED/PAID',
  `audit_by` VARCHAR(64) DEFAULT NULL COMMENT '审核人',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `audit_remark` VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  `create_by` VARCHAR(64) COMMENT '申请人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_withdraw_no` (`withdraw_no`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表(人工审核)';

-- ----------------------------
-- 7. 结算对账单表 sys_settlement_bill（T13.6/T14.3 结算单与对账报表，金额可追溯到订单）
-- ----------------------------
DROP TABLE IF EXISTS `sys_settlement_bill`;
CREATE TABLE `sys_settlement_bill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `bill_no` VARCHAR(64) NOT NULL COMMENT '对账单号',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `period` VARCHAR(10) NOT NULL COMMENT '账期(yyyy-MM)',
  `order_count` INT DEFAULT 0 COMMENT '订单笔数',
  `gross_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '订单总额',
  `platform_fee` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '平台服务费合计',
  `tenant_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '租户应得合计',
  `status` VARCHAR(20) DEFAULT 'GENERATED' COMMENT '状态：GENERATED/CONFIRMED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_tenant_period` (`tenant_id`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算对账单表';

-- =====================================================================
-- 8. 四档套餐初始数据（T14.1，先删后插幂等；价格/天数对齐需求文档 §4.2）
--    试用：0元/7天，不绑卡不自动扣款；首月：299/首月(仅首次)；月付：499/30天；包年：4790/年(约8折)
-- =====================================================================
DELETE FROM `sys_billing_plan` WHERE `plan_code` IN ('trial', 'first_month', 'monthly', 'yearly');
INSERT INTO `sys_billing_plan` (`plan_code`, `name`, `price`, `days`, `license_type`, `edition`, `first_only`, `need_pay`, `sort`, `status`, `remark`) VALUES
('trial',       '免费试用',   0.00,    7,   'TRIAL',  'BASIC',        0, 0, 1, '0', '7天免费试用，不绑卡、不自动扣款、无隐藏收费'),
('first_month', '首月特惠',   299.00,  30,  'FORMAL', 'ADVANCED',     1, 1, 2, '0', '299元/首月，同租户仅首次可用(first_only=1)'),
('monthly',     '月付套餐',   499.00,  30,  'FORMAL', 'ADVANCED',     0, 1, 3, '0', '499元/30天'),
('yearly',      '包年套餐',   4790.00, 365, 'FORMAL', 'PROFESSIONAL', 0, 1, 4, '0', '4790元/年(约8折,5988×0.8≈4790)');

-- =====================================================================
-- 9. 菜单：平台计费（目录 6120，platform 级，menu_id 占用 6120~6159；仅 admin 超管 role_id=1）
-- =====================================================================
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 6120 AND 6159;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(6120, '平台计费', 0, 8, 'billing', NULL, NULL, 1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), '', NULL, '计费套餐/订单/分账/提现/对账（M14，平台级）'),
-- 套餐管理
(6121, '计费套餐', 6120, 1, 'plan', 'billing/plan/index', NULL, 1, 0, 'C', '0', '0', 'billing:plan:list', 'shopping', 'admin', NOW(), '', NULL, '四档套餐维护'),
(6122, '套餐查询', 6121, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:plan:query', '#', 'admin', NOW(), '', NULL, ''),
(6123, '套餐新增', 6121, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:plan:add',   '#', 'admin', NOW(), '', NULL, ''),
(6124, '套餐编辑', 6121, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:plan:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 计费订单
(6125, '计费订单', 6120, 2, 'order', 'billing/order/index', NULL, 1, 0, 'C', '0', '0', 'billing:order:list', 'form', 'admin', NOW(), '', NULL, '下单/开通/续费订单'),
(6126, '订单查询', 6125, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:order:query',  '#', 'admin', NOW(), '', NULL, ''),
(6127, '手动开通', 6125, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:order:create', '#', 'admin', NOW(), '', NULL, '平台代下单/离线收款开通'),
-- 分账规则
(6130, '分账规则', 6120, 3, 'sharing', 'billing/sharing/index', NULL, 1, 0, 'C', '0', '0', 'billing:sharing:list', 'rate', 'admin', NOW(), '', NULL, '平台服务费率+租户应得率'),
(6131, '规则查询', 6130, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:sharing:query', '#', 'admin', NOW(), '', NULL, ''),
(6132, '规则编辑', 6130, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:sharing:edit',  '#', 'admin', NOW(), '', NULL, ''),
-- 提现审核
(6135, '提现审核', 6120, 4, 'withdrawal', 'billing/withdrawal/index', NULL, 1, 0, 'C', '0', '0', 'billing:withdrawal:list', 'validCode', 'admin', NOW(), '', NULL, '提现申请人工审核'),
(6136, '提现查询', 6135, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:withdrawal:query', '#', 'admin', NOW(), '', NULL, ''),
(6137, '提现审核', 6135, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:withdrawal:audit', '#', 'admin', NOW(), '', NULL, '通过后余额扣减'),
-- 对账单
(6140, '结算对账', 6120, 5, 'settlement', 'billing/settlement/index', NULL, 1, 0, 'C', '0', '0', 'billing:settlement:list', 'documentation', 'admin', NOW(), '', NULL, '按账期生成对账单'),
(6141, '对账查询', 6140, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:settlement:query',    '#', 'admin', NOW(), '', NULL, ''),
(6142, '对账生成', 6140, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:settlement:generate', '#', 'admin', NOW(), '', NULL, '');

-- 平台计费菜单授权给 admin 超管（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 6120 AND 6159;

-- ----------------------------
-- 10. 验收查询（执行后人工核对）
-- ----------------------------
-- 10.1 四档套餐
-- SELECT plan_code, name, price, days, first_only, need_pay FROM sys_billing_plan;  -- 预期 4 行
-- 10.2 计费表均含 tenant_id（sys_billing_plan 除外）
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('sys_billing_order','sys_virtual_account','sys_profit_sharing_rule','sys_profit_sharing_record','sys_withdrawal_request','sys_settlement_bill');  -- 预期 6 行
-- 10.3 平台计费菜单仅授权 admin(role_id=1)
-- SELECT role_id, COUNT(*) FROM sys_role_menu WHERE menu_id BETWEEN 6120 AND 6159 GROUP BY role_id;  -- 仅 1
