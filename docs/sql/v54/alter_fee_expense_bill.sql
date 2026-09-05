-- V5.4 报销单增强：新增多币种、合规校验、补助相关字段
-- 增量升级 fee_expense_bill，适配 MySQL5.6 / 5.7 / 8.0

-- 1、多币种字段
ALTER TABLE `fee_expense_bill`
    ADD COLUMN `currency_code` varchar(10) DEFAULT 'CNY' COMMENT '币种编码' AFTER `amount`,
  ADD COLUMN `original_amount` decimal(18,2) DEFAULT NULL COMMENT '原币金额（外币时填写）' AFTER `currency_code`,
  ADD COLUMN `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '使用汇率' AFTER `original_amount`;

-- 2、补助相关字段
ALTER TABLE `fee_expense_bill`
    ADD COLUMN `subsidy_amount` decimal(18,2) DEFAULT NULL COMMENT '补助金额' AFTER `exchange_rate`,
  ADD COLUMN `travel_days` int DEFAULT NULL COMMENT '出差天数（用于补助核算）' AFTER `subsidy_amount`;

-- 3、合规校验状态
ALTER TABLE `fee_expense_bill`
    ADD COLUMN `compliance_status` varchar(20) DEFAULT NULL COMMENT '合规校验状态（PASS/FAIL/PENDING_SEAL）' AFTER `allocation_status`;
