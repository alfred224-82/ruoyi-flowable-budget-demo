-- =====================================================================
-- M14_01 / M14_02 商业化计费与合规表建表脚本（系统管理包提取）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档》v1.16 T14.1~T14.4
-- 背景：
--   M14_01_billing_settlement.sql 含 7 张表 + 四档套餐初始数据 + 平台计费菜单，
--   M14_02_compliance.sql 含 4 张表 + 协议版本初始数据 + 合规管理菜单。
--   原 M14 脚本归入各版本目录（基础版/高级版/专业版/小微版），系统管理包不含。
--   而管理员操作手册 §5.7（计费套餐管理 / 订单管理 / 分账 / 提现 / 对账 / 合规）
--   全部操作均依赖这些表存在；§5.7 验收 SQL 直接查询 sys_billing_order 等表。
--   Java 实体 SysBillingPlan / SysBillingOrder / SysAgreementVersion 等已被
--   BillingOrderServiceImpl / LicenseServiceImpl 等多处引用，表不存在会导致运行时报错。
-- 本脚本：
--   从 M14_01 + M14_02 提取全部 11 张建表 + 初始数据，CREATE TABLE IF NOT EXISTS 幂等，
--   菜单部分不重复插入（INSERT IGNORE + 条件判断）。
--   与各版本目录 M14 脚本完全兼容（小微版先执行 M14_01 DROP+CREATE，本脚本 IF NOT EXISTS 跳过）。
-- 前置依赖：M3_05（sys_license.tenant_id）、M1_06（sys_tenant.access_channel）
-- =====================================================================

-- ----------------------------
-- 1. sys_billing_plan 计费套餐表（系统级共享，无 tenant_id，IGNORE_TENANT_TABLES）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_billing_plan` (
  `plan_code` VARCHAR(40) NOT NULL COMMENT '套餐编码：trial/first_month/monthly/yearly',
  `name` VARCHAR(100) NOT NULL COMMENT '套餐名称',
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '价格(元)',
  `days` INT NOT NULL DEFAULT 0 COMMENT '有效天数',
  `license_type` VARCHAR(20) NOT NULL DEFAULT 'FORMAL' COMMENT '授权类型：TRIAL/FORMAL',
  `edition` VARCHAR(20) DEFAULT 'BASIC' COMMENT '默认授予版本',
  `first_only` TINYINT DEFAULT 0 COMMENT '是否仅首次可用(0否 1是)',
  `need_pay` TINYINT DEFAULT 1 COMMENT '是否需支付(0否 1是)',
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
-- 2. sys_billing_order 计费订单表（租户级，含 tenant_id）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_billing_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `plan_code` VARCHAR(40) NOT NULL COMMENT '套餐编码(sys_billing_plan.plan_code)',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额(元)',
  `license_type` VARCHAR(20) DEFAULT 'FORMAL' COMMENT '授权类型：TRIAL/FORMAL',
  `edition` VARCHAR(20) DEFAULT 'BASIC' COMMENT '授予版本',
  `period_start` DATETIME DEFAULT NULL COMMENT '服务开始时间',
  `period_end` DATETIME DEFAULT NULL COMMENT '服务到期时间',
  `pay_channel` VARCHAR(30) DEFAULT NULL COMMENT '支付渠道',
  `pay_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '支付状态',
  `transaction_id` VARCHAR(100) DEFAULT NULL COMMENT '第三方支付流水号',
  `license_id` VARCHAR(64) DEFAULT NULL COMMENT '关联 License 编号',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计费订单表';

-- ----------------------------
-- 3. sys_virtual_account 虚拟账户表（租户级）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_virtual_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `balance` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '可用余额(元)',
  `frozen_balance` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '冻结余额',
  `total_income` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '累计入账',
  `total_withdrawn` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '累计提现',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1冻结）',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户虚拟账户表';

-- ----------------------------
-- 4. sys_profit_sharing_rule 分账规则表（租户级）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_profit_sharing_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `platform_rate` DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '平台服务费率',
  `tenant_rate` DECIMAL(5,4) NOT NULL DEFAULT 1.0000 COMMENT '租户应得率',
  `effective_time` DATETIME DEFAULT NULL COMMENT '生效时间',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账规则表';

-- ----------------------------
-- 5. sys_profit_sharing_record 分账记录表（租户级）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_profit_sharing_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `order_no` VARCHAR(64) DEFAULT NULL COMMENT '关联订单号',
  `order_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `platform_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '平台服务费金额',
  `tenant_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '租户应得金额',
  `platform_rate` DECIMAL(5,4) DEFAULT NULL COMMENT '分账时平台费率快照',
  `status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账记录表';

-- ----------------------------
-- 6. sys_withdrawal_request 提现申请表（租户级）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_withdrawal_request` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `withdraw_no` VARCHAR(64) NOT NULL COMMENT '提现单号',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '提现金额',
  `account_info` VARCHAR(300) DEFAULT NULL COMMENT '收款账户信息(脱敏)',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现申请表';

-- ----------------------------
-- 7. sys_settlement_bill 结算对账单表（租户级）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_settlement_bill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `bill_no` VARCHAR(64) NOT NULL COMMENT '对账单号',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `period` VARCHAR(10) NOT NULL COMMENT '账期(yyyy-MM)',
  `order_count` INT DEFAULT 0 COMMENT '订单笔数',
  `gross_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '订单总额',
  `platform_fee` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '平台服务费合计',
  `tenant_amount` DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '租户应得合计',
  `status` VARCHAR(20) DEFAULT 'GENERATED' COMMENT '状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_tenant_period` (`tenant_id`, `period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算对账单表';

-- ----------------------------
-- 8. sys_subscribe_quota 订阅消息授权次数表（租户级，M14_02 合规）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_subscribe_quota` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `openid` VARCHAR(100) NOT NULL COMMENT '微信 openid',
  `template_id` VARCHAR(100) NOT NULL COMMENT '订阅消息模板ID',
  `remaining_count` INT NOT NULL DEFAULT 0 COMMENT '剩余可发送次数',
  `total_granted` INT NOT NULL DEFAULT 0 COMMENT '累计授权次数',
  `total_sent` INT NOT NULL DEFAULT 0 COMMENT '累计发送次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_openid_tpl` (`tenant_id`, `openid`, `template_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅消息授权次数表';

-- ----------------------------
-- 9. sys_agreement_version 协议版本表（系统级共享，IGNORE_TENANT_TABLES，M14_02）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_agreement_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agreement_type` VARCHAR(30) NOT NULL COMMENT '协议类型：privacy/user',
  `version` VARCHAR(20) NOT NULL COMMENT '版本号',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '协议标题',
  `content_url` VARCHAR(500) DEFAULT NULL COMMENT '协议正文URL',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '正文哈希',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0生效 1历史）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_version` (`agreement_type`, `version`),
  KEY `idx_type_status` (`agreement_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议版本表(系统级共享,IGNORE_TENANT_TABLES)';

-- ----------------------------
-- 10. sys_user_agreement_log 用户协议同意留痕表（租户级，M14_02）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_agreement_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `openid` VARCHAR(100) DEFAULT NULL COMMENT '微信 openid',
  `agreement_type` VARCHAR(30) NOT NULL COMMENT '协议类型',
  `version` VARCHAR(20) NOT NULL COMMENT '同意的版本号',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
  `agree_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '同意时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type_version` (`agreement_type`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议同意留痕表';

-- ----------------------------
-- 11. sys_account_action_log 账户数据操作留痕表（租户级，M14_02）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_account_action_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `action_type` VARCHAR(30) NOT NULL COMMENT '操作类型：DATA_EXPORT/ACCOUNT_DELETE',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态',
  `result_ref` VARCHAR(500) DEFAULT NULL COMMENT '结果引用',
  `deadline` DATETIME DEFAULT NULL COMMENT '完成截止时间',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '处理人',
  `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type_status` (`action_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户数据操作留痕表';

-- =====================================================================
-- 12. 四档套餐初始数据（幂等：仅当表为空时插入）
-- =====================================================================
INSERT IGNORE INTO `sys_billing_plan` (`plan_code`, `name`, `price`, `days`, `license_type`, `edition`, `first_only`, `need_pay`, `sort`, `status`, `remark`) VALUES
('trial',       '免费试用',   0.00,    7,   'TRIAL',  'BASIC',        0, 0, 1, '0', '7天免费试用'),
('first_month', '首月特惠',   299.00,  30,  'FORMAL', 'ADVANCED',     1, 1, 2, '0', '299元/首月，仅首次可用'),
('monthly',     '月付套餐',   499.00,  30,  'FORMAL', 'ADVANCED',     0, 1, 3, '0', '499元/30天'),
('yearly',      '包年套餐',   4790.00, 365, 'FORMAL', 'PROFESSIONAL', 0, 1, 4, '0', '4790元/年(约8折)');

-- =====================================================================
-- 13. 协议版本初始数据（幂等）
-- =====================================================================
INSERT IGNORE INTO `sys_agreement_version` (`agreement_type`, `version`, `title`, `publish_time`, `status`, `create_by`, `remark`) VALUES
('privacy', 'v1.0', '隐私政策',     NOW(), '0', 'admin', '初始版本'),
('user',    'v1.0', '用户服务协议', NOW(), '0', 'admin', '初始版本');

-- =====================================================================
-- 14. 平台计费菜单（幂等：INSERT IGNORE 避免重复）
-- =====================================================================
INSERT IGNORE INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(6120, '平台计费', 0, 8, 'billing', NULL, NULL, 1, 0, 'M', '0', '0', '', 'money', 'admin', NOW(), '', NULL, '计费套餐/订单/分账/提现/对账（M14）'),
(6121, '计费套餐', 6120, 1, 'plan', 'billing/plan/index', NULL, 1, 0, 'C', '0', '0', 'billing:plan:list', 'shopping', 'admin', NOW(), '', NULL, ''),
(6122, '套餐查询', 6121, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:plan:query', '#', 'admin', NOW(), '', NULL, ''),
(6123, '套餐新增', 6121, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:plan:add',   '#', 'admin', NOW(), '', NULL, ''),
(6124, '套餐编辑', 6121, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:plan:edit',  '#', 'admin', NOW(), '', NULL, ''),
(6125, '计费订单', 6120, 2, 'order', 'billing/order/index', NULL, 1, 0, 'C', '0', '0', 'billing:order:list', 'form', 'admin', NOW(), '', NULL, ''),
(6126, '订单查询', 6125, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:order:query',  '#', 'admin', NOW(), '', NULL, ''),
(6127, '手动开通', 6125, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:order:create', '#', 'admin', NOW(), '', NULL, ''),
(6130, '分账规则', 6120, 3, 'sharing', 'billing/sharing/index', NULL, 1, 0, 'C', '0', '0', 'billing:sharing:list', 'rate', 'admin', NOW(), '', NULL, ''),
(6131, '规则查询', 6130, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:sharing:query', '#', 'admin', NOW(), '', NULL, ''),
(6132, '规则编辑', 6130, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:sharing:edit',  '#', 'admin', NOW(), '', NULL, ''),
(6135, '提现审核', 6120, 4, 'withdrawal', 'billing/withdrawal/index', NULL, 1, 0, 'C', '0', '0', 'billing:withdrawal:list', 'validCode', 'admin', NOW(), '', NULL, ''),
(6136, '提现查询', 6135, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:withdrawal:query', '#', 'admin', NOW(), '', NULL, ''),
(6137, '提现审核', 6135, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:withdrawal:audit', '#', 'admin', NOW(), '', NULL, ''),
(6140, '结算对账', 6120, 5, 'settlement', 'billing/settlement/index', NULL, 1, 0, 'C', '0', '0', 'billing:settlement:list', 'documentation', 'admin', NOW(), '', NULL, ''),
(6141, '对账查询', 6140, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:settlement:query',    '#', 'admin', NOW(), '', NULL, ''),
(6142, '对账生成', 6140, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'billing:settlement:generate', '#', 'admin', NOW(), '', NULL, ''),
-- 合规管理菜单（M14_02）
(6160, '合规管理', 0, 9, 'compliance2', NULL, NULL, 1, 0, 'M', '0', '0', '', 'checkbox', 'admin', NOW(), '', NULL, '协议版本/数据合规（M14 T14.4）'),
(6161, '协议版本', 6160, 1, 'agreement', 'compliance/agreement/index', NULL, 1, 0, 'C', '0', '0', 'compliance:agreement:list', 'documentation', 'admin', NOW(), '', NULL, ''),
(6162, '协议查询', 6161, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:agreement:query', '#', 'admin', NOW(), '', NULL, ''),
(6163, '协议发布', 6161, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:agreement:add',   '#', 'admin', NOW(), '', NULL, ''),
(6165, '数据合规', 6160, 2, 'action', 'compliance/action/index', NULL, 1, 0, 'C', '0', '0', 'compliance:action:list', 'druid', 'admin', NOW(), '', NULL, ''),
(6166, '合规查询', 6165, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:action:query',  '#', 'admin', NOW(), '', NULL, ''),
(6167, '合规处理', 6165, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:action:handle', '#', 'admin', NOW(), '', NULL, '');

-- 计费+合规菜单授权 admin 超管（role_id=1）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id IN (
  6120,6121,6122,6123,6124,6125,6126,6127,
  6130,6131,6132,6135,6136,6137,
  6140,6141,6142,
  6160,6161,6162,6163,6165,6166,6167
);

-- ----------------------------
-- 15. 验收查询
-- ----------------------------
-- 15.1 四档套餐
-- SELECT plan_code, name, price, days FROM sys_billing_plan;  -- 预期 4 行
-- 15.2 协议版本
-- SELECT agreement_type, version, status FROM sys_agreement_version;  -- 预期 2 行
-- 15.3 11 张表全部存在
-- SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()
--   AND TABLE_NAME IN ('sys_billing_plan','sys_billing_order','sys_virtual_account',
--     'sys_profit_sharing_rule','sys_profit_sharing_record','sys_withdrawal_request',
--     'sys_settlement_bill','sys_subscribe_quota','sys_agreement_version',
--     'sys_user_agreement_log','sys_account_action_log');  -- 预期 11
