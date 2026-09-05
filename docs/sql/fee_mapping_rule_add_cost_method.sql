-- =============================================
-- 映射规则增加费用确认方式(cost_method)字段
-- 用于区分：折旧(DEPRECIATION) / 计提(ACCRUAL) / 费率(FEE_RATE) / 直接费用化(DIRECT)
-- 执行时间：2026-07-25
-- =============================================

-- 1. fee_mapping_rule 增加 cost_method 字段
ALTER TABLE fee_mapping_rule
ADD COLUMN `cost_method` VARCHAR(20) DEFAULT 'DIRECT' COMMENT '费用确认方式(DIRECT直接费用化/DEPRECIATION折旧/ACCRUAL计提摊销/FEE_RATE费率利息)' AFTER `is_fixed_asset`;

-- 2. fee_expense_bill 增加 cost_method 字段
ALTER TABLE fee_expense_bill
ADD COLUMN `cost_method` VARCHAR(20) DEFAULT 'DIRECT' COMMENT '费用确认方式(DIRECT/DEPRECIATION/ACCRUAL/FEE_RATE)' AFTER `expenditure_type`;

-- 3. 根据 is_fixed_asset 回填已有映射规则的 cost_method
-- 固定资产 → 折旧
UPDATE fee_mapping_rule SET cost_method = 'DEPRECIATION' WHERE is_fixed_asset = 1;

-- 4. 特殊物资小类设置默认计提方式
-- 软件授权：无形资产按月摊销 → 计提
UPDATE fee_mapping_rule SET cost_method = 'ACCRUAL' WHERE item_category = 'SOFTWARE_LICENSE';

-- 咨询服务（项目交付）：按合同期计提
UPDATE fee_mapping_rule SET cost_method = 'ACCRUAL' WHERE item_category = 'CONSULTING' AND purpose_tag = 'PROJECT_DELIVERY';

-- 5. 回填已有报销单的 cost_method
UPDATE fee_expense_bill SET cost_method = 'DEPRECIATION' WHERE is_fixed_asset = 1;
UPDATE fee_expense_bill SET cost_method = 'DIRECT' WHERE is_fixed_asset = 0 OR is_fixed_asset IS NULL;
