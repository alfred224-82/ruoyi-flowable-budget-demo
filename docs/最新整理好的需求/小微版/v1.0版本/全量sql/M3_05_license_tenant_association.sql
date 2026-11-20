-- =====================================================================
-- M3_05 License 租户关联改造（T3.10 / T3.11 / T3.12 / T3.13 / T3.16）
-- 版本：v1.0  日期：2026-08-30
-- 依据：《资金管控系统开发任务文档》M3.5 License 与租户关联
-- 说明：
--   1. sys_license 新增 tenant_id 列（NULL=系统级/平台级，非 NULL=租户级）
--   2. 存量 License 保持 tenant_id = NULL（系统级兜底，admin 超管使用）
--   3. 新增索引支持按租户加载
-- =====================================================================

-- ----------------------------
-- 1. sys_license 新增 tenant_id 列（幂等）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m3_add_license_tenant_id`;
DELIMITER $$
CREATE PROCEDURE `m3_add_license_tenant_id`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_license' AND COLUMN_NAME = 'tenant_id') THEN
        ALTER TABLE `sys_license`
            ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID（NULL=系统级/平台级，非NULL=租户级）' AFTER `id`,
            ADD KEY `idx_license_tenant_id` (`tenant_id`);
    END IF;
END$$
DELIMITER ;
CALL m3_add_license_tenant_id();
DROP PROCEDURE IF EXISTS `m3_add_license_tenant_id`;

-- ----------------------------
-- 2. 存量 License 保持 tenant_id = NULL（系统级兜底）
--    无需额外操作，DEFAULT NULL 已处理
-- ----------------------------

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- SELECT id, license_id, tenant_id, edition, status FROM sys_license;
-- SHOW CREATE TABLE sys_license;
