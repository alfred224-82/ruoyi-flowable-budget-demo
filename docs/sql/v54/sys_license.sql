-- -----------------------------------------------------------
-- License授权表（V5.4 Phase 2）
-- 用于控制系统版本（BASIC/ADVANCED/ENTERPRISE）和功能开关
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `sys_license`;
CREATE TABLE `sys_license` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `license_id` varchar(50) NOT NULL COMMENT '授权编号（LIC-YYYY-NNNNNN）',
  `license_type` varchar(20) NOT NULL COMMENT '授权类型（TRIAL/FORMAL）',
  `customer` varchar(200) NOT NULL COMMENT '客户名称',
  `edition` varchar(20) NOT NULL COMMENT '版本等级（BASIC/ADVANCED/ENTERPRISE）',
  `trial_days` int DEFAULT 0 COMMENT '试用天数（仅TRIAL有效）',
  `industries` varchar(500) DEFAULT NULL COMMENT '授权行业列表JSON（如["INTERNET","FINANCE"]）',
  `max_tenants` int DEFAULT 1 COMMENT '最大租户数',
  `features` text NOT NULL COMMENT '功能开关列表JSON',
  `machine_fingerprint` varchar(128) NOT NULL COMMENT '机器指纹哈希',
  `issued_at` date NOT NULL COMMENT '签发日期',
  `expires_at` date DEFAULT NULL COMMENT '到期日期（NULL=永久）',
  `license_version` int DEFAULT 1 COMMENT 'License版本号（防降级回滚）',
  `signature` text NOT NULL COMMENT 'RSA-SHA256数字签名',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/EXPIRED/REVOKED/TAMPERED）',
  `activated_at` datetime DEFAULT NULL COMMENT '首次激活时间（试用起算）',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后校验时间（防时钟回拨）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_license_id` (`license_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='License授权表';

-- -----------------------------------------------------------
-- 行业配置主数据表（V5.4 Phase 4）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_industry_config`;
CREATE TABLE `fee_industry_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `industry_code` varchar(50) NOT NULL COMMENT '行业编码（INTERNET/REALESTATE/MANUFACTURING/FINANCE）',
  `industry_name` varchar(100) NOT NULL COMMENT '行业名称',
  `switches` text DEFAULT NULL COMMENT '行业开关组JSON',
  `depreciation_defaults` text DEFAULT NULL COMMENT '折旧默认参数JSON',
  `budget_thresholds` text DEFAULT NULL COMMENT '预算管控阈值JSON',
  `compliance_rules` text DEFAULT NULL COMMENT '行业合规拦截规则JSON',
  `ai_prompt_pack` text DEFAULT NULL COMMENT 'AI引导话术包JSON',
  `profit_tip_pack` text DEFAULT NULL COMMENT '盈利提示文案包JSON',
  `accrual_params` text DEFAULT NULL COMMENT '计提规则参数JSON',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_code` (`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行业配置主数据表';

-- -----------------------------------------------------------
-- 版本增量初始化数据标记表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_edition_init_data`;
CREATE TABLE `fee_edition_init_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `edition` varchar(20) NOT NULL COMMENT '版本（BASIC/ADVANCED/ENTERPRISE）',
  `init_key` varchar(100) NOT NULL COMMENT '初始化数据标识',
  `init_status` varchar(20) DEFAULT 'PENDING' COMMENT '执行状态（PENDING/DONE）',
  `executed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edition_key` (`edition`, `init_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本增量初始化数据标记表';

-- -----------------------------------------------------------
-- 初始化默认License（开发/演示用，企业版，永久有效）
-- 注意：生产环境需通过管理端点替换为正式签名License
-- -----------------------------------------------------------
DELETE FROM `sys_license` WHERE `license_id` = 'LIC-2026-000001';
INSERT INTO `sys_license` (`license_id`, `license_type`, `customer`, `edition`, `trial_days`, `industries`, `max_tenants`, `features`, `machine_fingerprint`, `issued_at`, `expires_at`, `license_version`, `signature`, `status`) VALUES
('LIC-2026-000001', 'FORMAL', '默认开发客户', 'ENTERPRISE', 0, '["INTERNET","REALESTATE","MANUFACTURING","FINANCE"]', 10,
 '["FEE_MULTI_CURRENCY","FEE_COMPLIANCE_CUSTOM","FEE_INDUSTRY_SINGLE","FEE_INDUSTRY_PACK_IO","FEE_TRACK_CONTROL","FEE_ACCRUAL_ENGINE","FEE_LOAN_LEDGER","FEE_PROFIT_CUSTOM_TIP","FEE_SUBSIDY_CONFIG","FEE_AI_CUSTOM_PROMPT","FEE_AI_CONFIDENCE_REVIEW","FEE_AI_PRIVATE_DEPLOY","FEE_DESENSITIZE_CUSTOM","FEE_CROSS_DEPT_ALLOCATE","FEE_FUND_POOL","FEE_AUDIT_FULL_EXPORT","BUDGET_BATCH_SUBMIT","BUDGET_BATCH_APPROVE","BUDGET_PERIOD_CONTROL","BUDGET_MULTI_LEVEL_APPROVAL","BUDGET_THREE_LEVEL_APPROVAL","BUDGET_SEND_BACK","BUDGET_DEADLINE_REMIND","BUDGET_TIMEOUT_ALERT","BUDGET_RISK_WARNING","BUDGET_RISK_CUSTOM","BUDGET_RISK_FORMULA","BUDGET_RISK_AI","BUDGET_AI_CHAT","BUDGET_AI_REPORT"]',
 'DEV-ENVIRONMENT', '2026-01-01', NULL, 1, 'DEV-SIGNATURE-PLACEHOLDER', 'ACTIVE');

-- -----------------------------------------------------------
-- 初始化4大行业配置
-- -----------------------------------------------------------
DELETE FROM `fee_industry_config` WHERE `industry_code` IN ('INTERNET', 'REALESTATE', 'MANUFACTURING', 'FINANCE');
INSERT INTO `fee_industry_config` (`industry_code`, `industry_name`, `switches`, `budget_thresholds`, `status`) VALUES
('INTERNET', '互联网行业', '{"enableMultiCurrency":true,"enableAccrual":true,"enableLoan":true}', '{"expenseWarning":80,"capitalWarning":90,"cashflowWarning":85}', 'ENABLED'),
('REALESTATE', '房地产行业', '{"enableMultiCurrency":false,"enableAccrual":true,"enableLoan":true}', '{"expenseWarning":75,"capitalWarning":85,"cashflowWarning":80}', 'ENABLED'),
('MANUFACTURING', '制造业', '{"enableMultiCurrency":true,"enableAccrual":true,"enableLoan":true}', '{"expenseWarning":80,"capitalWarning":85,"cashflowWarning":90}', 'ENABLED'),
('FINANCE', '金融行业', '{"enableMultiCurrency":true,"enableAccrual":true,"enableLoan":true,"enableDesensitize":true}', '{"expenseWarning":70,"capitalWarning":80,"cashflowWarning":75}', 'ENABLED');
