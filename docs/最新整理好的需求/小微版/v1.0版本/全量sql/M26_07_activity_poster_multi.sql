-- =====================================================================
-- M26_07  活动海报多图扩列（V3.9 §一.3：活动级 1~6 张，横滑）
-- =====================================================================
-- 需求概述：
--   V3.9 §一.3 要求活动海报支持 1~6 张（小程序详情横滑展示）。
--   PC 推广发布表单「活动图片」image-upload limit 1→6，cover_url 存逗号分隔多值：
--     ① 新上传：逗号分隔 ossId 串（6×19+5≈119 字符）；
--     ② 编辑回显后再保存：逗号分隔 URL 串（后端 resolveOssUrls 已翻译，
--        ImageUpload 对 URL 直填项不再回传 ossId），6 条 URL 拼接可超 500 字符。
--   原列宽 VARCHAR(500) 在 ② 场景可能触发 Data too long，扩为 VARCHAR(2000)。
--
-- 前置：
--   [26]M15_11（promotion_activity 表）
--
-- 幂等：INFORMATION_SCHEMA 守卫（CHARACTER_MAXIMUM_LENGTH < 2000 才 MODIFY）
-- ★无菜单/权限/开关变更，执行后无需刷新权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. promotion_activity.cover_url 扩列 VARCHAR(500) → VARCHAR(2000)
--    （6 张 URL × 约 300 字符上限 + 逗号分隔；COMMENT 同步多图语义）
-- =====================================================================
SET @tbl = 'promotion_activity';
SET @col = 'cover_url';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col
       AND CHARACTER_MAXIMUM_LENGTH < 2000) = 1,
    CONCAT('ALTER TABLE `', @tbl, '` MODIFY COLUMN `', @col,
           '` VARCHAR(2000) DEFAULT NULL COMMENT ''封面图URL（活动海报1~6张，逗号分隔）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. 验收查询（执行后人工核对）
-- =====================================================================

-- 2.1 cover_url 列宽已扩至 2000 且 COMMENT 含多图语义
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--    AND COLUMN_NAME = 'cover_url';
-- 预期：1 行，varchar(2000)，COMMENT 含 '1~6 张'
