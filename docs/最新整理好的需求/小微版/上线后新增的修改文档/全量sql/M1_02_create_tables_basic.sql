-- =====================================================================
-- M1_02 基础版业务表建表脚本（T1.1）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控平台新功能详细设计文档》Phase 18~26 建表定义
--       《资金管控系统设计文档》2.6 基础版数据模型
--       《资金管控系统开发任务文档》T1.1
-- 规范：
--   1. 逐表含 tenant_id + 租户索引（全局多租户硬约束，设计文档 1.2）
--   2. 预留 department_id / org_id（ADR-009，基础版不启用）
--   3. 先删后建，脚本可重复执行
--   4. 高级版/专业版增量表一次建齐（一套系统策略），见 M1_03 脚本
-- =====================================================================

-- ----------------------------
-- 1. 项目信息表
-- ----------------------------
DROP TABLE IF EXISTS `fee_project`;
CREATE TABLE `fee_project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `client_name` VARCHAR(200) COMMENT '客户名称',
  `contract_amount` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '初始合同额',
  `current_contract_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '当前合同额(含变更)',
  `status` VARCHAR(30) DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING_APPROVAL/ACTIVE/COMPLETED/CLOSED/LOST',
  `start_date` DATE COMMENT '开始日期',
  `end_date` DATE COMMENT '结束日期',
  `project_manager_id` BIGINT COMMENT '项目经理ID',
  `project_manager_name` VARCHAR(50) COMMENT '项目经理姓名',
  `total_budget` DECIMAL(14,2) DEFAULT 0 COMMENT '项目总预算',
  `used_budget` DECIMAL(14,2) DEFAULT 0 COMMENT '已用预算',
  `verified_income` DECIMAL(14,2) DEFAULT 0 COMMENT '已核验收入',
  `pending_income` DECIMAL(14,2) DEFAULT 0 COMMENT '待核验收入',
  `total_expense` DECIMAL(14,2) DEFAULT 0 COMMENT '总支出(报销+付款)',
  `loss_reason` VARCHAR(500) COMMENT '烂账原因',
  `loss_type` VARCHAR(50) COMMENT '烂账类型',
  `bad_debt_provision` DECIMAL(14,2) DEFAULT 0 COMMENT '坏账计提金额',
  `change_count` INT DEFAULT 0 COMMENT '变更次数',
  `department_id` BIGINT DEFAULT NULL COMMENT '部门ID(高级版双粒度预留)',
  `org_id` BIGINT DEFAULT NULL COMMENT '组织ID(专业版多组织预留)',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目信息表';

-- ----------------------------
-- 2. 项目预算科目表（审批通过生成并汇入大类，T5.1）
-- ----------------------------
DROP TABLE IF EXISTS `fee_project_subject`;
CREATE TABLE `fee_project_subject` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `category_id` BIGINT COMMENT '费用大类ID',
  `category_name` VARCHAR(100) COMMENT '费用大类名称',
  `budget_subject_id` BIGINT COMMENT '预算科目ID',
  `subject_name` VARCHAR(100) COMMENT '科目名称(可覆盖)',
  `budget_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '项目级预算金额',
  `used_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '已用金额',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project_category` (`project_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目预算科目表';

-- ----------------------------
-- 3. 项目成员表（数据范围过滤依据，T2.3）
-- ----------------------------
DROP TABLE IF EXISTS `fee_project_member`;
CREATE TABLE `fee_project_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `user_name` VARCHAR(50) COMMENT '用户姓名',
  `role` VARCHAR(20) DEFAULT 'MEMBER' COMMENT 'MANAGER/MEMBER/VIEWER',
  `invest_ratio` INT DEFAULT NULL COMMENT '投入比例%(专业版成员负载预警,FEAT_MEMBER_LOAD)',
  `is_active` TINYINT DEFAULT 1 COMMENT '是否有效',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_project_user` (`project_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目成员表';

-- ----------------------------
-- 4. 付款单表（必选项目，老板审批，通过后扣项目预算+记总支出，T5.2）
-- ----------------------------
DROP TABLE IF EXISTS `fee_payment`;
CREATE TABLE `fee_payment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `payment_no` VARCHAR(30) NOT NULL COMMENT '付款单号 PAY-YYYYMMDD-XXX',
  `project_id` BIGINT NOT NULL COMMENT '关联项目(必选)',
  `payee` VARCHAR(200) NOT NULL COMMENT '收款方(对方)',
  `payer` VARCHAR(200) COMMENT '付款方(我方)',
  `amount` DECIMAL(14,2) NOT NULL COMMENT '付款金额',
  `pay_type` VARCHAR(30) COMMENT 'ENGINEERING/MATERIAL/SERVICE/RENT/OTHER',
  `payment_date` DATE COMMENT '付款日期',
  `invoice_status` VARCHAR(20) DEFAULT 'NONE' COMMENT 'HAS/NONE/PENDING',
  `invoice_urls` TEXT COMMENT '发票附件',
  `contract_urls` TEXT COMMENT '合同/验收单附件',
  `description` VARCHAR(500) COMMENT '说明',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `approved_by` BIGINT COMMENT '审批人',
  `approved_at` DATETIME COMMENT '审批时间',
  `process_instance_id` VARCHAR(64) COMMENT '流程实例ID',
  `department_id` BIGINT DEFAULT NULL COMMENT '部门ID(高级版预留)',
  `org_id` BIGINT DEFAULT NULL COMMENT '组织ID(专业版预留)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='付款单表';

-- ----------------------------
-- 5. 收款单表（必选项目，财务审核，联动生成已核验收入确认，T5.3）
-- ----------------------------
DROP TABLE IF EXISTS `fee_receipt`;
CREATE TABLE `fee_receipt` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `receipt_no` VARCHAR(30) NOT NULL COMMENT '收款单号 RCP-YYYYMMDD-XXX',
  `project_id` BIGINT NOT NULL COMMENT '关联项目(必选)',
  `payer` VARCHAR(200) NOT NULL COMMENT '付款方(对方)',
  `amount` DECIMAL(14,2) NOT NULL COMMENT '收款金额',
  `receipt_type` VARCHAR(30) COMMENT 'PROJECT_PAYMENT/REFUND/OTHER',
  `received_date` DATE COMMENT '收款日期',
  `proof_urls` TEXT COMMENT '回单附件',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
  `approved_by` BIGINT COMMENT '审核人',
  `approved_at` DATETIME COMMENT '审核时间',
  `department_id` BIGINT DEFAULT NULL COMMENT '部门ID(高级版预留)',
  `org_id` BIGINT DEFAULT NULL COMMENT '组织ID(专业版预留)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收款单表';

-- ----------------------------
-- 6. 收入确认表（三态：待核验/已核验/已驳回，T5.4 / ADR-021）
-- ----------------------------
DROP TABLE IF EXISTS `fee_income_confirmation`;
CREATE TABLE `fee_income_confirmation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `confirmed_amount` DECIMAL(14,2) NOT NULL COMMENT '确认收入金额',
  `confirm_date` DATE COMMENT '确认日期',
  `basis_type` VARCHAR(30) COMMENT 'CONTRACT/INVOICE/RECEIPT/OTHER',
  `basis_no` VARCHAR(100) COMMENT '依据编号',
  `basis_urls` TEXT COMMENT '依据附件',
  `confirmed_by` BIGINT COMMENT '创建人(项目经理/财务)',
  `confirmed_by_name` VARCHAR(50) COMMENT '创建人姓名',
  `verify_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING待核验/VERIFIED已核验/REJECTED已驳回',
  `verify_by` BIGINT COMMENT '核验人(财务)',
  `verify_at` DATETIME COMMENT '核验时间',
  `reject_reason` VARCHAR(500) COMMENT '驳回原因',
  `org_id` BIGINT DEFAULT NULL COMMENT '组织ID(专业版预留)',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project_status` (`project_id`, `verify_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收入确认表(三态)';

-- ----------------------------
-- 7. 合同变更表（增量叠加不回溯，分步生效，30/60/90逾期升级，T5.5 / ADR-022）
-- ----------------------------
DROP TABLE IF EXISTS `fee_project_change`;
CREATE TABLE `fee_project_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `change_type` VARCHAR(30) COMMENT 'CONTRACT_INCREASE/BUDGET_INCREASE/COMPREHENSIVE',
  `change_reason` VARCHAR(500) COMMENT '变更原因',
  `old_contract_amount` DECIMAL(14,2) COMMENT '原合同额',
  `new_contract_amount` DECIMAL(14,2) COMMENT '新合同额',
  `contract_delta` DECIMAL(14,2) DEFAULT 0 COMMENT '合同额变更量',
  `budget_deltas` JSON COMMENT '按大类预算变更 {"差旅招待":2000}',
  `change_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CONFIRMED/OVERDUE',
  `basis_urls` TEXT COMMENT '依据附件',
  `created_by` BIGINT COMMENT '发起人',
  `approved_by` BIGINT COMMENT '审批人(老板)',
  `approved_at` DATETIME COMMENT '审批时间',
  `confirmed_at` DATETIME COMMENT '收款确认时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合同变更表';

-- ----------------------------
-- 8. 烂账记录表（风险标记+坏账计提+项目状态LOST联动，T5.6）
-- ----------------------------
DROP TABLE IF EXISTS `fee_bad_debt`;
CREATE TABLE `fee_bad_debt` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `marked_by` BIGINT COMMENT '标记人',
  `marked_at` DATETIME COMMENT '标记时间',
  `reason` VARCHAR(500) COMMENT '原因说明',
  `type` VARCHAR(50) COMMENT 'LOST_CONTACT/QUALITY_DISPUTE/PRICE_ISSUE/INCOMPLETE/OTHER',
  `proposal` VARCHAR(30) COMMENT 'BAD_DEBT/FOLLOW_UP/LEGAL',
  `provision_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '计提坏账金额',
  `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN/FOLLOWING/CLOSED',
  `follow_up_notes` TEXT COMMENT '跟进记录',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='烂账记录表';

-- ----------------------------
-- 9. 项目关联规则表（三层：角色硬规则/大类软提示/项目级，T5.7 / ADR-023）
-- ----------------------------
DROP TABLE IF EXISTS `fee_project_link_rule`;
CREATE TABLE `fee_project_link_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID(0=内置默认模板)',
  `scope` VARCHAR(20) NOT NULL COMMENT 'ROLE/CATEGORY/PROJECT',
  `target` VARCHAR(100) COMMENT '角色标识/大类ID/项目ID',
  `rule` VARCHAR(20) NOT NULL COMMENT 'REQUIRED/OPTIONAL/NO_PROMPT',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目关联规则表';

-- 内置默认规则（租户开通时按 tenant_id=0 模板复制；先删后插幂等）
DELETE FROM `fee_project_link_rule` WHERE `tenant_id` = 0;
INSERT INTO `fee_project_link_rule` (`tenant_id`, `scope`, `target`, `rule`, `is_enabled`) VALUES
(0, 'ROLE', 'MANAGER', 'REQUIRED', 1),
(0, 'ROLE', 'EMPLOYEE', 'OPTIONAL', 1),
(0, 'ROLE', 'FINANCE', 'OPTIONAL', 1),
(0, 'CATEGORY', 'TRAVEL', 'OPTIONAL', 1),
(0, 'CATEGORY', 'PROCUREMENT', 'OPTIONAL', 1);

-- ----------------------------
-- 10. 补票记录表（无发票后补通道，T5.8 / ADR-018）
-- ----------------------------
DROP TABLE IF EXISTS `fee_invoice_supplement`;
CREATE TABLE `fee_invoice_supplement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `bill_id` BIGINT NOT NULL COMMENT '关联报销单',
  `invoice_urls` TEXT COMMENT '补传发票',
  `ocr_result` TEXT COMMENT 'OCR识别结果',
  `ocr_amount` DECIMAL(14,2) COMMENT 'OCR识别金额',
  `verify_passed` TINYINT DEFAULT NULL COMMENT '发票验真结果',
  `supplement_date` DATE COMMENT '补票时间',
  `estimated_date` DATE COMMENT '预计补票日期(默认提交日+7天)',
  `reserved_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '额度预占金额(审批通过预占,补票转实扣,超时释放)',
  `audit_reason` VARCHAR(500) COMMENT '财务审核理由(后补单必填)',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING待补/COMPLETED已补/OVERDUE超时异常',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_bill` (`bill_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='补票记录表';

-- ----------------------------
-- 11. 员工薪资表（公司级方案A，T5.9 / ADR-024）
-- ----------------------------
DROP TABLE IF EXISTS `fee_staff_salary`;
CREATE TABLE `fee_staff_salary` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT COMMENT '用户ID',
  `user_name` VARCHAR(50) COMMENT '用户姓名',
  `monthly_base` DECIMAL(14,2) DEFAULT 0 COMMENT '月薪基数',
  `social_rate` DECIMAL(5,4) DEFAULT 0 COMMENT '社保比例',
  `social_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '社保公司部分',
  `provident_rate` DECIMAL(5,4) DEFAULT 0 COMMENT '公积金比例',
  `provident_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '公积金公司部分',
  `other_allowance` DECIMAL(14,2) DEFAULT 0 COMMENT '其他补贴',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/RESIGNED',
  `effective_date` DATE COMMENT '生效日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工薪资表';

-- ----------------------------
-- 12. 薪资计提记录表（每月1号 salaryAccrualJob 计提）
-- ----------------------------
DROP TABLE IF EXISTS `fee_salary_record`;
CREATE TABLE `fee_salary_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `accrual_month` CHAR(7) NOT NULL COMMENT '计提月份 yyyy-MM',
  `user_id` BIGINT COMMENT '用户ID',
  `user_name` VARCHAR(50) COMMENT '用户姓名',
  `base_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '月薪基数',
  `social_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '社保公司部分',
  `provident_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '公积金公司部分',
  `allowance_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '其他补贴',
  `total_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '计提总额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_month` (`tenant_id`, `accrual_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资计提记录表';

-- ----------------------------
-- 13. 资产表（折旧规则财务设，系统自动算，T5.9 / ADR-026）
-- ----------------------------
DROP TABLE IF EXISTS `fee_asset`;
CREATE TABLE `fee_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `asset_name` VARCHAR(200) NOT NULL COMMENT '资产名称',
  `category` VARCHAR(30) COMMENT 'ELECTRONIC/FURNITURE/VEHICLE/OFFICE',
  `original_value` DECIMAL(14,2) DEFAULT 0 COMMENT '原值',
  `residual_value` DECIMAL(14,2) DEFAULT 0 COMMENT '残值',
  `life_months` INT DEFAULT 0 COMMENT '使用年限(月)',
  `monthly_depreciation` DECIMAL(14,2) DEFAULT 0 COMMENT '月折旧额',
  `depreciated_months` INT DEFAULT 0 COMMENT '已折旧月数',
  `net_value` DECIMAL(14,2) DEFAULT 0 COMMENT '净值',
  `attribution_type` VARCHAR(20) DEFAULT 'COMPANY' COMMENT 'COMPANY/PROJECT',
  `project_id` BIGINT COMMENT '归属项目(可空=公司级)',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SCRAPPED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产表';

-- ----------------------------
-- 14. 折旧记录表（每月1号 assetDepreciationJob，含项目分摊）
-- ----------------------------
DROP TABLE IF EXISTS `fee_asset_depreciation`;
CREATE TABLE `fee_asset_depreciation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `asset_id` BIGINT NOT NULL COMMENT '资产ID',
  `accrual_month` CHAR(7) NOT NULL COMMENT '计提月份 yyyy-MM',
  `amount` DECIMAL(14,2) DEFAULT 0 COMMENT '月折旧额',
  `project_id` BIGINT COMMENT '分摊项目(可空=公司级)',
  `share_ratio` DECIMAL(5,4) DEFAULT NULL COMMENT '项目分摊比例',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_month` (`tenant_id`, `accrual_month`),
  KEY `idx_asset` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折旧记录表';

-- ----------------------------
-- 15. 折旧规则表（财务设置：资产分类/年限/残值率）
-- ----------------------------
DROP TABLE IF EXISTS `fee_depreciation_rule`;
CREATE TABLE `fee_depreciation_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `category` VARCHAR(30) NOT NULL COMMENT '资产分类 ELECTRONIC/FURNITURE/VEHICLE/OFFICE',
  `life_months` INT NOT NULL COMMENT '使用年限(月)',
  `residual_rate` DECIMAL(5,4) DEFAULT 0 COMMENT '残值率',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_category` (`tenant_id`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='折旧规则表';

-- ----------------------------
-- 16. 项目分成表（老板定比例，项目经理仅可申请，T5.9 / ADR-025）
-- ----------------------------
DROP TABLE IF EXISTS `fee_project_sharing`;
CREATE TABLE `fee_project_sharing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `project_id` BIGINT NOT NULL COMMENT '项目ID',
  `share_to_user_id` BIGINT COMMENT '分成对象用户ID',
  `share_to_user_name` VARCHAR(50) COMMENT '分成对象姓名',
  `role` VARCHAR(30) COMMENT 'INTRODUCER/PARTNER/CHANNEL',
  `base_type` VARCHAR(20) COMMENT 'CONTRACT/INCOME/PROFIT/FIXED',
  `base_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '分成基数',
  `ratio` DECIMAL(5,4) DEFAULT 0 COMMENT '分成比例(老板定)',
  `share_amount` DECIMAL(14,2) DEFAULT 0 COMMENT '分成金额',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/ACTIVE/PAID',
  `basis_url` TEXT COMMENT '依据附件',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目分成表';

-- ----------------------------
-- 17. 贷款表（财务设参数，老板只读，T5.9 / ADR-027）
-- ----------------------------
DROP TABLE IF EXISTS `fee_loan`;
CREATE TABLE `fee_loan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `loan_name` VARCHAR(200) NOT NULL COMMENT '贷款名称',
  `amount` DECIMAL(14,2) DEFAULT 0 COMMENT '贷款金额',
  `annual_rate` DECIMAL(7,4) DEFAULT 0 COMMENT '年利率',
  `method` VARCHAR(30) COMMENT 'EQUAL_INSTALLMENT/INTEREST_FIRST',
  `start_date` DATE COMMENT '开始日期',
  `end_date` DATE COMMENT '结束日期',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/SETTLED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贷款表';

-- ----------------------------
-- 18. 利息计提记录表（每月1号 loanInterestJob）
-- ----------------------------
DROP TABLE IF EXISTS `fee_loan_interest`;
CREATE TABLE `fee_loan_interest` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `loan_id` BIGINT NOT NULL COMMENT '贷款ID',
  `accrual_month` CHAR(7) NOT NULL COMMENT '计提月份 yyyy-MM',
  `amount` DECIMAL(14,2) DEFAULT 0 COMMENT '月利息金额',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_month` (`tenant_id`, `accrual_month`),
  KEY `idx_loan` (`loan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='利息计提记录表';

-- ----------------------------
-- 19. 凭证导出模板表（内置7套，T5.11 / ADR-017/020；自定义=高级版见M1_03）
-- ----------------------------
DROP TABLE IF EXISTS `fee_export_template`;
CREATE TABLE `fee_export_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID(0=内置模板)',
  `template_code` VARCHAR(30) NOT NULL COMMENT '模板编码',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `target_system` VARCHAR(100) COMMENT '适用财务系统',
  `column_config` JSON COMMENT '列头定义',
  `delimiter` VARCHAR(10) DEFAULT ',' COMMENT '分隔符',
  `is_builtin` TINYINT DEFAULT 0 COMMENT '是否内置(内置不可删)',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`, `template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='凭证导出模板表';

-- 内置 7 套模板（先删后插幂等）
DELETE FROM `fee_export_template` WHERE `tenant_id` = 0 AND `is_builtin` = 1;
INSERT INTO `fee_export_template` (`tenant_id`, `template_code`, `template_name`, `target_system`, `delimiter`, `is_builtin`, `sort_order`) VALUES
(0, 'GENERAL', '通用模板', '通用财务软件', ',', 1, 1),
(0, 'KINGDEE', '金蝶模板', '金蝶云星空/金蝶KIS', ',', 1, 2),
(0, 'YONYOU', '用友模板', '用友U8/用友T+', ',', 1, 3),
(0, 'CHANJET', '畅捷通模板', '畅捷通好会计/好生意', ',', 1, 4),
(0, 'SUPERDATA', '速达模板', '速达3000/5000', ',', 1, 5),
(0, 'GRASP', '管家婆模板', '管家婆辉煌版', ',', 1, 6),
(0, 'LEMONCLOUD', '柠檬云模板', '柠檬云财务', ',', 1, 7);
