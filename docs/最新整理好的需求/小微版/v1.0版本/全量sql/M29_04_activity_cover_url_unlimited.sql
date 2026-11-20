-- =====================================================================
-- M29_04  活动海报 cover_url 解除张数上限（PC 端老板创建活动不再限 6 张）
-- =====================================================================
-- 需求概述：
--   老板 PC「推广发布」创建活动时，海报上传由「最多 6 张」放开为「不限张数」。
--   前端 image-upload :limit 由 6 调整为 999（等同不限），文案同步更新。
--   数据库端 promotion_activity.cover_url 原为 VARCHAR(2000)（[132]M26_07 扩容），
--   在 6 张场景刚好够用；不限张数后（例如 20 张 URL × 约 300 字符 ≈ 6000 字符）
--   会触发 Data too long，需扩为 TEXT（最大 65535 字节，足够承载数百张 URL 拼接）。
--
-- 前置：
--   [26]M15_11（promotion_activity 表）
--   [132]M26_07（cover_url VARCHAR(500)→VARCHAR(2000)）
--
-- 幂等：INFORMATION_SCHEMA 守卫（DATA_TYPE <> 'text' 才 MODIFY）
-- ★无菜单/权限/开关变更，执行后无需刷新权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. promotion_activity.cover_url 由 VARCHAR(2000) 扩为 TEXT
--    （不限制张数场景：任意张数 URL 逗号拼接均能落库）
-- =====================================================================
SET @tbl = 'promotion_activity';
SET @col = 'cover_url';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col
       AND DATA_TYPE <> 'text') = 1,
    CONCAT('ALTER TABLE `', @tbl, '` MODIFY COLUMN `', @col,
           '` TEXT DEFAULT NULL COMMENT ''封面图URL（活动海报多张，逗号分隔，不限张数）'''),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. 验收查询（执行后人工核对）
-- =====================================================================

-- 2.1 cover_url 已扩为 TEXT 且 COMMENT 含「不限张数」语义
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--    AND COLUMN_NAME = 'cover_url';
-- 预期：1 行，DATA_TYPE=text，COLUMN_TYPE=text，COMMENT 含 '不限张数'
