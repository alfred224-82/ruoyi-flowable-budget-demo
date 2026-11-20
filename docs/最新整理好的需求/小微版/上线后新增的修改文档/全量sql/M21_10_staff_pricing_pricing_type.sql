-- =====================================================================
-- M21_10  资源价格设置升级：staff_pricing 补 pricing_type + extra_config 两列
-- =====================================================================
-- 需求概述：
--   人员定价支持 4 种计价模式（duration 计时 / package 套餐 / times 计量 / fixed 一口价），
--   extra_config 存套餐/一口价的含时长、超时费等扩展配置 JSON。
--
-- 根因：
--   Java 实体 com.ruoyi.system.domain.StaffPricing 已声明 pricingType / extraConfig 两字段，
--   StaffPricingServiceImpl.listAll() 查询会 SELECT 这两列，但建表脚本 [106]M21_03
--   （staff_pricing）未包含这两列 → 生产运行时报
--   Unknown column 'pricing_type' in 'field list'。
--   ★M21_03 建表脚本保持不变（历史归档），本脚本以幂等 ALTER 补齐缺失列。
--
-- 前置：
--   [106]M21_03（staff_pricing 建表）
--
-- 幂等：条件 ALTER（INFORMATION_SCHEMA 守卫 + PREPARE），可重复执行
-- ★新增列带 DEFAULT，存量行自动回填 pricing_type='duration'，无需 UPDATE
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. staff_pricing 补 pricing_type 列（计价模式，默认 duration 计时）
-- =====================================================================
SET @tbl = 'staff_pricing';
SET @col = 'pricing_type';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` VARCHAR(20) NOT NULL DEFAULT ''duration'' COMMENT ''计价模式（duration计时/package套餐/times计量/fixed一口价）'' AFTER `hourly_rate`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. staff_pricing 补 extra_config 列（扩展配置 JSON）
-- =====================================================================
SET @col = 'extra_config';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TEXT DEFAULT NULL COMMENT ''扩展配置JSON（套餐/一口价的含时长、超时费等）'' AFTER `pricing_type`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 3. 验收查询（执行后人工核对）
-- =====================================================================
-- 3.1 staff_pricing 新增两列就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_pricing'
--   AND COLUMN_NAME IN ('pricing_type','extra_config');
-- 预期：2 行，pricing_type DEFAULT='duration'
