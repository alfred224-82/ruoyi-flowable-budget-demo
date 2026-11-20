-- =====================================================================
-- M20_06 固定支出表修复与增强（v1.64，2026-09-30）
-- 版本：v1.0  日期：2026-09-30
-- 概述：
--   1. 修复 monthly_fixed_cost.create_by/update_by 类型不匹配（bigint→varchar(64)）
--      BaseEntity.createBy 为 String 类型，MetaObjectHandler 填充用户名，
--      原 bigint 列导致 "Incorrect integer value: 'lijie' for column 'create_by'"
--   2. 新增 period_rule_id 列，关联租户管理员配置的周期规则（period_rule.id）
--      boss 选择标签后自动带出周期规则，按周期分摊金额
-- 前置依赖：M15_21（monthly_fixed_cost 建表）+ M18_02（period_rule 建表）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 修复 create_by/update_by 类型：bigint → varchar(64)
--    对齐 BaseEntity.createBy/updateBy 的 String 类型
-- ----------------------------
-- ★幂等：INFORMATION_SCHEMA 条件判断，列类型已正确则跳过
SET @col_type_create_by = (
    SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost' AND COLUMN_NAME = 'create_by'
);
SET @col_type_update_by = (
    SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost' AND COLUMN_NAME = 'update_by'
);

SET @sql_fix_create_by = IF(
    @col_type_create_by IS NOT NULL AND @col_type_create_by != 'varchar(64)',
    'ALTER TABLE `monthly_fixed_cost` MODIFY COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建者''',
    'SELECT 1 AS skip_fix_create_by'
);
SET @sql_fix_update_by = IF(
    @col_type_update_by IS NOT NULL AND @col_type_update_by != 'varchar(64)',
    'ALTER TABLE `monthly_fixed_cost` MODIFY COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新者''',
    'SELECT 1 AS skip_fix_update_by'
);

PREPARE stmt_fix_create_by FROM @sql_fix_create_by;
EXECUTE stmt_fix_create_by;
DEALLOCATE PREPARE stmt_fix_create_by;

PREPARE stmt_fix_update_by FROM @sql_fix_update_by;
EXECUTE stmt_fix_update_by;
DEALLOCATE PREPARE stmt_fix_update_by;

-- ----------------------------
-- 2. 新增 period_rule_id 列（关联周期规则，可选）
-- ----------------------------
-- ★幂等：INFORMATION_SCHEMA 守卫，列已存在则跳过
SET @col_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost' AND COLUMN_NAME = 'period_rule_id'
);

SET @sql_add_period_rule_id = IF(
    @col_exists = 0,
    'ALTER TABLE `monthly_fixed_cost` ADD COLUMN `period_rule_id` bigint DEFAULT NULL COMMENT ''关联周期规则ID（period_rule.id，可选）'' AFTER `is_recurring`',
    'SELECT 1 AS skip_add_period_rule_id'
);

PREPARE stmt_add_col FROM @sql_add_period_rule_id;
EXECUTE stmt_add_col;
DEALLOCATE PREPARE stmt_add_col;

-- ----------------------------
-- 3. 新增索引
-- ----------------------------
SET @idx_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost' AND INDEX_NAME = 'idx_period_rule'
);

SET @sql_add_idx = IF(
    @idx_exists = 0,
    'ALTER TABLE `monthly_fixed_cost` ADD KEY `idx_period_rule` (`period_rule_id`)',
    'SELECT 1 AS skip_add_idx'
);

PREPARE stmt_add_idx FROM @sql_add_idx;
EXECUTE stmt_add_idx;
DEALLOCATE PREPARE stmt_add_idx;

-- ----------------------------
-- 4. 验收查询
-- ----------------------------
-- A. create_by/update_by 类型已修正
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost'
--    AND COLUMN_NAME IN ('create_by', 'update_by');
-- 预期：create_by=varchar(64), update_by=varchar(64)
--
-- B. period_rule_id 列已添加
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost' AND COLUMN_NAME = 'period_rule_id';
-- 预期：1 行，bigint, 关联周期规则ID
--
-- C. 索引已创建
-- SELECT INDEX_NAME FROM INFORMATION_SCHEMA.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'monthly_fixed_cost' AND INDEX_NAME = 'idx_period_rule';
-- 预期：1 行
