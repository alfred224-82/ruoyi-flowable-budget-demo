-- =====================================================================
-- M2_04 组织管理数据租户隔离改造（T2.4）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《各角色菜单权限文档》（v2.3）§2.5.2 / §3.5
-- 说明：
--   1. sys_dept 新增 tenant_id 列，启用部门租户隔离
--   2. sys_role 新增 tenant_id 列（预留，暂不启用拦截器隔离，共享角色 9201~9205 需跨租户可见）
--   3. 存量数据 tenant_id = 0（系统级，admin 超管可见）
--   4. 租户创建时自动初始化默认部门树（绑定 tenant_id）
-- =====================================================================

-- ----------------------------
-- 1. sys_dept 新增 tenant_id
-- ----------------------------
ALTER TABLE `sys_dept` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `dept_id`;
CREATE INDEX `idx_dept_tenant_id` ON `sys_dept` (`tenant_id`);

-- ----------------------------
-- 2. sys_role 新增 tenant_id（预留字段，NULL=全局共享角色）
-- ----------------------------
ALTER TABLE `sys_role` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID(NULL=全局共享)' AFTER `role_id`;
CREATE INDEX `idx_role_tenant_id` ON `sys_role` (`tenant_id`);

-- ----------------------------
-- 3. 存量数据标记为系统级（tenant_id=0）
-- ----------------------------
UPDATE `sys_dept` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL;
UPDATE `sys_role` SET `tenant_id` = 0 WHERE `tenant_id` IS NULL AND `role_id` NOT IN (9201, 9202, 9203, 9204, 9205);
-- 共享业务角色保持 NULL（跨租户可见）
UPDATE `sys_role` SET `tenant_id` = NULL WHERE `role_id` IN (9201, 9202, 9203, 9204, 9205);

-- ----------------------------
-- 4. 验收查询
-- ----------------------------
-- SELECT tenant_id, COUNT(*) AS cnt FROM sys_dept GROUP BY tenant_id;
-- SELECT tenant_id, role_id, role_key FROM sys_role ORDER BY tenant_id, role_id;
