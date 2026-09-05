-- =============================================
-- 多租户管理体系 + 公司福利政策配置
-- 设计文档 Phase 16 + Phase 17
-- 创建日期：2026-08-01
-- =============================================

-- ----------------------------
-- 1. 租户信息表
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT '租户编码',
  `company_name` VARCHAR(200) NOT NULL COMMENT '企业名称',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `industry` VARCHAR(50) DEFAULT NULL COMMENT '行业',
  `scale` VARCHAR(20) DEFAULT NULL COMMENT '规模（SMALL/MEDIUM/LARGE）',
  `edition` VARCHAR(20) NOT NULL DEFAULT 'BASIC' COMMENT '版本（BASIC/ADVANCED/ENTERPRISE）',
  `license_type` VARCHAR(20) DEFAULT 'TRIAL' COMMENT '授权类型（TRIAL/FORMAL）',
  `expire_date` DATETIME DEFAULT NULL COMMENT '授权到期日',
  `max_users` INT DEFAULT 10 COMMENT '最大用户数',
  `features` TEXT COMMENT '功能开关JSON',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态（PENDING/NORMAL/DISABLED/EXPIRED）',
  `admin_user_id` BIGINT DEFAULT NULL COMMENT '租户管理员用户ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户信息表';

-- ----------------------------
-- 2. 租户版本变更日志表
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_version_log`;
CREATE TABLE `sys_tenant_version_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `from_edition` VARCHAR(20) DEFAULT NULL COMMENT '变更前版本',
  `to_edition` VARCHAR(20) NOT NULL COMMENT '变更后版本',
  `change_type` VARCHAR(20) NOT NULL COMMENT '变更类型（INIT/UPGRADE/DOWNGRADE）',
  `operator` VARCHAR(50) DEFAULT NULL COMMENT '操作人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户版本变更日志';

-- ----------------------------
-- 3. 租户级功能配置表
-- ----------------------------
DROP TABLE IF EXISTS `sys_tenant_config`;
CREATE TABLE `sys_tenant_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT 'JSON配置值',
  `config_type` VARCHAR(50) DEFAULT NULL COMMENT '配置分类',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_config` (`tenant_id`, `config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户级功能配置';

-- ----------------------------
-- 4. 公司福利政策配置表
-- ----------------------------
DROP TABLE IF EXISTS `fee_welfare_config`;
CREATE TABLE `fee_welfare_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `welfare_code` VARCHAR(50) NOT NULL COMMENT '福利编码',
  `welfare_name` VARCHAR(100) NOT NULL COMMENT '福利名称',
  `category` VARCHAR(30) NOT NULL COMMENT '福利大类（MEAL/TRAVEL/TRANSPORT/OTHER）',
  `sub_category` VARCHAR(50) DEFAULT NULL COMMENT '福利子类',
  `region` VARCHAR(20) DEFAULT 'ALL' COMMENT '适用区域（DOMESTIC/INTERNATIONAL/ALL）',
  `trip_duration` VARCHAR(20) DEFAULT 'ANY' COMMENT '出差时长（SHORT/LONG/ANY）',
  `city_level` VARCHAR(20) DEFAULT 'ANY' COMMENT '城市等级（TIER1/TIER2/TIER3/ANY）',
  `country_group` VARCHAR(30) DEFAULT 'ANY' COMMENT '国家分组（HK_MO_TW/ASIA/EUROPE_AMERICA/OTHER/ANY）',
  `standard_amount` DECIMAL(12,2) NOT NULL COMMENT '补助标准金额',
  `unit` VARCHAR(20) NOT NULL DEFAULT 'DAY' COMMENT '计量单位（DAY/MONTH/TIME/MEAL）',
  `max_limit` INT DEFAULT 0 COMMENT '最大享受天数/次数（0=不限）',
  `applicable_rank` VARCHAR(50) DEFAULT 'ALL' COMMENT '适用职级',
  `effective_date` DATE DEFAULT NULL COMMENT '生效日期',
  `expiry_date` DATE DEFAULT NULL COMMENT '失效日期',
  `status` VARCHAR(20) DEFAULT 'ENABLED' COMMENT '状态（ENABLED/DISABLED）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0代表存在 2代表删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_welfare_code` (`tenant_id`, `welfare_code`),
  KEY `idx_tenant_category` (`tenant_id`, `category`),
  KEY `idx_region_duration` (`region`, `trip_duration`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司福利政策配置表';

-- ----------------------------
-- 初始化数据：租户管理菜单
-- ----------------------------
-- 租户管理目录
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6000, '租户管理', 1, 10, 'tenant', NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'peoples', 'admin', NOW(), '', NULL, '租户管理目录');

-- 租户列表菜单
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6001, '租户列表', 6000, 1, 'list', 'system/tenant/index', NULL, 1, 0, 'C', '0', '0', 'system:tenant:list', 'list', 'admin', NOW(), '', NULL, '租户列表菜单');

-- 租户管理按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6002, '租户查询', 6001, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenant:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6003, '租户新增', 6001, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenant:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6004, '租户修改', 6001, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenant:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6005, '租户删除', 6001, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:tenant:remove', '#', 'admin', NOW(), '', NULL, '');

-- ----------------------------
-- 初始化数据：福利配置菜单
-- ----------------------------
-- 福利配置菜单（在费用管控目录下）
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6010, '福利配置', 2000, 10, 'welfare', 'system/fee/welfare/index', NULL, 1, 0, 'C', '0', '0', 'fee:welfare:list', 'money', 'admin', NOW(), '', NULL, '福利政策配置菜单');

-- 福利配置按钮
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6011, '福利查询', 6010, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:welfare:query', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6012, '福利新增', 6010, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:welfare:add', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6013, '福利修改', 6010, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:welfare:edit', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6014, '福利删除', 6010, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:welfare:remove', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6015, '福利导入', 6010, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:welfare:import', '#', 'admin', NOW(), '', NULL, '');
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES (6016, '福利导出', 6010, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:welfare:export', '#', 'admin', NOW(), '', NULL, '');
