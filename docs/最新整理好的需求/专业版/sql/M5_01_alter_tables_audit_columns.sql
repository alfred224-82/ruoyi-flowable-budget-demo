-- =====================================================================
-- M5_01 基础版业务表补充审计字段（M1_02 增量修正）
-- 版本：v1.0  日期：2026-08-29
-- 说明：
--   1. M1_02 中部分轻量表未包含 create_by / update_by / update_time 审计列，
--      与框架 BaseEntity 自动填充（CreateAndUpdateMetaObjectHandler）冲突，
--      本脚本幂等补齐（列已存在则跳过，可重复执行）；
--   2. 对齐《开发与落地规范》第6条"留痕"要求。
-- =====================================================================

DROP PROCEDURE IF EXISTS `m5_add_audit_columns`;
DELIMITER $$
CREATE PROCEDURE `m5_add_audit_columns`(IN tbl_name VARCHAR(64))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl_name AND COLUMN_NAME = 'create_by') THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN `create_by` VARCHAR(64) COMMENT ''创建者''');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl_name AND COLUMN_NAME = 'update_by') THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN `update_by` VARCHAR(64) COMMENT ''更新者''');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl_name AND COLUMN_NAME = 'update_time') THEN
        SET @ddl = CONCAT('ALTER TABLE `', tbl_name, '` ADD COLUMN `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL m5_add_audit_columns('fee_project_subject');
CALL m5_add_audit_columns('fee_project_member');
CALL m5_add_audit_columns('fee_income_confirmation');
CALL m5_add_audit_columns('fee_project_change');
CALL m5_add_audit_columns('fee_bad_debt');
CALL m5_add_audit_columns('fee_project_link_rule');
CALL m5_add_audit_columns('fee_invoice_supplement');
CALL m5_add_audit_columns('fee_staff_salary');
CALL m5_add_audit_columns('fee_salary_record');
CALL m5_add_audit_columns('fee_asset');
CALL m5_add_audit_columns('fee_asset_depreciation');
CALL m5_add_audit_columns('fee_depreciation_rule');
CALL m5_add_audit_columns('fee_project_sharing');
CALL m5_add_audit_columns('fee_loan');
CALL m5_add_audit_columns('fee_loan_interest');
CALL m5_add_audit_columns('fee_export_template');

DROP PROCEDURE IF EXISTS `m5_add_audit_columns`;

-- ----------------------------
-- T5.9 依赖：报销单补充项目关联与发票后补字段（T5.7 / T5.8）
-- fee_expense_bill 为既有表，幂等补列
-- ----------------------------
DROP PROCEDURE IF EXISTS `m5_add_bill_column`;
DELIMITER $$
CREATE PROCEDURE `m5_add_bill_column`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill' AND COLUMN_NAME = 'project_id') THEN
        ALTER TABLE `fee_expense_bill`
            ADD COLUMN `project_id` BIGINT DEFAULT NULL COMMENT '关联项目ID(0=公司级/未归类)' AFTER `project_code`,
            ADD KEY `idx_project_id` (`project_id`);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'fee_expense_bill' AND COLUMN_NAME = 'invoice_defer') THEN
        ALTER TABLE `fee_expense_bill`
            ADD COLUMN `invoice_defer` TINYINT DEFAULT 0 COMMENT '是否发票后补(0否 1是)';
    END IF;
END$$
DELIMITER ;

CALL m5_add_bill_column();
DROP PROCEDURE IF EXISTS `m5_add_bill_column`;
