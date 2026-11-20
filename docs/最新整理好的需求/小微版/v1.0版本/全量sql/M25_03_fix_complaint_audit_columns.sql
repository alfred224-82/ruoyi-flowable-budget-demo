-- =====================================================================
-- M25_03 complaint_record / compliance_agreement 审计列补全（新增缺失列）
-- 版本：v1.0  日期：2026-10-12
-- 问题：org.springframework.jdbc.BadSqlGrammarException:
--       java.sql.SQLSyntaxErrorException: Unknown column 'create_by' in 'field list'
--       请求地址 '/system/supervision/complaints' 查询投诉列表时报错。
-- 根因：ComplaintRecord / ComplianceAgreement 实体均继承 TenantEntity → BaseEntity，
--       BaseEntity 定义 create_by / create_time / update_by / update_time 四列，
--       MyBatis-Plus 自动生成 SELECT 会带上全部四列。
--       但 M25_01 原始建表脚本只建了部分审计列：
--         · complaint_record    —— 缺 create_by / update_by
--         · compliance_agreement —— 缺 create_by / update_by / update_time
--       导致列缺失 SQL 语法错误。
--       ★与 M22_02（BIGINT→VARCHAR）同类缺陷，此处补修 M25 两张新表遗漏的审计列。
-- 修复：ALTER TABLE 补齐缺失审计列（create_by/update_by 为 VARCHAR(64)，与 BaseEntity 对齐）。
--       ★M25_01 采用 DROP TABLE IF EXISTS 重建，直接重跑会清空存量数据；
--         故本脚本用幂等 ADD COLUMN 原地补列，不删表、不丢数据。
-- 前置依赖：M25_01（complaint_record / compliance_agreement 建表）
-- 规范：ALTER TABLE 使用 INFORMATION_SCHEMA 幂等（仅当列不存在时才 ADD），可重复执行
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 1. complaint_record：补齐 create_by / update_by
-- =====================================================================
DROP PROCEDURE IF EXISTS `_m25_03_alter_complaint_record`;
DELIMITER $$
CREATE PROCEDURE `_m25_03_alter_complaint_record`()
BEGIN
    -- 1.1 create_by（放在 create_time 之前）
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'complaint_record'
          AND COLUMN_NAME  = 'create_by'
    ) THEN
        ALTER TABLE `complaint_record`
            ADD COLUMN `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者' AFTER `handle_time`;
    END IF;

    -- 1.2 update_by（放在 update_time 之前）
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'complaint_record'
          AND COLUMN_NAME  = 'update_by'
    ) THEN
        ALTER TABLE `complaint_record`
            ADD COLUMN `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者' AFTER `create_time`;
    END IF;
END$$
DELIMITER ;
CALL `_m25_03_alter_complaint_record`();
DROP PROCEDURE IF EXISTS `_m25_03_alter_complaint_record`;

-- =====================================================================
-- 2. compliance_agreement：补齐 create_by / update_by / update_time
-- =====================================================================
DROP PROCEDURE IF EXISTS `_m25_03_alter_compliance_agreement`;
DELIMITER $$
CREATE PROCEDURE `_m25_03_alter_compliance_agreement`()
BEGIN
    -- 2.1 create_by（放在 create_time 之前）
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'compliance_agreement'
          AND COLUMN_NAME  = 'create_by'
    ) THEN
        ALTER TABLE `compliance_agreement`
            ADD COLUMN `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者' AFTER `sign_ip`;
    END IF;

    -- 2.2 update_by（放在 create_time 之后）
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'compliance_agreement'
          AND COLUMN_NAME  = 'update_by'
    ) THEN
        ALTER TABLE `compliance_agreement`
            ADD COLUMN `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者' AFTER `create_time`;
    END IF;

    -- 2.3 update_time（放在 update_by 之后）
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'compliance_agreement'
          AND COLUMN_NAME  = 'update_time'
    ) THEN
        ALTER TABLE `compliance_agreement`
            ADD COLUMN `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `update_by`;
    END IF;
END$$
DELIMITER ;
CALL `_m25_03_alter_compliance_agreement`();
DROP PROCEDURE IF EXISTS `_m25_03_alter_compliance_agreement`;

-- =====================================================================
-- 3. 验收查询（执行后人工核对）
-- =====================================================================
-- 3.1 complaint_record 审计列
-- SELECT COLUMN_NAME, COLUMN_TYPE
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'complaint_record'
--    AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time')
--  ORDER BY ORDINAL_POSITION;
-- 预期：4 行（create_by varchar(64) / create_time datetime / update_by varchar(64) / update_time datetime）
--
-- 3.2 compliance_agreement 审计列
-- SELECT COLUMN_NAME, COLUMN_TYPE
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'compliance_agreement'
--    AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time')
--  ORDER BY ORDINAL_POSITION;
-- 预期：4 行（create_by varchar(64) / create_time datetime / update_by varchar(64) / update_time datetime）
