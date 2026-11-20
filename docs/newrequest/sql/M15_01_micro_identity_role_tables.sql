-- =====================================================================
-- M15_01 小微版身份与角色映射核心表建表脚本（T15.1，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第二章（角色权限模型）/ 第十六章（数据库核心表）
--       《资金管控系统开发任务文档》v1.13 T15.1
--       《多业态SaaS平台需求规格说明书》v1.0 第 11/12 章（身份识别与登录模型、标准角色集与角色映射机制）
-- 前置依赖：M2_01_roles.sql（标准角色集 sys_role 已建）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）
--   2. user_tenant_role / tenant_role_mapping 为【租户级表】，含 tenant_id + idx_tenant_id；
--      登录跨租户查询时由 TenantHelper 显式忽略过滤（与 user_tenant_bind 同策略，不入 IGNORE_TENANT_TABLES）
--   3. role_template 为【系统级共享表】（无 tenant_id），须纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES
--   4. 手机号为主键身份模型：一个手机号 = 一个 sys_user，跨端跨租户唯一；
--      user_tenant_role 支持「一人多租户、一租户多角色」
--   5. 平台管理员只能做角色映射（挑选/改名/启停/分配用户），不能新建角色 code、不能改角色权限
-- =====================================================================

-- ----------------------------
-- 1. 用户 × 租户 × 角色 关联表 user_tenant_role（支持一人多租户、多角色）
-- ----------------------------
DROP TABLE IF EXISTS `user_tenant_role`;
CREATE TABLE `user_tenant_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID(sys_user.user_id，手机号为主键)',
  `role_code` VARCHAR(64) NOT NULL COMMENT '标准角色编码(sys_role.role_key：boss/salesman/customer/...)',
  `is_primary` TINYINT DEFAULT 0 COMMENT '是否该租户下主角色(0否 1是；多角色时默认视图)',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_user_tenant` (`user_id`, `tenant_id`),
  UNIQUE KEY `uk_tenant_user_role` (`tenant_id`, `user_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户×租户×角色关联表(一人多租户多角色)';

-- ----------------------------
-- 2. 租户角色映射表 tenant_role_mapping（全版本通用：平台管理员挑选+改名+启停）
-- ----------------------------
DROP TABLE IF EXISTS `tenant_role_mapping`;
CREATE TABLE `tenant_role_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '标准角色编码(sys_role.role_key)',
  `display_name` VARCHAR(64) NOT NULL COMMENT '租户内显示名(如 salesman→"助教"、customer→"顾客")',
  `sort_order` INT DEFAULT 0 COMMENT '显示排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1禁用；不用的标准角色直接禁用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_role` (`tenant_id`, `role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户角色映射表(挑选+改名+启停,全版本通用)';

-- ----------------------------
-- 3. 角色模板表 role_template（系统级共享：菜单+区块配方，按版本 × 标准角色）
--    version_type：micro(小微版) / basic / advanced / professional(企业版各版本)
-- ----------------------------
DROP TABLE IF EXISTS `role_template`;
CREATE TABLE `role_template` (
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
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 user_tenant_role / tenant_role_mapping 含 tenant_id；role_template 为系统级（无 tenant_id）
-- SELECT TABLE_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'tenant_id'
--    AND TABLE_NAME IN ('user_tenant_role','tenant_role_mapping','role_template');
-- 预期：2 行（role_template 无 tenant_id）
--
-- 4.2 同一 user_id 可关联多个 tenant_id（一人多租户）
-- SELECT user_id, COUNT(DISTINCT tenant_id) FROM user_tenant_role GROUP BY user_id;
--
-- 4.3 role_template 按 version_type 区分小微/企业版配方
-- SELECT version_type, role_code FROM role_template ORDER BY version_type, role_code;
--
-- 4.4 ★代码侧：role_template 须加入 MybatisPlusConfig.IGNORE_TENANT_TABLES（系统级共享表）
