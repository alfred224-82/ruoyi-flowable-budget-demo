-- =====================================================================
-- M5_10 预算编制年月+部门维度升级脚本（T5.13/T5.14/T6.1）
-- 幂等脚本，可重复执行
-- 执行前提：M1_03_create_tables_advanced_pro.sql 已执行（department_id 列已存在）
-- =====================================================================

-- 1. 幂等补列：budget_preparation.department_id（若 M1_03 未执行则补）
DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_column_if_missing`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column;
    IF v_cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL sp_add_column_if_missing('budget_preparation', 'department_id',
  '`department_id` BIGINT DEFAULT NULL COMMENT ''部门ID(年月+部门维度，T5.13/T5.14)''');
CALL sp_add_column_if_missing('budget_preparation_detail', 'department_id',
  '`department_id` BIGINT DEFAULT NULL COMMENT ''部门ID(年月+部门维度，T5.13/T5.14)''');

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;

-- 2. 补联合索引：按部门+年度+月份查询（编制单定位、使用率统计）
DROP PROCEDURE IF EXISTS `sp_add_index_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_index_if_missing`(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index;
    IF v_cnt = 0 THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` ', p_ddl);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 编制单按部门+年月定位索引
CALL sp_add_index_if_missing('budget_preparation', 'idx_bp_dept_year_month',
  '(`department_id`, `budget_year`, `budget_month`)');

-- 明细表按部门索引（报销按部门扣额度查询）
CALL sp_add_index_if_missing('budget_preparation_detail', 'idx_bpd_dept_id',
  '(`department_id`)');

DROP PROCEDURE IF EXISTS `sp_add_index_if_missing`;

-- 3. 存量数据回填：将已有编制单的 department_id 设为 org_id（向后兼容）
UPDATE budget_preparation
   SET department_id = org_id
 WHERE department_id IS NULL
   AND org_id IS NOT NULL;

-- 4. 存量明细回填：将已有明细的 department_id 设为对应编制单的 department_id
UPDATE budget_preparation_detail d
  INNER JOIN budget_preparation p ON d.sheet_id = p.id
   SET d.department_id = p.department_id
 WHERE d.department_id IS NULL
   AND p.department_id IS NOT NULL;

-- 5. 验证
SELECT 'M5_10 执行完成' AS info,
       (SELECT COUNT(*) FROM budget_preparation WHERE department_id IS NOT NULL) AS filled_preparation_count,
       (SELECT COUNT(*) FROM budget_preparation_detail WHERE department_id IS NOT NULL) AS filled_detail_count;
