-- =====================================================================
-- M15_15 sys_tenant 补 tenant_type / version_type 列（企业版独立部署路径）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档》v1.16 T15.25 / T15.26
-- 背景：
--   M15_10 小微版分档脚本含 tenant_type / version_type 补列 + role_template 分档配方，
--   但 M15_10 依赖 M15_01（role_template 表），而 M15_01 仅归入小微版部署路径。
--   企业版（系统管理 / 高级版 / 专业版）部署路径不含 M15_01~M15_08，
--   而 SysTenant Java 实体已含 tenantType / versionType 字段，
--   MyBatis-Plus 全量 SELECT 会报 Unknown column 'tenant_type' in 'field list'。
-- 本脚本：
--   从 M15_10 提取 sys_tenant 补列 + 存量回填 + 索引部分，无 role_template 依赖，
--   企业版部署路径可独立执行。与 M15_10 完全兼容（幂等 INFORMATION_SCHEMA 守卫）。
-- 前置依赖：无（仅依赖 sys_tenant 表已存在）
-- =====================================================================

-- ----------------------------
-- 1. sys_tenant 增加 tenant_type / version_type 列（幂等条件 ALTER）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_15_add_tenant_type_columns`;
DELIMITER $$
CREATE PROCEDURE `m15_15_add_tenant_type_columns`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'tenant_type') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `tenant_type` VARCHAR(16) NOT NULL DEFAULT 'enterprise'
                COMMENT '产品线：micro(小微版)/enterprise(企业版)；T15.25 硬隔离 + T15.29 级联' AFTER `edition`;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'version_type') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `version_type` VARCHAR(20) DEFAULT NULL
                COMMENT '版本档位：micro_lite/micro_pro/basic/advanced/pro；T15.26 分档' AFTER `tenant_type`;
    END IF;
END$$
DELIMITER ;
CALL m15_15_add_tenant_type_columns();
DROP PROCEDURE IF EXISTS `m15_15_add_tenant_type_columns`;

-- ----------------------------
-- 2. 存量租户回填 tenant_type / version_type（按 edition 映射，幂等 UPDATE）
--    企业线：BASIC→basic / ADVANCED→advanced / PROFESSIONAL→pro；tenant_type=enterprise
--    小微线存量（若有 edition=MICRO）：默认 micro_pro（收费档），tenant_type=micro
-- ----------------------------
UPDATE `sys_tenant` SET `tenant_type` = 'enterprise', `version_type` = 'basic'      WHERE `edition` = 'BASIC'      AND (`version_type` IS NULL OR `version_type` = '');
UPDATE `sys_tenant` SET `tenant_type` = 'enterprise', `version_type` = 'advanced'   WHERE `edition` = 'ADVANCED'   AND (`version_type` IS NULL OR `version_type` = '');
UPDATE `sys_tenant` SET `tenant_type` = 'enterprise', `version_type` = 'pro'        WHERE `edition` IN ('PROFESSIONAL','ENTERPRISE') AND (`version_type` IS NULL OR `version_type` = '');
UPDATE `sys_tenant` SET `tenant_type` = 'micro',      `version_type` = 'micro_pro'  WHERE `edition` = 'MICRO'      AND (`version_type` IS NULL OR `version_type` = '');

-- ----------------------------
-- 3. 索引：支撑按 tenant_type / version_type 过滤（幂等条件 ALTER）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_15_add_tenant_type_index`;
DELIMITER $$
CREATE PROCEDURE `m15_15_add_tenant_type_index`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND INDEX_NAME = 'idx_tenant_type_version') THEN
        ALTER TABLE `sys_tenant` ADD INDEX `idx_tenant_type_version` (`tenant_type`, `version_type`);
    END IF;
END$$
DELIMITER ;
CALL m15_15_add_tenant_type_index();
DROP PROCEDURE IF EXISTS `m15_15_add_tenant_type_index`;

-- ----------------------------
-- 4. 验收查询
-- ----------------------------
-- SELECT COLUMN_NAME, COLUMN_DEFAULT, COLUMN_COMMENT FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_tenant' AND COLUMN_NAME IN ('tenant_type','version_type');
-- 预期：2 行

-- SELECT id, company_name, edition, tenant_type, version_type FROM sys_tenant ORDER BY id;
-- 预期：edition=BASIC→tenant_type=enterprise/version_type=basic；ADVANCED→advanced；PROFESSIONAL→pro
