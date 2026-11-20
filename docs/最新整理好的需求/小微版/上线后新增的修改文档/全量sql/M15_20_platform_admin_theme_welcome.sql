-- =====================================================================
-- M15_20 platform_admin 小程序主题选择 + 首页欢迎语（T15.36，v1.30 新增）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档（小微版）》v1.30 T15.36 详细设计
-- 概述：platform_admin 在 PC 端选择小程序全局主题（默认 4 套），选定后写入
--       sys_tenant.app_theme_key，小程序端下次冷启动读取最新主题配置。
--       首页显示「【租户名称】管理系统 欢迎您！【用户名】」欢迎语。
-- 变更内容：
--   1. sys_tenant 补列：app_theme_key varchar(32) default 'theme_default'
--   2. 幂等 ALTER TABLE
-- 前置依赖：M15_06（sys_tenant 表已存在）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 确保 exec_dynsql 存储过程存在
-- ----------------------------
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS `exec_dynsql`(IN p_sql TEXT)
BEGIN
    IF p_sql IS NOT NULL AND p_sql != '' THEN
        SET @dyn_sql = p_sql;
        PREPARE stmt FROM @dyn_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- ----------------------------
-- 2. 补列 app_theme_key（租户全局主题配置）
-- ----------------------------
SET @db_name = DATABASE();
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `sys_tenant` ADD COLUMN `app_theme_key` varchar(32) DEFAULT ''theme_default'' COMMENT ''小程序全局主题key（theme_default/theme_warm/theme_fresh/theme_elegant，platform_admin选定后全局换肤）'' AFTER `version_type`',
    'SELECT ''app_theme_key already exists''');
CALL exec_dynsql(@sql);

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- A. 新列已补
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key';
-- 预期：1 行（app_theme_key varchar(32) default theme_default）
--
-- B. 存量租户默认主题
-- SELECT id, company_name, app_theme_key FROM sys_tenant WHERE tenant_type = 'micro' LIMIT 5;
-- 预期：app_theme_key 均为 theme_default（或 NULL，MySQL DEFAULT 仅对新插入行生效）
