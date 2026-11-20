-- =====================================================================
-- M15_19 活动图片上传 + 背景主题扩展（T15.35，v1.30 新增）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档（小微版）》v1.30 T15.35 详细设计
-- 概述：boss 发布/编辑活动时，新增活动 logo、活动海报、背景主题三类字段。
--       poster_url 已存在（M15_28），仅需补 logo_url 和 theme_key 两列。
-- 变更内容：
--   1. promotion_activity 补列：logo_url varchar(500)、theme_key varchar(32) default 'theme_default'
--   2. 幂等 ALTER TABLE（INFORMATION_SCHEMA 条件判断）
-- 前置依赖：M15_11（promotion_activity 表已建立）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 0. 存储过程：动态 SQL 执行器（幂等辅助，若不存在则创建）
-- ----------------------------
-- ★exec_dynsql 须在补列逻辑之前就绪，本脚本自建而非依赖外部脚本
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
-- 1. 补列 logo_url（活动 logo，方形标识图，列表卡片左上角）
-- ----------------------------
SET @db_name = DATABASE();
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'promotion_activity' AND COLUMN_NAME = 'logo_url');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `promotion_activity` ADD COLUMN `logo_url` varchar(500) DEFAULT NULL COMMENT ''活动logo（方形标识图，列表卡片左上角图标）'' AFTER `poster_url`',
    'SELECT ''logo_url already exists''');
CALL exec_dynsql(@sql);

-- ----------------------------
-- 2. 补列 theme_key（背景主题，关联全局 4 套主题色板）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = @db_name AND TABLE_NAME = 'promotion_activity' AND COLUMN_NAME = 'theme_key');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `promotion_activity` ADD COLUMN `theme_key` varchar(32) DEFAULT ''theme_default'' COMMENT ''背景主题key（theme_default/theme_warm/theme_fresh/theme_elegant，与T15.36全局主题共用色板）'' AFTER `logo_url`',
    'SELECT ''theme_key already exists''');
CALL exec_dynsql(@sql);

-- ----------------------------
-- 4. 验收查询
-- ----------------------------
-- A. 新列已补
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--    AND COLUMN_NAME IN ('logo_url', 'theme_key');
-- 预期：2 行（logo_url varchar(500), theme_key varchar(32) default theme_default）
--
-- B. poster_url 仍存在（未被误删）
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity' AND COLUMN_NAME = 'poster_url';
-- 预期：1
