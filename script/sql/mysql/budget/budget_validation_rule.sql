-- ----------------------------
-- 预算科目规则校验表
-- ----------------------------
DROP TABLE IF EXISTS `budget_validation_rule`;
CREATE TABLE `budget_validation_rule` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码（唯一）',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(30) NOT NULL COMMENT '规则类型（REQUIRED-必填, MIN_AMOUNT-最低金额, MAX_AMOUNT-最高金额, RATIO_LIMIT-比例限制, SUM_CHECK-合计校验, FORMULA-公式校验）',
  `severity_level` VARCHAR(20) NOT NULL DEFAULT 'ERROR' COMMENT '严重级别（ERROR-错误/红色阻断, WARNING-警告/黄色, INFO-建议/蓝色）',
  `subject_type` VARCHAR(20) DEFAULT NULL COMMENT '适用科目类型（NULL表示全部类型）',
  `subject_code` VARCHAR(20) DEFAULT NULL COMMENT '适用科目编码（NULL表示该类型全部）',
  `threshold_value` DECIMAL(18,2) DEFAULT NULL COMMENT '阈值',
  `threshold_value2` DECIMAL(18,2) DEFAULT NULL COMMENT '阈值2（用于范围校验）',
  `rule_expression` VARCHAR(500) DEFAULT NULL COMMENT '规则表达式/说明',
  `error_message` VARCHAR(500) NOT NULL COMMENT '校验不通过时的提示信息',
  `is_active` TINYINT(1) DEFAULT 1 COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` INT(4) DEFAULT 0 COMMENT '排序号',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '规则说明',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_severity` (`severity_level`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='预算科目规则校验表';

-- ----------------------------
-- 初始化校验规则数据
-- ----------------------------
INSERT INTO `budget_validation_rule` (`rule_code`, `rule_name`, `rule_type`, `severity_level`, `subject_type`, `subject_code`, `threshold_value`, `threshold_value2`, `rule_expression`, `error_message`, `is_active`, `sort_order`, `description`, `create_by`, `create_time`) VALUES
-- 必填校验
('REQ_ALL', '所有叶子科目必须填写预算金额', 'REQUIRED', 'INFO', NULL, NULL, NULL, NULL, NULL, '存在未填写预算金额的叶子科目，请补充完整', 1, 1, '检查所有叶子科目是否已填写预算金额', 'admin', NOW()),

-- 最低金额校验
('MIN_INCOME', '收入类科目预算金额不得为负', 'MIN_AMOUNT', 'ERROR', 'INCOME', NULL, 0, NULL, 'amount >= 0', '收入类科目预算金额不能为负数', 1, 2, '收入类科目金额必须 >= 0', 'admin', NOW()),
('MIN_COST', '成本类科目预算金额不得为负', 'MIN_AMOUNT', 'ERROR', 'COST', NULL, 0, NULL, 'amount >= 0', '成本类科目预算金额不能为负数', 1, 3, '成本类科目金额必须 >= 0', 'admin', NOW()),
('MIN_EXPENSE', '费用类科目预算金额不得为负', 'MIN_AMOUNT', 'ERROR', 'EXPENSE', NULL, 0, NULL, 'amount >= 0', '费用类科目预算金额不能为负数', 1, 4, '费用类科目金额必须 >= 0', 'admin', NOW()),

-- 最高金额预警
('MAX_SINGLE_EXPENSE', '单项费用预算超过100万', 'MAX_AMOUNT', 'WARNING', 'EXPENSE', NULL, 1000000, NULL, 'amount <= 1000000', '单项费用类预算金额超过100万元，请确认是否合理', 1, 5, '单项费用超过100万时警告', 'admin', NOW()),
('MAX_SINGLE_COST', '单项成本预算超过200万', 'MAX_AMOUNT', 'WARNING', 'COST', NULL, 2000000, NULL, 'amount <= 2000000', '单项成本类预算金额超过200万元，请确认是否合理', 1, 6, '单项成本超过200万时警告', 'admin', NOW()),

-- 比例限制
('RATIO_ADMIN_FEE', '管理费用占费用类总额比例建议不超过40%', 'RATIO_LIMIT', 'WARNING', 'EXPENSE', '3002', NULL, NULL, 'subject_sum / type_total <= 0.4', '管理费用占费用类总额比例超过40%，建议优化费用结构', 1, 7, '管理费用占比校验', 'admin', NOW()),
('RATIO_SALES_FEE', '销售费用占收入类总额比例建议不超过15%', 'RATIO_LIMIT', 'INFO', 'EXPENSE', '3001', NULL, NULL, 'sales_fee / income_total <= 0.15', '销售费用占收入比例超过15%，建议关注费效比', 1, 8, '销售费用占收入比校验', 'admin', NOW()),

-- 合计校验
('SUM_INCOME_NOT_ZERO', '收入类预算总额不能为零', 'SUM_CHECK', 'ERROR', 'INCOME', NULL, NULL, NULL, 'type_total > 0', '收入类科目预算总额为零，请至少填写一项收入预算', 1, 9, '收入类合计不能为零', 'admin', NOW()),

-- 建议性规则
('INFO_ZERO_BASED', '建议采用零基预算法逐项审核', 'FORMULA', 'INFO', NULL, NULL, NULL, NULL, NULL, '建议对每个科目采用零基预算法重新审核，避免惯性预算', 1, 10, '零基预算建议', 'admin', NOW()),
('INFO_ROUND_CHECK', '预算金额建议取整到百位', 'FORMULA', 'INFO', NULL, NULL, NULL, NULL, 'amount % 100 == 0', '预算金额未取整到百位，建议调整为整百数以提升预算编制质量', 1, 11, '金额取整建议', 'admin', NOW());
