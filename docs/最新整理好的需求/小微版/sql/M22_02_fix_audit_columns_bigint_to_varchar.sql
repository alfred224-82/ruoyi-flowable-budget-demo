-- =====================================================================
-- M22_02  staff_pricing / staff_schedule 审计字段类型修复（BIGINT → VARCHAR(64)）
-- 版本：v1.0  日期：2026-10-10
-- 问题：java.sql.SQLException: Incorrect integer value: 'lijie' for column 'create_by' at row 1
--       请求地址 '/micro/resourcePrice/staff/save' 保存助教价格时报错。
-- 根因：staff_pricing（M21_03）与 staff_schedule（M22_01）两张表的实体均继承
--       TenantEntity → BaseEntity，BaseEntity.createBy/updateBy 为 String 类型，
--       CreateAndUpdateMetaObjectHandler.insertFill 填充【登录用户名】（如 'lijie'）。
--       但建表脚本把 create_by/update_by 定义成了 BIGINT（整数），
--       写入用户名字符串时触发 "Incorrect integer value" 报错。
--       ★与 M21_04 修复 service_item/resource_status_log 属同一类缺陷，此处补修遗漏的两张表。
-- 修复：将 create_by/update_by 由 BIGINT 修正为 VARCHAR(64)，与 BaseEntity 对齐。
-- 前置依赖：M21_03（staff_pricing 建表）+ M22_01（staff_schedule 建表）
-- 规范：ALTER TABLE 使用 INFORMATION_SCHEMA 幂等（仅当类型非 varchar 时才 MODIFY）
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 1. staff_pricing：create_by / update_by 类型兜底修复（BIGINT → VARCHAR(64)）
-- =====================================================================
DROP PROCEDURE IF EXISTS `_m22_02_alter_staff_pricing`;
DELIMITER $$
CREATE PROCEDURE `_m22_02_alter_staff_pricing`()
BEGIN
    -- 1.1 修复 create_by 类型：BIGINT → VARCHAR(64)
    --     BaseEntity.createBy 为 String，MetaObjectHandler 填充用户名
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'staff_pricing'
          AND COLUMN_NAME  = 'create_by'
          AND DATA_TYPE    != 'varchar'
    ) THEN
        ALTER TABLE `staff_pricing`
            MODIFY COLUMN `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者';
    END IF;

    -- 1.2 修复 update_by 类型：BIGINT → VARCHAR(64)
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'staff_pricing'
          AND COLUMN_NAME  = 'update_by'
          AND DATA_TYPE    != 'varchar'
    ) THEN
        ALTER TABLE `staff_pricing`
            MODIFY COLUMN `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者';
    END IF;
END$$
DELIMITER ;
CALL `_m22_02_alter_staff_pricing`();
DROP PROCEDURE IF EXISTS `_m22_02_alter_staff_pricing`;

-- =====================================================================
-- 2. staff_schedule：create_by / update_by 类型兜底修复（BIGINT → VARCHAR(64)）
-- =====================================================================
DROP PROCEDURE IF EXISTS `_m22_02_alter_staff_schedule`;
DELIMITER $$
CREATE PROCEDURE `_m22_02_alter_staff_schedule`()
BEGIN
    -- 2.1 修复 create_by 类型：BIGINT → VARCHAR(64)
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'staff_schedule'
          AND COLUMN_NAME  = 'create_by'
          AND DATA_TYPE    != 'varchar'
    ) THEN
        ALTER TABLE `staff_schedule`
            MODIFY COLUMN `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者';
    END IF;

    -- 2.2 修复 update_by 类型：BIGINT → VARCHAR(64)
    IF EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'staff_schedule'
          AND COLUMN_NAME  = 'update_by'
          AND DATA_TYPE    != 'varchar'
    ) THEN
        ALTER TABLE `staff_schedule`
            MODIFY COLUMN `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者';
    END IF;
END$$
DELIMITER ;
CALL `_m22_02_alter_staff_schedule`();
DROP PROCEDURE IF EXISTS `_m22_02_alter_staff_schedule`;

-- =====================================================================
-- 3. 验收查询（执行后人工核对）
-- =====================================================================
-- 3.1 staff_pricing 审计字段类型
-- SELECT COLUMN_NAME, COLUMN_TYPE
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_pricing'
--    AND COLUMN_NAME IN ('create_by','update_by')
--  ORDER BY ORDINAL_POSITION;
-- 预期：2 行，均为 varchar(64)
--
-- 3.2 staff_schedule 审计字段类型
-- SELECT COLUMN_NAME, COLUMN_TYPE
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_schedule'
--    AND COLUMN_NAME IN ('create_by','update_by')
--  ORDER BY ORDINAL_POSITION;
-- 预期：2 行，均为 varchar(64)
