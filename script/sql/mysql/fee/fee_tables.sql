-- =============================================
-- 预算与核算映射优化（二期）数据库表结构
-- 版本：v1.0
-- 日期：2026-07-13
-- =============================================

-- ----------------------------
-- 1. 用途标签表 fee_purpose_tag
-- ----------------------------
DROP TABLE IF EXISTS `fee_purpose_tag`;
CREATE TABLE `fee_purpose_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tag_code` VARCHAR(50) NOT NULL COMMENT '标签编码',
  `tag_name` VARCHAR(200) NOT NULL COMMENT '标签名称',
  `tag_icon` VARCHAR(100) DEFAULT NULL COMMENT '标签图标',
  `default_accounting_class` VARCHAR(50) DEFAULT 'EXPENSE' COMMENT '默认会计大类',
  `require_project` TINYINT DEFAULT 0 COMMENT '是否要求项目编号(0否 1是)',
  `sort_order` INT DEFAULT 0 COMMENT '排序号',
  `status` VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用途标签表';

-- ----------------------------
-- 4. 映射规则表 fee_mapping_rule
-- ----------------------------
DROP TABLE IF EXISTS `fee_mapping_rule`;
CREATE TABLE `fee_mapping_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码',
  `item_category` VARCHAR(100) NOT NULL COMMENT '物资小类编码',
  `purpose_tag` VARCHAR(50) NOT NULL COMMENT '用途标签编码',
  `budget_subject` VARCHAR(100) NOT NULL COMMENT '目标预算科目编码',
  `accounting_class` VARCHAR(50) NOT NULL COMMENT '归属会计大类',
  `is_fixed_asset` TINYINT DEFAULT 0 COMMENT '是否固定资产(0否 1是)',
  `priority` INT DEFAULT 0 COMMENT '优先级(数值越大优先级越高)',
  `status` VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_item_purpose` (`item_category`, `purpose_tag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='映射规则表';

-- ----------------------------
-- 5. 报销单表 fee_expense_bill
-- ----------------------------
DROP TABLE IF EXISTS `fee_expense_bill`;
CREATE TABLE `fee_expense_bill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` VARCHAR(50) NOT NULL COMMENT '报销单号',
  `dept_id` BIGINT NOT NULL COMMENT '部门ID',
  `item_category` VARCHAR(100) NOT NULL COMMENT '物资小类编码',
  `purpose_tag` VARCHAR(50) NOT NULL COMMENT '用途标签编码',
  `project_code` VARCHAR(100) DEFAULT NULL COMMENT '项目编号',
  `calc_budget_subject` VARCHAR(100) DEFAULT NULL COMMENT '计算后预算科目编码',
  `calc_accounting_class` VARCHAR(50) DEFAULT NULL COMMENT '计算后会计大类',
  `amount` DECIMAL(18,2) NOT NULL COMMENT '报销金额',
  `expense_date` DATE NOT NULL COMMENT '费用发生日期',
  `is_fixed_asset` TINYINT DEFAULT 0 COMMENT '是否固定资产(0否 1是)',
  `useful_life` INT DEFAULT NULL COMMENT '使用年限(年)，仅固定资产时填写',
  `expenditure_type` VARCHAR(20) DEFAULT 'OPERATING' COMMENT '支出类型(CAPITAL/OPERATING)',
  `is_over_budget` TINYINT DEFAULT 0 COMMENT '是否超预算(0否 1是)',
  `over_budget_approval_id` BIGINT DEFAULT NULL COMMENT '关联超预算审批单ID',
  `status` VARCHAR(30) DEFAULT 'DRAFT' COMMENT '状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)',
  `allocation_status` VARCHAR(20) DEFAULT 'NONE' COMMENT '分摊状态(NONE/PENDING/ALLOCATED)',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`),
  KEY `idx_expense_date` (`expense_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报销单表';

-- ----------------------------
-- 6. 超预算审批表 fee_over_budget_approval
-- ----------------------------
DROP TABLE IF EXISTS `fee_over_budget_approval`;
CREATE TABLE `fee_over_budget_approval` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_id` BIGINT NOT NULL COMMENT '关联报销单ID',
  `budget_subject` VARCHAR(100) NOT NULL COMMENT '超预算的预算科目编码',
  `original_budget` DECIMAL(18,2) NOT NULL COMMENT '该科目原预算金额',
  `used_amount` DECIMAL(18,2) NOT NULL COMMENT '该科目已使用金额',
  `request_amount` DECIMAL(18,2) NOT NULL COMMENT '本次申请金额',
  `shortage_amount` DECIMAL(18,2) NOT NULL COMMENT '缺口金额',
  `approval_action` VARCHAR(20) DEFAULT NULL COMMENT '审批动作(APPROVE/REJECT/ADJUST)',
  `adjust_from_subject` VARCHAR(100) DEFAULT NULL COMMENT '调出科目编码',
  `adjust_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '调剂金额',
  `approver_id` BIGINT DEFAULT NULL COMMENT '审批人ID',
  `approval_opinion` VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
  `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSED)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='超预算审批表';

-- ----------------------------
-- 7. 预算科目调剂流水表 fee_budget_adjustment_log
-- ----------------------------
DROP TABLE IF EXISTS `fee_budget_adjustment_log`;
CREATE TABLE `fee_budget_adjustment_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `from_subject` VARCHAR(100) NOT NULL COMMENT '调出科目编码',
  `to_subject` VARCHAR(100) NOT NULL COMMENT '调入科目编码',
  `adjust_amount` DECIMAL(18,2) NOT NULL COMMENT '调剂金额',
  `related_approval_id` BIGINT DEFAULT NULL COMMENT '关联审批单ID',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operate_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_approval_id` (`related_approval_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算科目调剂流水表';

-- ----------------------------
-- 8. 费用分摊记录表 fee_cost_allocation
-- ----------------------------
DROP TABLE IF EXISTS `fee_cost_allocation`;
CREATE TABLE `fee_cost_allocation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_id` BIGINT NOT NULL COMMENT '关联报销单ID',
  `allocation_batch` VARCHAR(20) NOT NULL COMMENT '分摊批次(YYYY-MM)',
  `target_dept_id` BIGINT NOT NULL COMMENT '目标部门ID',
  `alloc_amount` DECIMAL(18,2) NOT NULL COMMENT '分摊金额',
  `alloc_ratio` DECIMAL(10,4) NOT NULL COMMENT '分摊比例',
  `alloc_rule_id` BIGINT DEFAULT NULL COMMENT '分摊规则ID',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_batch` (`allocation_batch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用分摊记录表';

-- ----------------------------
-- 9. 费用分摊规则表 fee_cost_allocation_rule
-- ----------------------------
DROP TABLE IF EXISTS `fee_cost_allocation_rule`;
CREATE TABLE `fee_cost_allocation_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` VARCHAR(200) NOT NULL COMMENT '规则名称',
  `allocation_method` VARCHAR(30) NOT NULL COMMENT '分摊方法(HEAD_COUNT/WORK_HOUR/RATIO)',
  `target_dept_id` BIGINT NOT NULL COMMENT '目标部门ID',
  `weight` DECIMAL(10,4) NOT NULL COMMENT '权重',
  `status` VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用分摊规则表';
