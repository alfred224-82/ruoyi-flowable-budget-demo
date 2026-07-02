-- ----------------------------
-- 预算编制相关表结构
-- ----------------------------

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1、预算编制主表
-- ----------------------------
DROP TABLE IF EXISTS `budget_preparation`;
CREATE TABLE `budget_preparation` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_no` VARCHAR(50) NOT NULL COMMENT '预算单号（唯一，格式：BG-YYYYMM-XXX）',
  `org_id` BIGINT(20) NOT NULL COMMENT '组织ID（关联sys_dept表的dept_id）',
  `org_name` VARCHAR(100) DEFAULT NULL COMMENT '组织名称（冗余字段，便于查询）',
  `budget_year` INT(4) NOT NULL COMMENT '预算年度',
  `budget_month` INT(2) NOT NULL COMMENT '预算月份（1-12）',
  `budget_period` VARCHAR(7) GENERATED ALWAYS AS (CONCAT(`budget_year`, '-', LPAD(`budget_month`, 2, '0'))) STORED COMMENT '预算期间（格式：YYYY-MM，自动生成）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'Draft' COMMENT '状态（Draft-草稿，Pending_Review-待审核，Branch_Pending-分公司待审，Pending_Revision-待修订，Approved-已批准，Rejected-已驳回）',
  `reject_level` VARCHAR(10) DEFAULT 'None' COMMENT '驳回来源（HQ-总部，Branch-分公司，None-无）',
  `reject_reason` TEXT COMMENT '驳回理由',
  `deadline_time` DATETIME DEFAULT NULL COMMENT '截止时间',
  `current_handler` VARCHAR(50) DEFAULT NULL COMMENT '当前处理人（用户账号）',
  `total_budget` DECIMAL(18,2) DEFAULT 0.00 COMMENT '预算总额（自动汇总明细表）',
  `total_actual` DECIMAL(18,2) DEFAULT 0.00 COMMENT '实际总额',
  `variance_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '差异率（百分比）',
  `process_instance_id` VARCHAR(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sheet_no` (`sheet_no`),
  KEY `idx_org_period` (`org_id`, `budget_year`, `budget_month`),
  KEY `idx_status` (`status`),
  KEY `idx_budget_period` (`budget_period`),
  KEY `idx_process_instance_id` (`process_instance_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='预算编制主表';

-- ----------------------------
-- 2、预算编制明细表
-- ----------------------------
DROP TABLE IF EXISTS `budget_preparation_detail`;
CREATE TABLE `budget_preparation_detail` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` BIGINT(20) NOT NULL COMMENT '预算单ID（外键，关联budget_preparation.id）',
  `subject_code` VARCHAR(20) NOT NULL COMMENT '科目编码（关联budget_subject.subject_code）',
  `subject_name` VARCHAR(100) DEFAULT NULL COMMENT '科目名称（冗余字段）',
  `subject_type` VARCHAR(20) DEFAULT NULL COMMENT '科目类型（INCOME-收入类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类，COST-成本类）',
  `budget_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '预算金额',
  `actual_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '实际金额',
  `variance_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '差异金额（自动计算：预算-实际）',
  `variance_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '差异率（百分比）',
  `dept_id` BIGINT(20) DEFAULT NULL COMMENT '部门ID（关联sys_dept.dept_id）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '部门名称（冗余字段）',
  `sort_order` INT(4) DEFAULT 0 COMMENT '排序号',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_subject_code` (`subject_code`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='预算编制明细表';

-- ----------------------------
-- 3、预算编制校验规则表
-- ----------------------------
DROP TABLE IF EXISTS `budget_validation_rule`;
CREATE TABLE `budget_validation_rule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码（唯一）',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_category` VARCHAR(20) NOT NULL COMMENT '规则类别（BUDGET_OVER-预算超标，LOGIC_ERROR-逻辑错误，SUBJECT_ERROR-科目错误，MISSING_DATA-数据缺失，HISTORICAL_COMPARE-历史对比，FORMULA_CHECK-公式校验）',
  `threshold_value` DECIMAL(10,2) DEFAULT NULL COMMENT '阈值',
  `risk_level` VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级（HIGH-高，MEDIUM-中，LOW-低）',
  `validation_scope` VARCHAR(20) DEFAULT 'ALL' COMMENT '校验范围（ALL-全部，INCOME-收入类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类，COST-成本类）',
  `is_strict` TINYINT(1) DEFAULT 0 COMMENT '是否严格校验（0-警告，1-阻止提交）',
  `rule_formula` VARCHAR(500) DEFAULT NULL COMMENT '校验公式或表达式',
  `rule_description` VARCHAR(500) DEFAULT NULL COMMENT '规则描述',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '错误提示信息',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` INT(4) DEFAULT 0 COMMENT '排序号',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_rule_category` (`rule_category`),
  KEY `idx_is_active` (`is_active`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='预算编制校验规则表';

-- ----------------------------
-- 初始化-预算编制校验规则表数据
-- ----------------------------
INSERT INTO `budget_validation_rule` VALUES
(1, 'VAL_001', '预算超标检查', 'BUDGET_OVER', 20.00, 'HIGH', 'ALL', 0, NULL, '部门预算与上月实际偏差超过±20%', '预算金额超出合理范围，请核实', 1, 1, 'admin', NOW(), '', NULL, NULL),
(2, 'VAL_002', '汇总逻辑检查', 'LOGIC_ERROR', NULL, 'HIGH', 'ALL', 1, 'SUM(detail.budget_amount) = header.total_budget', '明细合计与汇总金额不一致', '明细金额汇总必须等于预算总额', 1, 2, 'admin', NOW(), '', NULL, NULL),
(3, 'VAL_003', '科目有效性检查', 'SUBJECT_ERROR', NULL, 'MEDIUM', 'ALL', 1, NULL, '科目编码不存在于科目表或未启用', '请选择有效的预算科目', 1, 3, 'admin', NOW(), '', NULL, NULL),
(4, 'VAL_004', '必填字段完整性检查', 'MISSING_DATA', NULL, 'MEDIUM', 'ALL', 1, NULL, '必填字段存在空值', '请完善所有必填字段信息', 1, 4, 'admin', NOW(), '', NULL, NULL),
(5, 'VAL_005', '历史数据对比', 'HISTORICAL_COMPARE', 30.00, 'LOW', 'EXPENSE', 0, NULL, '本期与上期同期波动超过±30%', '预算金额与历史数据偏差较大，请确认', 1, 5, 'admin', NOW(), '', NULL, NULL),
(6, 'VAL_006', '收入类预算公式校验', 'FORMULA_CHECK', NULL, 'MEDIUM', 'INCOME', 0, 'budget_amount >= 0', '收入类科目预算金额不能为负数', '收入预算应为正数或零', 1, 6, 'admin', NOW(), '', NULL, NULL),
(7, 'VAL_007', '费用类预算合理性检查', 'BUDGET_OVER', 50.00, 'MEDIUM', 'EXPENSE', 0, NULL, '费用类科目预算超过上月实际50%', '费用预算增长过快，请提供说明', 1, 7, 'admin', NOW(), '', NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
