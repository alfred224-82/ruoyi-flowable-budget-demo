-- 报销单表新增报销年月字段（对应预算编制期间）
-- 执行时间：2026-07-19

ALTER TABLE fee_expense_bill
ADD COLUMN expense_period VARCHAR(7) DEFAULT NULL COMMENT '报销年月（格式：YYYY-MM，对应预算编制期间）' AFTER expense_date;

-- 为已有数据回填默认值（使用费用日期的年月）
UPDATE fee_expense_bill
SET expense_period = DATE_FORMAT(expense_date, '%Y-%m')
WHERE expense_period IS NULL AND expense_date IS NOT NULL;
