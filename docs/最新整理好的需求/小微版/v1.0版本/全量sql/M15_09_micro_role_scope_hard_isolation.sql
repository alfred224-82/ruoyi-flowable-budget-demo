-- =====================================================================
-- M15_09 角色可见性硬隔离脚本（T15.25，v1.14 新增 / 对齐《小微版完整方案》v10.2）
-- 版本：v1.0  日期：2026-09-08
-- 依据：《小微版完整方案》v10.5 第二章 2.2（角色可见性硬隔离）
--       《资金管控系统开发任务文档》v1.14 T15.25
-- 前置依赖：M2_01_roles.sql（标准角色集已建）、M15_07（salesman/customer 已补）
-- 规范：
--   1. tenant_type_scope 补列走 INFORMATION_SCHEMA 条件判断（幂等）
--   2. ★硬隔离发生在数据源层（SQL WHERE），非前端隐藏；平台管理员「可选角色列表」接口
--      按本租户 tenant_type 过滤：SELECT ... WHERE tenant_type_scope IN (tenant_type,'both')
--   3. tenant_role_mapping 写入校验 role_code 的 scope 与本租户 tenant_type 匹配，越线报错
--   4. 标准角色 scope 归属：
--        boss=both（小微/企业共用）
--        salesman/customer=micro（小微专属）
--        manager(project_manager)/employee(expense_staff)/finance=enterprise（企业专属）
--        platform_admin=both（平台级管理员，跨产品线）
--   5. 索引 idx_role_scope_status 支撑可选角色列表高频查询
-- =====================================================================

-- ----------------------------
-- 1. sys_role 增加 tenant_type_scope 列（幂等条件 ALTER；默认 both，存量回填按角色归属）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_09_add_tenant_type_scope`;
DELIMITER $$
CREATE PROCEDURE `m15_09_add_tenant_type_scope`()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_role' AND COLUMN_NAME = 'tenant_type_scope') THEN
        ALTER TABLE `sys_role`
            ADD COLUMN `tenant_type_scope` VARCHAR(16) NOT NULL DEFAULT 'both'
                COMMENT '角色产品线归属：micro(小微专属)/enterprise(企业专属)/both(双线共用)；T15.25 硬隔离' AFTER `status`;
    END IF;
END$$
DELIMITER ;
CALL m15_09_add_tenant_type_scope();
DROP PROCEDURE IF EXISTS `m15_09_add_tenant_type_scope`;

-- ----------------------------
-- 2. 存量标准角色回填 tenant_type_scope（按 role_key 精确回填，幂等 UPDATE）
--    ★boss=both（双线共用）；salesman/customer=micro；manager/employee/finance=enterprise；platform_admin=both
-- ----------------------------
UPDATE `sys_role` SET `tenant_type_scope` = 'both'       WHERE `role_key` IN ('boss', 'platform_admin');
UPDATE `sys_role` SET `tenant_type_scope` = 'micro'      WHERE `role_key` IN ('salesman', 'customer');
UPDATE `sys_role` SET `tenant_type_scope` = 'enterprise' WHERE `role_key` IN ('manager', 'employee', 'finance');

-- ----------------------------
-- 3. 索引：支撑「可选角色列表」高频查询（按 scope + status 过滤）
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
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 tenant_type_scope 列存在且默认 both
-- SELECT COLUMN_NAME, COLUMN_DEFAULT, COLUMN_COMMENT FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_role' AND COLUMN_NAME='tenant_type_scope';
--
-- 4.2 标准角色 scope 归属正确
-- SELECT role_id, role_key, role_name, tenant_type_scope FROM sys_role
--  WHERE role_key IN ('boss','salesman','customer','manager','employee','finance','platform_admin')
--  ORDER BY role_id;
-- 预期：boss/platform_admin=both；salesman/customer=micro；manager/employee/finance=enterprise
--
-- 4.3 ★硬隔离验证（运行时，T15.25 接口部署后）：
--   小微租户(tenant_type=micro) 调 /micro/tenantRole/optional-list：
--     SELECT role_key FROM sys_role WHERE tenant_type_scope IN ('micro','both') AND status='0' AND tenant_id IS NULL;
--     预期返回：boss, salesman, customer, platform_admin（不含 manager/employee/finance）
--   企业租户(tenant_type=enterprise) 调同接口：
--     SELECT role_key FROM sys_role WHERE tenant_type_scope IN ('enterprise','both') AND status='0' AND tenant_id IS NULL;
--     预期返回：boss, manager, employee, finance, platform_admin（不含 salesman/customer）
--
-- 4.4 跨线映射写入被拒（运行时）：
--   小微租户尝试 INSERT tenant_role_mapping(role_code='manager') → 服务层校验 scope 不匹配 → 抛 ServiceException
