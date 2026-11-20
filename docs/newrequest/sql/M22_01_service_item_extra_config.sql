-- =============================================
-- M22 修复：service_item 表补充 extra_config 列
-- 背景：promotion_activity.selected_project_id 指向 service_item.id，
--       但 ProjectSignupServiceImpl 需要 extra_config 存储业态配置（quota/tableCount 等）。
-- 日期：2026-10-10
-- =============================================

-- ----------------------------
-- 1. service_item 新增 extra_config 列（幂等）
-- ----------------------------
SET @tbl = 'service_item';
SET @col = 'extra_config';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TEXT DEFAULT NULL COMMENT ''业态差异配置JSON（quota/tableCount/perTable等，T11报名用）'' AFTER `description`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. 验证
-- ----------------------------
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_item'
--   AND COLUMN_NAME = 'extra_config';
-- 预期：1 行
