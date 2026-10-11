-- =====================================================================
-- M2_04 组织管理数据租户隔离改造（T2.4）
-- 版本：v1.1  日期：2026-09-06
-- 依据：《各角色菜单权限文档》（v2.3）§2.5.2 / §3.5
-- 说明：
--   1. sys_dept 新增 tenant_id 列，启用部门租户隔离
--   2. sys_role 新增 tenant_id 列（预留，暂不启用拦截器隔离，共享角色 9201~9205 需跨租户可见）
--   3. 存量数据 tenant_id = 0（系统级，admin 超管可见）
--   4. 租户创建时自动初始化默认部门树（绑定 tenant_id）
--   5. v1.1 幂等化：改为基于 INFORMATION_SCHEMA 条件判断补列/补索引，可安全重复执行
--      （原裸 ALTER 重复执行报 1060 Duplicate column / 1061 Duplicate key，同 BUG-002，
--       对齐 M1_01_existing_tables_add_tenant_id.sql 幂等规范）
-- =====================================================================

-- ----------------------------
-- 1. sys_dept / sys_role 幂等补列 + 补索引（tenant_id）
--    缺列则补列（DEFAULT NULL，值由第 2 节数据迁移赋定）；缺索引则补索引
-- ----------------------------
DROP PROCEDURE IF EXISTS `m2_add_tenant_id_col`;
DELIMITER $$
CREATE PROCEDURE `m2_add_tenant_id_col`(IN p_table VARCHAR(64), IN p_after VARCHAR(64), IN p_comment VARCHAR(128), IN p_index VARCHAR(64))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = 'tenant_id') THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT ''', p_comment, ''' AFTER `', p_after, '`');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND INDEX_NAME = p_index) THEN
        SET @ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (`tenant_id`)');
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- sys_dept：tenant_id AFTER dept_id，索引 idx_dept_tenant_id
CALL m2_add_tenant_id_col('sys_dept', 'dept_id', '租户ID', 'idx_dept_tenant_id');
-- sys_role：预留字段，NULL=全局共享角色，AFTER role_id，索引 idx_role_tenant_id
CALL m2_add_tenant_id_col('sys_role', 'role_id', '租户ID(NULL=全局共享)', 'idx_role_tenant_id');

DROP PROCEDURE IF EXISTS `m2_add_tenant_id_col`;

-- ----------------------------
-- 2. 存量数据标记为系统级（tenant_id=0）
-- ----------------------------
UPDATE `sys_dept` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL;
UPDATE `sys_role` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL AND `role_id` NOT IN (9201, 9202, 9203, 9204, 9205);
-- 共享业务角色保持 NULL（跨租户可见）
UPDATE `sys_role` SET `tenant_id` = NULL WHERE `role_id` IN (9201, 9202, 9203, 9204, 9205);

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- SELECT tenant_id, COUNT(*) AS cnt FROM sys_dept GROUP BY tenant_id;
-- SELECT tenant_id, role_id, role_key FROM sys_role ORDER BY tenant_id, role_id;
