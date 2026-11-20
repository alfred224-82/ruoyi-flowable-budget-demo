-- =====================================================================
-- M5_12 报销单表结构补齐（fee_expense_bill 缺失列幂等补全）
-- 版本：v1.0  日期：2026-09-06
-- 依据：《资金管控系统详细设计文档》V5.4 多币种/补助/合规校验章节、
--       sql/business_tables.sql 报销单表完整口径、ExpenseBill 实体与 ExpenseBillMapper.xml
-- 背景：fee_expense_bill 属既有存量表（M 系列只做增量补列，不建表），
--       基础环境由 script/sql/mysql/fee/fee_tables.sql 建出的表结构缺列，
--       仅执行了 M1_01/M1_03/M5_01 等增量（tenant_id、project_id、evidence_level、org_id、
--       invoice_defer），而 V5.4 增量（currency_code/original_amount/exchange_rate/
--       subsidy_amount/travel_days/compliance_status）、cost_method 增量以及
--       申请人-审批人审计列从未落库，导致 MyBatis-Plus 全量 SELECT 报
--       「Unknown column 'currency_code' in 'field list'」（补完该列会依次再报其余缺列）。
-- 序号：M5_02~M5_11 已被高级版/专业版 M5 系列占用，本脚本取 M5_12 以避免跨版本重名。
-- 前置：M5_01（基础版索引 [22]，高级/专业版阶段一~八同序）先补 project_id/invoice_defer；
--       本脚本 AFTER 子句不依赖其列，两者顺序可互换。
-- 归档：基础版/高级版/专业版三份副本内容一致（累计制），修改需同步。
-- 规范：
--   1. 幂等：补列走存储过程 + INFORMATION_SCHEMA.COLUMNS 条件判断，可重复执行
--   2. 降级保护：只做 ADD COLUMN 与带 WHERE 守卫的回填，不删列、不删数据
--   3. 列定义与 sql/business_tables.sql 保持一致（类型/默认值/AFTER 位置）
-- =====================================================================

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_column_if_missing`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column;
    IF v_cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ----------------------------
-- 1. V5.4 多币种字段（currency_code / original_amount / exchange_rate）
-- ----------------------------
CALL sp_add_column_if_missing('fee_expense_bill', 'currency_code',
  '`currency_code` VARCHAR(10) DEFAULT ''CNY'' COMMENT ''币种编码(CNY/USD/EUR/GBP/JPY)'' AFTER `amount`');
CALL sp_add_column_if_missing('fee_expense_bill', 'original_amount',
  '`original_amount` DECIMAL(18,2) DEFAULT NULL COMMENT ''原币金额（外币时填写）'' AFTER `currency_code`');
CALL sp_add_column_if_missing('fee_expense_bill', 'exchange_rate',
  '`exchange_rate` DECIMAL(10,6) DEFAULT NULL COMMENT ''使用汇率'' AFTER `original_amount`');

-- ----------------------------
-- 2. V5.4 补助字段（subsidy_amount / travel_days）
-- ----------------------------
CALL sp_add_column_if_missing('fee_expense_bill', 'subsidy_amount',
  '`subsidy_amount` DECIMAL(18,2) DEFAULT NULL COMMENT ''补助金额'' AFTER `exchange_rate`');
CALL sp_add_column_if_missing('fee_expense_bill', 'travel_days',
  '`travel_days` INT DEFAULT NULL COMMENT ''出差天数（用于补助核算）'' AFTER `subsidy_amount`');

-- ----------------------------
-- 3. 费用确认方式（cost_method，与 fee_mapping_rule 同名字段口径一致）
-- ----------------------------
CALL sp_add_column_if_missing('fee_expense_bill', 'cost_method',
  '`cost_method` VARCHAR(20) DEFAULT ''DIRECT'' COMMENT ''费用确认方式(DIRECT/DEPRECIATION/ACCRUAL/FEE_RATE)'' AFTER `expenditure_type`');

-- ----------------------------
-- 4. V5.4 合规校验状态（compliance_status）
-- ----------------------------
CALL sp_add_column_if_missing('fee_expense_bill', 'compliance_status',
  '`compliance_status` VARCHAR(20) DEFAULT NULL COMMENT ''合规校验状态(PASS/FAIL/PENDING_SEAL)'' AFTER `allocation_status`');

-- ----------------------------
-- 5. 申请人/审批人审计列（实体与分页 SQL 均引用，缺失会导致报销单列表查询直接报错）
-- ----------------------------
CALL sp_add_column_if_missing('fee_expense_bill', 'applicant_id',
  '`applicant_id` BIGINT DEFAULT NULL COMMENT ''申请人ID'' AFTER `update_time`');
CALL sp_add_column_if_missing('fee_expense_bill', 'applicant_name',
  '`applicant_name` VARCHAR(50) DEFAULT NULL COMMENT ''申请人姓名'' AFTER `applicant_id`');
CALL sp_add_column_if_missing('fee_expense_bill', 'approver_id',
  '`approver_id` BIGINT DEFAULT NULL COMMENT ''审批人ID'' AFTER `applicant_name`');
CALL sp_add_column_if_missing('fee_expense_bill', 'approve_time',
  '`approve_time` DATETIME DEFAULT NULL COMMENT ''审批时间'' AFTER `approver_id`');
CALL sp_add_column_if_missing('fee_expense_bill', 'reject_reason',
  '`reject_reason` TEXT COMMENT ''驳回原因'' AFTER `approve_time`');
CALL sp_add_column_if_missing('fee_expense_bill', 'reject_time',
  '`reject_time` DATETIME DEFAULT NULL COMMENT ''驳回时间'' AFTER `reject_reason`');

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;

-- ----------------------------
-- 6. 存量数据回填（带 WHERE 守卫，天然幂等）
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;
UPDATE `fee_expense_bill` SET `currency_code` = 'CNY' WHERE `currency_code` IS NULL OR `currency_code` = '';
UPDATE `fee_expense_bill` SET `cost_method` = 'DEPRECIATION' WHERE `is_fixed_asset` = 1 AND (`cost_method` IS NULL OR `cost_method` = 'DIRECT');
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 7. 验收查询
-- ----------------------------
-- 7.1 13 列全部就位（预期 13 行）
-- SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill'
--    AND COLUMN_NAME IN ('currency_code','original_amount','exchange_rate','subsidy_amount','travel_days',
--                        'cost_method','compliance_status','applicant_id','applicant_name','approver_id',
--                        'approve_time','reject_reason','reject_time');
--
-- 7.2 存量单据币种已回填（预期 0）
-- SELECT COUNT(*) FROM fee_expense_bill WHERE currency_code IS NULL OR currency_code = '';
--
-- 7.3 页面验收：报销单列表/详情可正常打开，外币单据展示原币金额与汇率，不再报 1054
