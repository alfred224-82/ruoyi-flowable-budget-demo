-- =====================================================================
-- M15_01 role_template 角色模板表建表脚本（系统管理包提取，企业版独立部署路径）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档》v1.16 T15.1 / T15.7
-- 背景：
--   M15_01_micro_identity_role_tables.sql 含三张表：user_tenant_role / tenant_role_mapping / role_template。
--   其中 user_tenant_role 与 tenant_role_mapping 为租户级表，归小微版部署路径；
--   而 role_template 为【系统级共享表】（无 tenant_id，已在 IGNORE_TENANT_TABLES），
--   Java 实体 RoleTemplate 已被 RoleTemplateServiceImpl / PageBlockConfigServiceImpl /
--   SysTenantServiceImpl 等多处引用，企业版部署路径同样需要该表存在。
--   原 M15_01 仅归入小微版目录，企业版（系统管理 / 高级版 / 专业版）执行 00_执行顺序.sql
--   后 role_template 不存在，运行时 SELECT 报 Table 'ry-flowable-plus.role_template' doesn't exist。
-- 本脚本：
--   从 M15_01 提取 role_template 单表建表逻辑，CREATE TABLE IF NOT EXISTS 幂等，
--   与小微版 M15_01 完全兼容（小微版先执行 M15_01 DROP+CREATE，本脚本 IF NOT EXISTS 跳过）。
-- 前置依赖：无（仅依赖 MySQL 实例本身）
-- =====================================================================

-- ----------------------------
-- 1. role_template 角色模板表（系统级共享，无 tenant_id，幂等建表）
--    version_type：micro(小微版) / basic / advanced / professional(企业版各版本)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `role_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `version_type` VARCHAR(30) NOT NULL COMMENT '版本类型：micro/basic/advanced/professional',
  `role_code` VARCHAR(64) NOT NULL COMMENT '标准角色编码(sys_role.role_key)',
  `menu_keys` JSON COMMENT '绑定菜单集合(perms 数组或 menu_id 数组)',
  `block_keys` JSON COMMENT '绑定小程序区块集合(page_block_config.block_key 数组)',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_role` (`version_type`, `role_code`),
  KEY `idx_version` (`version_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色模板表(系统级共享,IGNORE_TENANT_TABLES;菜单+区块配方)';

-- ----------------------------
-- 2. 验收查询
-- ----------------------------
-- 2.1 表存在且结构正确
-- SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_template';
-- 预期：1 行
--
-- 2.2 role_template 无 tenant_id（系统级共享表）
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_template' AND COLUMN_NAME = 'tenant_id';
-- 预期：0 行
--
-- 2.3 唯一键 uk_version_role 就位
-- SELECT INDEX_NAME, COLUMN_NAME FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'role_template' AND INDEX_NAME = 'uk_version_role';
-- 预期：2 行（version_type, role_code）
