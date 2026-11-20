-- =====================================================================
-- M15_09 sys_role 补 tenant_type_scope 列（角色可见性硬隔离，T15.25）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档》v1.16 T15.25 / T15.26
--       《小微版完整方案》v10.5 §2.1.0 角色可见性硬隔离
-- 背景：
--   SysRole Java 实体含 tenantTypeScope 字段（映射 tenant_type_scope），
--   TenantRoleMappingServiceImpl.listOptionalRoles() 按此列做数据源层过滤，
--   小微版 M15_09 原脚本含补列 + 回填 + 索引，但该脚本仅归入小微版部署路径。
--   企业版（系统管理 / 高级版 / 专业版）部署路径不含小微版 M15 脚本，
--   而 MyBatis-Plus 全量 SELECT sys_role 会报 Unknown column 'tenant_type_scope'。
-- 本脚本：
--   从 M15_09 提取 sys_role 补列 + 存量回填 + 索引部分，无小微版前置依赖，
--   企业版部署路径可独立执行。与小微版 M15_09 完全兼容（幂等 INFORMATION_SCHEMA 守卫）。
-- 前置依赖：无（仅依赖 sys_role 表已存在）
-- =====================================================================

-- ----------------------------
-- 1. sys_role 增加 tenant_type_scope 列（幂等条件 ALTER）
--    micro=小微版专属 / enterprise=企业版专属 / both=双线共用
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_09_add_tenant_type_scope`;
DELIMITER $$
CREATE PROCEDURE `m15_09_add_tenant_type_scope`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND COLUMN_NAME = 'tenant_type_scope') THEN
        ALTER TABLE `sys_role`
            ADD COLUMN `tenant_type_scope` VARCHAR(20) DEFAULT 'both'
                COMMENT '角色产品线归属：micro(小微专属)/enterprise(企业专属)/both(双线共用)；T15.25 硬隔离'
                AFTER `remark`;
    END IF;
END$$
DELIMITER ;
CALL m15_09_add_tenant_type_scope();
DROP PROCEDURE IF EXISTS `m15_09_add_tenant_type_scope`;

-- ----------------------------
-- 2. 存量标准角色回填 tenant_type_scope（幂等 UPDATE，仅更新 NULL 或空值行）
--    归属规则（§2.1.0）：
--      boss / platform_admin / admin → both（双线共用）
--      salesman / customer           → micro（小微专属）
--      manager / employee / finance  → enterprise（企业专属）
-- ----------------------------
UPDATE `sys_role` SET `tenant_type_scope` = 'both'
 WHERE `role_key` IN ('boss', 'platform_admin', 'admin')
   AND (`tenant_type_scope` IS NULL OR `tenant_type_scope` = '');
UPDATE `sys_role` SET `tenant_type_scope` = 'micro'
 WHERE `role_key` IN ('salesman', 'customer')
   AND (`tenant_type_scope` IS NULL OR `tenant_type_scope` = '');
UPDATE `sys_role` SET `tenant_type_scope` = 'enterprise'
 WHERE `role_key` IN ('manager', 'employee', 'finance')
   AND (`tenant_type_scope` IS NULL OR `tenant_type_scope` = '');

-- ----------------------------
-- 3. 索引：支撑数据源层按 tenant_type_scope + status 过滤（幂等条件 ALTER）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_09_add_role_scope_index`;
DELIMITER $$
CREATE PROCEDURE `m15_09_add_role_scope_index`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND INDEX_NAME = 'idx_role_scope_status') THEN
        ALTER TABLE `sys_role` ADD INDEX `idx_role_scope_status` (`tenant_type_scope`, `status`);
    END IF;
END$$
DELIMITER ;
CALL m15_09_add_role_scope_index();
DROP PROCEDURE IF EXISTS `m15_09_add_role_scope_index`;

-- ----------------------------
-- 4. 验收查询
-- ----------------------------
-- SELECT COLUMN_NAME, COLUMN_DEFAULT, COLUMN_COMMENT FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_role' AND COLUMN_NAME='tenant_type_scope';
-- 预期：1 行，DEFAULT='both'

-- SELECT role_key, tenant_type_scope FROM sys_role
--  WHERE role_key IN ('boss','salesman','customer','platform_admin','manager','employee','finance','admin')
--  ORDER BY role_key;
-- 预期：admin/boss/platform_admin=both；salesman/customer=micro；manager/employee/finance=enterprise
