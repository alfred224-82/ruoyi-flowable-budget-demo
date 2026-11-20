-- =====================================================================
-- T14.2 只读宽限期支持：sys_tenant 新增 grace_period_until 列
-- 幂等脚本：列已存在时跳过
-- =====================================================================

-- 1. 新增 grace_period_until 列（只读宽限期截止日）
SET @db_name = DATABASE();
SET @col_name = 'grace_period_until';
SET @table_name = 'sys_tenant';

SELECT COUNT(*) INTO @col_exists
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db_name
  AND TABLE_NAME = @table_name
  AND COLUMN_NAME = @col_name;

SET @sql = IF(@col_exists = 0,
    CONCAT('ALTER TABLE `', @table_name, '` ADD COLUMN `', @col_name,
           '` datetime DEFAULT NULL COMMENT ''只读宽限期截止日（T14.2：到期后3天内允许只读访问）'' AFTER `status`'),
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 将存量已到期(status=EXPIRED)的租户保持原状（不自动回退为 GRACE）
--    仅新到期的租户才进入 GRACE 宽限期流程
--    如需将存量 EXPIRED 租户也进入宽限期，可手动执行：
--    UPDATE sys_tenant SET status='GRACE', grace_period_until=DATE_ADD(expire_date, INTERVAL 3 DAY)
--    WHERE status='EXPIRED' AND expire_date IS NOT NULL AND expire_date >= DATE_SUB(NOW(), INTERVAL 3 DAY);

SELECT 'T14.2 grace_period_until 列迁移完成' AS result;
