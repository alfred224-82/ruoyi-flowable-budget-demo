-- =====================================================================
-- M15_20_fix sys_tenant 补 app_theme_key 列（修复 /portal/stores 500）
-- 版本：v1.1  日期：2026-10-11
-- 问题：SysTenant 实体定义了 appThemeKey 字段，MyBatis-Plus selectList
--       生成 SELECT * 时因数据库缺少 app_theme_key 列导致 500 错误。
--       v1.0 使用 SET @sql + CALL exec_dynsql 方式，部分客户端执行失败。
--       v1.1 改用存储过程 IF NOT EXISTS 模式（与 M15_10 一致）。
-- 方案：幂等补列，列已存在则跳过。
-- 前置依赖：sys_tenant 表已存在
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 幂等补列 app_theme_key（存储过程模式，与 M15_10 一致）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_20_add_app_theme_key`;
DELIMITER $$
CREATE PROCEDURE `m15_20_add_app_theme_key`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `app_theme_key` VARCHAR(32) DEFAULT 'theme_default'
                COMMENT '小程序全局主题key（theme_default/theme_warm/theme_fresh/theme_elegant，platform_admin选定后全局换肤）'
                AFTER `version_type`;
    END IF;
END$$
DELIMITER ;
CALL m15_20_add_app_theme_key();
DROP PROCEDURE IF EXISTS `m15_20_add_app_theme_key`;

-- ----------------------------
-- 2. 验收查询
-- ----------------------------
-- A. 确认列已存在
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key';
-- 预期：1 行（app_theme_key varchar(32) default theme_default）
--
-- B. 验证 /portal/stores 接口恢复正常
-- SELECT id, company_name, city, app_theme_key FROM sys_tenant WHERE status = 'NORMAL' LIMIT 5;
