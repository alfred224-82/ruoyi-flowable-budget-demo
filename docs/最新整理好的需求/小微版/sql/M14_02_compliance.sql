-- =====================================================================
-- M14_02 合规改造建表脚本（T14.4）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 T14.4（合规改造）
--       《多业态SaaS平台需求规格说明书》v1.0 §8（非功能与合规要求）
-- 前置依赖：M14_01_billing_settlement.sql、M13_01_access_channel.sql（biz_message_log）
-- 规范：
--   1. 先删后建 / 先删后插，脚本可重复执行（幂等）
--   2. sys_agreement_version 为【系统级共享表】(无 tenant_id)，须纳入 IGNORE_TENANT_TABLES
--   3. 微信【一次性订阅消息】：发送前校验剩余授权次数(remaining_count)，不足则不发送并引导重新授权(不报错)
--   4. 隐私政策/用户协议入口与版本留痕；用户数据导出与账号注销可在 7 日内完成并留痕
--   5. 内容安全 imgSecCheck/msgSecCheck 审核留痕复用 M12 biz_content_security_log（全覆盖）
-- =====================================================================

-- ----------------------------
-- 1. 订阅消息授权次数表 sys_subscribe_quota（微信一次性订阅消息，发一次扣一次）
-- ----------------------------
DROP TABLE IF EXISTS `sys_subscribe_quota`;
CREATE TABLE `sys_subscribe_quota` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `openid` VARCHAR(100) NOT NULL COMMENT '微信 openid',
  `template_id` VARCHAR(100) NOT NULL COMMENT '订阅消息模板ID',
  `remaining_count` INT NOT NULL DEFAULT 0 COMMENT '剩余可发送次数(授权一次+1，发送一次-1)',
  `total_granted` INT NOT NULL DEFAULT 0 COMMENT '累计授权次数',
  `total_sent` INT NOT NULL DEFAULT 0 COMMENT '累计发送次数',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_openid_tpl` (`tenant_id`, `openid`, `template_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订阅消息授权次数表(一次性订阅消息合规)';

-- ----------------------------
-- 2. 协议版本表 sys_agreement_version（系统级共享；隐私政策/用户协议版本留痕）
-- ----------------------------
DROP TABLE IF EXISTS `sys_agreement_version`;
CREATE TABLE `sys_agreement_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `agreement_type` VARCHAR(30) NOT NULL COMMENT '协议类型：privacy(隐私政策)/user(用户协议)',
  `version` VARCHAR(20) NOT NULL COMMENT '版本号(如 v1.0)',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '协议标题',
  `content_url` VARCHAR(500) DEFAULT NULL COMMENT '协议正文URL(OSS)',
  `content_hash` VARCHAR(64) DEFAULT NULL COMMENT '正文哈希(防篡改)',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0生效 1历史）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_type_version` (`agreement_type`, `version`),
  KEY `idx_type_status` (`agreement_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='协议版本表(系统级共享,IGNORE_TENANT_TABLES)';

-- ----------------------------
-- 3. 用户协议同意留痕表 sys_user_agreement_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_agreement_log`;
CREATE TABLE `sys_user_agreement_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `openid` VARCHAR(100) DEFAULT NULL COMMENT '微信 openid(小程序端)',
  `agreement_type` VARCHAR(30) NOT NULL COMMENT '协议类型：privacy/user',
  `version` VARCHAR(20) NOT NULL COMMENT '同意的版本号',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '来源IP',
  `agree_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '同意时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type_version` (`agreement_type`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户协议同意留痕表';

-- ----------------------------
-- 4. 账户数据操作留痕表 sys_account_action_log（数据导出/账号注销，7 日内完成并留痕）
-- ----------------------------
DROP TABLE IF EXISTS `sys_account_action_log`;
CREATE TABLE `sys_account_action_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `action_type` VARCHAR(30) NOT NULL COMMENT '操作类型：DATA_EXPORT(数据导出)/ACCOUNT_DELETE(账号注销)',
  `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/DONE/REJECTED',
  `result_ref` VARCHAR(500) DEFAULT NULL COMMENT '结果引用(导出文件URL/注销凭证)',
  `deadline` DATETIME DEFAULT NULL COMMENT '完成截止时间(申请后 7 日内)',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '处理人',
  `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_type_status` (`action_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户数据操作留痕表(导出/注销)';

-- =====================================================================
-- 5. 协议版本初始数据（先删后插幂等）
-- =====================================================================
DELETE FROM `sys_agreement_version` WHERE `agreement_type` IN ('privacy', 'user') AND `version` = 'v1.0';
INSERT INTO `sys_agreement_version` (`agreement_type`, `version`, `title`, `publish_time`, `status`, `create_by`, `remark`) VALUES
('privacy', 'v1.0', '隐私政策',   NOW(), '0', 'admin', '初始版本，入口在小程序/PC 登录页与"我的-关于"'),
('user',    'v1.0', '用户服务协议', NOW(), '0', 'admin', '初始版本，入口在小程序/PC 登录页与"我的-关于"');

-- =====================================================================
-- 6. 菜单：合规管理（目录 6160，platform 级，menu_id 占用 6160~6179；仅 admin 超管 role_id=1）
-- =====================================================================
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 6160 AND 6179;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(6160, '合规管理', 0, 9, 'compliance2', NULL, NULL, 1, 0, 'M', '0', '0', '', 'checkbox', 'admin', NOW(), '', NULL, '协议版本/数据合规（M14 T14.4，平台级）'),
(6161, '协议版本', 6160, 1, 'agreement', 'compliance/agreement/index', NULL, 1, 0, 'C', '0', '0', 'compliance:agreement:list', 'documentation', 'admin', NOW(), '', NULL, '隐私政策/用户协议版本留痕'),
(6162, '协议查询', 6161, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:agreement:query', '#', 'admin', NOW(), '', NULL, ''),
(6163, '协议发布', 6161, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:agreement:add',   '#', 'admin', NOW(), '', NULL, '发布新版本，旧版转历史'),
(6165, '数据合规', 6160, 2, 'action', 'compliance/action/index', NULL, 1, 0, 'C', '0', '0', 'compliance:action:list', 'druid', 'admin', NOW(), '', NULL, '数据导出/账号注销申请处理(7日内)'),
(6166, '合规查询', 6165, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:action:query',  '#', 'admin', NOW(), '', NULL, ''),
(6167, '合规处理', 6165, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'compliance:action:handle', '#', 'admin', NOW(), '', NULL, '导出/注销处理并回写结果');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 6160 AND 6179;

-- ----------------------------
-- 7. 验收查询（执行后人工核对）
-- ----------------------------
-- 7.1 协议版本初始数据
-- SELECT agreement_type, version, status FROM sys_agreement_version;  -- 预期 privacy/user 各 1 行 status=0
-- 7.2 表含 tenant_id（sys_agreement_version 除外，系统级共享）
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('sys_subscribe_quota','sys_user_agreement_log','sys_account_action_log');  -- 预期 3 行
