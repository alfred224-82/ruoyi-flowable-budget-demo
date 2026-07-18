-- =====================================================
-- Phase 1: 费用管控新功能建表脚本
-- 包含: 超预算审批单、预算调剂流水、操作日志、预留预算、预算调整申请、预留预算分摊记录
-- 同时: 为 fee_expense_bill 增加审批相关字段
-- =====================================================

-- 1. 超预算审批单表（如已存在则跳过）
CREATE TABLE IF NOT EXISTS `fee_over_budget_approval` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_id` BIGINT(20) NOT NULL COMMENT '关联报销单ID',
  `bill_no` VARCHAR(50) DEFAULT NULL COMMENT '关联报销单号',
  `cost_center_code` VARCHAR(50) DEFAULT NULL COMMENT '预算责任中心编码',
  `budget_subject` VARCHAR(50) NOT NULL COMMENT '超预算的预算科目编码',
  `budget_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '预算科目名称',
  `original_budget` DECIMAL(15,2) NOT NULL COMMENT '该科目原预算金额',
  `used_amount` DECIMAL(15,2) NOT NULL COMMENT '该科目已使用金额',
  `request_amount` DECIMAL(15,2) NOT NULL COMMENT '本次申请金额',
  `shortage_amount` DECIMAL(15,2) NOT NULL COMMENT '缺口金额',
  `approval_action` VARCHAR(20) DEFAULT NULL COMMENT '审批动作：APPROVE/REJECT/ADJUST',
  `adjust_from_subject` VARCHAR(50) DEFAULT NULL COMMENT '调出科目编码',
  `adjust_from_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '调出科目名称',
  `adjust_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '调剂金额',
  `approver_id` BIGINT(20) DEFAULT NULL COMMENT '审批人ID',
  `approver_name` VARCHAR(50) DEFAULT NULL COMMENT '审批人姓名',
  `approval_opinion` TEXT COMMENT '审批意见',
  `approval_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`),
  KEY `idx_approver` (`approver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='超预算审批单表';

-- 2. 预算调剂流水表
CREATE TABLE IF NOT EXISTS `fee_budget_adjustment_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cost_center_code` VARCHAR(50) NOT NULL COMMENT '预算责任中心编码',
  `from_subject` VARCHAR(50) NOT NULL COMMENT '调出科目编码',
  `from_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '调出科目名称',
  `to_subject` VARCHAR(50) NOT NULL COMMENT '调入科目编码',
  `to_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '调入科目名称',
  `adjust_amount` DECIMAL(15,2) NOT NULL COMMENT '调剂金额',
  `related_approval_id` BIGINT(20) DEFAULT NULL COMMENT '关联超预算审批单ID',
  `related_bill_no` VARCHAR(50) DEFAULT NULL COMMENT '关联报销单号',
  `operator_id` BIGINT(20) NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `operate_time` DATETIME NOT NULL COMMENT '操作时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_approval_id` (`related_approval_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算调剂流水表';

-- 3. 操作日志表（仅INSERT+SELECT）
CREATE TABLE IF NOT EXISTS `fee_operation_log` (
  `log_id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module` VARCHAR(30) NOT NULL COMMENT '所属模块：EXPENSE/OVER_BUDGET/BUDGET_ADJUST',
  `operation_type` VARCHAR(30) NOT NULL COMMENT '操作类型',
  `bill_id` BIGINT(20) DEFAULT NULL COMMENT '关联报销单ID',
  `bill_no` VARCHAR(50) DEFAULT NULL COMMENT '报销单编号',
  `related_app_id` BIGINT(20) DEFAULT NULL COMMENT '关联预算调整申请ID',
  `operator_id` BIGINT(20) NOT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) NOT NULL COMMENT '操作人姓名',
  `operator_role` VARCHAR(30) DEFAULT NULL COMMENT '操作人角色',
  `before_status` VARCHAR(30) DEFAULT NULL COMMENT '操作前状态',
  `after_status` VARCHAR(30) DEFAULT NULL COMMENT '操作后状态',
  `operation_detail` TEXT COMMENT '操作详情（JSON格式）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作人IP地址',
  `operation_time` DATETIME NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_bill_no` (`bill_no`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用管控操作日志表（仅INSERT+SELECT）';

-- 4. 预留预算表
CREATE TABLE IF NOT EXISTS `fee_reserved_budget` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `budget_period` VARCHAR(20) NOT NULL COMMENT '预算期间',
  `level_type` VARCHAR(20) NOT NULL COMMENT '层级类型：DEPT/BRANCH/HQ',
  `dept_id` BIGINT(20) DEFAULT NULL COMMENT '部门ID（HQ级别为空）',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '部门名称',
  `total_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '预留预算总额度',
  `allocated_amount` DECIMAL(15,2) NOT NULL DEFAULT 0.00 COMMENT '已分摊金额',
  `available_amount` DECIMAL(15,2) GENERATED ALWAYS AS (`total_amount` - `allocated_amount`) STORED COMMENT '剩余可用额度（生成列）',
  `warning_threshold` DECIMAL(5,2) DEFAULT 80.00 COMMENT '预警阈值百分比',
  `creator_id` BIGINT(20) DEFAULT NULL COMMENT '编制人ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/FROZEN/EXPIRED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` VARCHAR(64) DEFAULT '',
  `update_by` VARCHAR(64) DEFAULT '',
  `remark` VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_period_level` (`budget_period`, `level_type`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预留预算表';

-- 5. 预算调整申请表
CREATE TABLE IF NOT EXISTS `fee_budget_adjustment_app` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_no` VARCHAR(50) NOT NULL COMMENT '申请单号（BA-YYYYMMDD-XXX）',
  `bill_id` BIGINT(20) DEFAULT NULL COMMENT '关联报销单ID',
  `bill_no` VARCHAR(50) DEFAULT NULL COMMENT '关联报销单号',
  `applicant_id` BIGINT(20) NOT NULL COMMENT '申请人ID',
  `applicant_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名',
  `dept_id` BIGINT(20) NOT NULL COMMENT '申请人部门ID',
  `dept_name` VARCHAR(100) DEFAULT NULL COMMENT '申请人部门名称',
  `item_category` VARCHAR(100) DEFAULT NULL COMMENT '物资小类编码',
  `purpose_tag` VARCHAR(50) DEFAULT NULL COMMENT '用途标签编码',
  `target_subject` VARCHAR(50) NOT NULL COMMENT '目标预算科目编码',
  `target_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '目标预算科目名称',
  `cost_center_code` VARCHAR(50) DEFAULT NULL COMMENT '责任中心编码',
  `current_balance` DECIMAL(15,2) NOT NULL COMMENT '申请时科目可用余额',
  `request_amount` DECIMAL(15,2) NOT NULL COMMENT '申请调整金额',
  `budget_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '预算金额',
  `reason` TEXT NOT NULL COMMENT '调整原因说明',
  `current_level` INT NOT NULL DEFAULT 1 COMMENT '当前审批层级：1部门/2分公司/3总公司',
  `dept_reserved_id` BIGINT(20) DEFAULT NULL COMMENT '部门预留预算ID',
  `branch_reserved_id` BIGINT(20) DEFAULT NULL COMMENT '分公司预留预算ID',
  `hq_reserved_id` BIGINT(20) DEFAULT NULL COMMENT '总公司预留预算ID',
  `allocate_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '实际分摊金额',
  `allocate_from_reserved_id` BIGINT(20) DEFAULT NULL COMMENT '实际分摊来源的预留预算ID',
  `reject_reason` TEXT DEFAULT NULL COMMENT '驳回原因',
  `status` VARCHAR(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `create_by` VARCHAR(64) DEFAULT '',
  `update_by` VARCHAR(64) DEFAULT '',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_no` (`app_no`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`),
  KEY `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算调整申请表';

-- 6. 预留预算分摊记录表
CREATE TABLE IF NOT EXISTS `fee_reserved_allocation_log` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reserved_id` BIGINT(20) NOT NULL COMMENT '关联预留预算ID',
  `application_id` BIGINT(20) NOT NULL COMMENT '关联预算调整申请ID',
  `app_no` VARCHAR(50) DEFAULT NULL COMMENT '关联申请单号',
  `target_subject` VARCHAR(50) NOT NULL COMMENT '目标预算科目编码',
  `target_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '目标预算科目名称',
  `allocate_amount` DECIMAL(15,2) NOT NULL COMMENT '分摊金额',
  `before_available` DECIMAL(15,2) NOT NULL COMMENT '分摊前科目可用余额',
  `after_available` DECIMAL(15,2) NOT NULL COMMENT '分摊后科目可用余额',
  `before_reserved_available` DECIMAL(15,2) NOT NULL COMMENT '分摊前预留剩余额度',
  `after_reserved_available` DECIMAL(15,2) NOT NULL COMMENT '分摊后预留剩余额度',
  `operator_id` BIGINT(20) NOT NULL COMMENT '操作人ID（审批人）',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `operate_time` DATETIME NOT NULL COMMENT '操作时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reserved_id` (`reserved_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预留预算分摊记录表';

-- 7. 为 fee_expense_bill 增加审批相关字段（如不存在）
-- 1. 申请人ID
ALTER TABLE `fee_expense_bill` ADD COLUMN `applicant_id` BIGINT(20) DEFAULT NULL COMMENT '申请人ID';
-- 2. 申请人姓名
ALTER TABLE `fee_expense_bill` ADD COLUMN `applicant_name` VARCHAR(50) DEFAULT NULL COMMENT '申请人姓名';
-- 3. 审批人ID
ALTER TABLE `fee_expense_bill` ADD COLUMN `approver_id` BIGINT(20) DEFAULT NULL COMMENT '审批人ID';
-- 4. 审批时间
ALTER TABLE `fee_expense_bill` ADD COLUMN `approve_time` DATETIME DEFAULT NULL COMMENT '审批时间';
-- 5. 驳回原因
ALTER TABLE `fee_expense_bill` ADD COLUMN `reject_reason` TEXT DEFAULT NULL COMMENT '驳回原因';
-- 6. 驳回时间
ALTER TABLE `fee_expense_bill` ADD COLUMN `reject_time` DATETIME DEFAULT NULL COMMENT '驳回时间';

-- 8. 预算调整申请表：将 shortage_amount 重命名为 budget_amount
-- 如已存在旧列，执行以下 ALTER 进行迁移
-- ALTER TABLE `fee_budget_adjustment_app` CHANGE `shortage_amount` `budget_amount` DECIMAL(15,2) DEFAULT NULL COMMENT '预算金额';

-- 9. 预算调整申请表：新增物资小类和用途标签字段
ALTER TABLE `fee_budget_adjustment_app` ADD COLUMN `item_category` VARCHAR(100) DEFAULT NULL COMMENT '物资小类编码' AFTER `dept_name`;
ALTER TABLE `fee_budget_adjustment_app` ADD COLUMN `purpose_tag` VARCHAR(50) DEFAULT NULL COMMENT '用途标签编码' AFTER `item_category`;
