-- =====================================================================
-- M1_01 存量业务表 tenant_id 幂等补齐脚本（T1.4 多租户隔离检查）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统设计文档》1.2 全局多租户硬约束 / ADR-007
-- 说明：
--   1. 本脚本替代 docs/sql/phase18_multi_tenant.sql（原脚本为裸 ALTER，重复执行报
--      Duplicate column，见 docs/newrequest/bug/BUG-002）
--   2. 基于 information_schema 条件判断，可安全重复执行（幂等）
--   3. 覆盖 Dump20260723 快照中全部无 tenant_id 的 fee_*/budget_* 存量表
--   4. 新增表（M1_02/M1_03 脚本）建表时已直接包含 tenant_id，不在此列
-- =====================================================================

DROP PROCEDURE IF EXISTS `sp_add_tenant_id`;
DELIMITER $$
CREATE PROCEDURE `sp_add_tenant_id`(IN p_table VARCHAR(64))
BEGIN
    DECLARE v_col_cnt INT DEFAULT 0;
    DECLARE v_idx_cnt INT DEFAULT 0;

    -- 1) 缺 tenant_id 列则补列（默认租户 1）
    SELECT COUNT(*) INTO v_col_cnt FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = 'tenant_id';
    IF v_col_cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT ''租户ID''');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;

    -- 2) 缺租户索引则补索引
    SELECT COUNT(*) INTO v_idx_cnt FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = 'idx_tenant_id';
    IF v_idx_cnt = 0 THEN
        SET @ddl = CONCAT('CREATE INDEX `idx_tenant_id` ON `', p_table, '` (`tenant_id`)');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ----------------------------
-- 费用模块存量表（13 张）
-- ----------------------------
CALL sp_add_tenant_id('fee_expense_bill');
CALL sp_add_tenant_id('fee_purpose_tag');
CALL sp_add_tenant_id('fee_mapping_rule');
CALL sp_add_tenant_id('fee_over_budget_approval');
CALL sp_add_tenant_id('fee_budget_adjustment_log');
CALL sp_add_tenant_id('fee_budget_adjustment_app');
CALL sp_add_tenant_id('fee_cost_allocation');
CALL sp_add_tenant_id('fee_cost_allocation_rule');
CALL sp_add_tenant_id('fee_reserved_budget');
CALL sp_add_tenant_id('fee_reserved_allocation_log');
CALL sp_add_tenant_id('fee_operation_log');
CALL sp_add_tenant_id('fee_cost_center');
CALL sp_add_tenant_id('fee_cost_center_dept');

-- ----------------------------
-- 预算模块存量表（11 张）
-- ----------------------------
CALL sp_add_tenant_id('budget_preparation');
CALL sp_add_tenant_id('budget_preparation_detail');
CALL sp_add_tenant_id('budget_subject');
CALL sp_add_tenant_id('budget_detail');
CALL sp_add_tenant_id('budget_sheet');
CALL sp_add_tenant_id('budget_reject_history');
CALL sp_add_tenant_id('budget_reject_reason');
CALL sp_add_tenant_id('budget_validation_rule');
CALL sp_add_tenant_id('budget_risk_rule');
CALL sp_add_tenant_id('budget_email_log');
CALL sp_add_tenant_id('budget_operation_log');

-- ----------------------------
-- Phase16~17 已建但快照早于迁移的表（如已执行过 phase18 脚本则自动跳过）
-- ----------------------------
CALL sp_add_tenant_id('fee_subsidy_standard');
CALL sp_add_tenant_id('fee_contract_ledger');
CALL sp_add_tenant_id('fee_fund_pool_config');
CALL sp_add_tenant_id('fee_workflow_config');
CALL sp_add_tenant_id('fee_workflow_node_config');
CALL sp_add_tenant_id('fee_welfare_config');
CALL sp_add_tenant_id('fee_accrual_record');
CALL sp_add_tenant_id('fee_ai_prompt_config');
CALL sp_add_tenant_id('fee_asset_card');
CALL sp_add_tenant_id('fee_budget_track_config');
CALL sp_add_tenant_id('fee_compliance_config');
CALL sp_add_tenant_id('fee_compliance_record');
CALL sp_add_tenant_id('fee_cross_dept_allocation_log');
CALL sp_add_tenant_id('fee_desensitize_rule');
CALL sp_add_tenant_id('fee_exchange_rate');
CALL sp_add_tenant_id('fee_industry_config');
CALL sp_add_tenant_id('fee_loan_ledger');
CALL sp_add_tenant_id('fee_profit_tip_config');

-- ----------------------------
-- 用户表（用户归属租户，NULL=平台级账号）
-- ----------------------------
CALL sp_add_tenant_id('sys_user');

DROP PROCEDURE IF EXISTS `sp_add_tenant_id`;

-- ----------------------------
-- 校验（执行后可人工核对，全部表应返回 1 或 0 行缺失记录）
-- ----------------------------
-- SELECT TABLE_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME IN ('fee_expense_bill','fee_purpose_tag','fee_mapping_rule',
--        'fee_over_budget_approval','fee_budget_adjustment_log','fee_budget_adjustment_app',
--        'fee_cost_allocation','fee_cost_allocation_rule','fee_reserved_budget',
--        'fee_reserved_allocation_log','fee_operation_log','fee_cost_center','fee_cost_center_dept',
--        'budget_preparation','budget_preparation_detail','budget_subject','budget_detail',
--        'budget_sheet','budget_reject_history','budget_reject_reason','budget_validation_rule',
--        'budget_risk_rule','budget_email_log','budget_operation_log','sys_user')
--    AND COLUMN_NAME = 'tenant_id';
