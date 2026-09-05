-- 报销单表补充缺失字段（useful_life等）
-- 执行时间：2026-07-19

-- 1. 新增使用年限字段（固定资产折旧用）
ALTER TABLE fee_expense_bill
ADD COLUMN `useful_life` INT DEFAULT NULL COMMENT '使用年限(年)，仅固定资产时填写' AFTER `is_fixed_asset`;

-- 2. 新增支出类型字段
ALTER TABLE fee_expense_bill
ADD COLUMN `expenditure_type` VARCHAR(20) DEFAULT 'OPERATING' COMMENT '支出类型(CAPITAL/OPERATING)' AFTER `useful_life`;

-- 3. 新增超预算标识字段
ALTER TABLE fee_expense_bill
ADD COLUMN `is_over_budget` TINYINT DEFAULT 0 COMMENT '是否超预算(0否 1是)' AFTER `expenditure_type`;

-- 4. 新增关联超预算审批单ID字段
ALTER TABLE fee_expense_bill
ADD COLUMN `over_budget_approval_id` BIGINT DEFAULT NULL COMMENT '关联超预算审批单ID' AFTER `is_over_budget`;

-- 5. 新增分摊状态字段
ALTER TABLE fee_expense_bill
ADD COLUMN `allocation_status` VARCHAR(20) DEFAULT 'NONE' COMMENT '分摊状态(NONE/PENDING/ALLOCATED)' AFTER `status`;
