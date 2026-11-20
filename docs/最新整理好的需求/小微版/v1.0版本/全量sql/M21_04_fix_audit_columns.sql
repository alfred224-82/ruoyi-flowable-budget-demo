-- =====================================================================
-- M21_04  service_item / resource_status_log 补全 BaseEntity 审计字段
-- 版本：v1.0  日期：2026-10-07
-- 问题：java.sql.SQLSyntaxErrorException: Unknown column 'update_by' in 'field list'
-- 根因：两张表继承了 TenantEntity→BaseEntity，MyBatis-Plus 自动 SELECT
--       create_by/create_time/update_by/update_time 四个审计字段，
--       但建表时只创建了 create_by/create_time，缺少 update_by/update_time。
--       另外 resource_status_log.create_by 类型为 BIGINT，与 BaseEntity.createBy
--       的 String 类型不匹配，需修正为 VARCHAR(64)。
-- 前置依赖：M15_04（service_item 建表）+ M21_02（resource_status_log 建表）
-- 规范：ALTER TABLE 使用 INFORMATION_SCHEMA 幂等（列已存在则跳过）
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 1. service_item 补 update_by / update_time
-- =====================================================================
DROP PROCEDURE IF EXISTS `_m21_04_alter_service_item`;
DELIMITER $$
CREATE PROCEDURE `_m21_04_alter_service_item`()
BEGIN
    -- 1.1 补 update_by
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'service_item'
          AND COLUMN_NAME  = 'update_by'
    ) THEN
        ALTER TABLE `service_item`
            ADD COLUMN `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者';
    END IF;

    -- 1.2 补 update_time
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'service_item'
          AND COLUMN_NAME  = 'update_time'
    ) THEN
        ALTER TABLE `service_item`
            ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间';
    END IF;

    -- 1.3 兜底：确保 create_by 类型为 VARCHAR(64)（与 BaseEntity.createBy String 对齐）
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'service_item'
          AND COLUMN_NAME  = 'create_by'
          AND DATA_TYPE    != 'varchar'
    ) THEN
        ALTER TABLE `service_item`
            MODIFY COLUMN `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者';
    END IF;
END$$
DELIMITER ;
CALL `_m21_04_alter_service_item`();
DROP PROCEDURE IF EXISTS `_m21_04_alter_service_item`;

-- =====================================================================
-- 2. resource_status_log 补 update_by / update_time + 修复 create_by 类型
-- =====================================================================
DROP PROCEDURE IF EXISTS `_m21_04_alter_resource_status_log`;
DELIMITER $$
CREATE PROCEDURE `_m21_04_alter_resource_status_log`()
BEGIN
    -- 2.1 修复 create_by 类型：BIGINT → VARCHAR(64)
    --     BaseEntity.createBy 为 String，MetaObjectHandler 填充用户名
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'resource_status_log'
          AND COLUMN_NAME  = 'create_by'
          AND DATA_TYPE    != 'varchar'
    ) THEN
        ALTER TABLE `resource_status_log`
            MODIFY COLUMN `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者';
    END IF;

    -- 2.2 补 update_by
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'resource_status_log'
          AND COLUMN_NAME  = 'update_by'
    ) THEN
        ALTER TABLE `resource_status_log`
            ADD COLUMN `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者';
    END IF;

    -- 2.3 补 update_time
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'resource_status_log'
          AND COLUMN_NAME  = 'update_time'
    ) THEN
        ALTER TABLE `resource_status_log`
            ADD COLUMN `update_time` DATETIME DEFAULT NULL COMMENT '更新时间';
    END IF;
END$$
DELIMITER ;
CALL `_m21_04_alter_resource_status_log`();
DROP PROCEDURE IF EXISTS `_m21_04_alter_resource_status_log`;

-- =====================================================================
-- 3. 验收查询（执行后人工核对）
-- =====================================================================
-- 3.1 service_item 审计字段完整性
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_item'
--    AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time')
--  ORDER BY ORDINAL_POSITION;
-- 预期：4 行，create_by=varchar(64), update_by=varchar(64)
--
-- 3.2 resource_status_log 审计字段完整性
-- SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resource_status_log'
--    AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time')
--  ORDER BY ORDINAL_POSITION;
-- 预期：4 行，create_by=varchar(64), update_by=varchar(64)
