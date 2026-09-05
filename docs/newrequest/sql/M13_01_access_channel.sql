-- =====================================================================
-- M13_01 访问通道建表脚本（T13.1~T13.6）
-- 版本：v1.1  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12 M13（访问通道）
--       《多业态SaaS平台需求规格说明书》v1.0 §4.1（访问通道与版本绑定）/ §7.1（部署形态：主体独立、收款分离）
--       《资金管控系统设计文档》v1.3 §1.4（三维组合：访问通道）
-- 前置依赖：
--   M1_06_sys_tenant_biz_channel.sql（sys_tenant.access_channel/wecom_*/mp_app_id 已建）
--   M1_05_platform_foundation_tables.sql（user_tenant_bind 已建；feature_flag 含 FEAT_WECOM/FEAT_OPEN_API/FEAT_FLOW_CUSTOM）
--   M7_03_open_api.sql（open_api_credential/open_api_call_log/open_api_webhook 三表 + 菜单 3340~3347 perms fee:openApi:* 已建，本脚本不重复定义）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）
--   2. 业务表逐表含 tenant_id + 租户索引
--   3. wecom_secret / tenant_payment_config.api_key 须【加密存储】、接口返回【脱敏】（代码层处理）
--   4. 通道能力按开关分级：FEAT_WECOM(高级版+) / FEAT_OPEN_API+FEAT_FLOW_CUSTOM(专业版)；菜单模板按版本登记
--   5. 公众号总入口(T13.1)/端适配(T13.4) 为配置与代码逻辑，无独立建表；引导态 /portal/** 白名单绝不写 TenantContext
--   6. 开放平台(T13.5) 网关鉴权复用 M7_03 表与 fee:openApi:* 菜单，由后端 OpenApiCredentialServiceImpl 实现
-- =====================================================================

-- ----------------------------
-- 1. 消息发送记录表 biz_message_log（T13.3 消息 Provider 抽象；发送记录留痕可查，失败降级不阻塞主流程）
-- ----------------------------
DROP TABLE IF EXISTS `biz_message_log`;
CREATE TABLE `biz_message_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `access_channel` VARCHAR(20) DEFAULT NULL COMMENT '发送时租户通道：wechat/wecom',
  `provider` VARCHAR(40) NOT NULL COMMENT '消息Provider：wechat_template/wechat_kf/wecom_app',
  `biz_type` VARCHAR(40) DEFAULT NULL COMMENT '业务类型：approval/warning/expire_remind/live_remind',
  `receiver` VARCHAR(200) DEFAULT NULL COMMENT '接收者(openid/企微userid/用户ID,脱敏)',
  `template_id` VARCHAR(100) DEFAULT NULL COMMENT '模板ID',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '标题',
  `content` VARCHAR(1000) DEFAULT NULL COMMENT '内容摘要(脱敏)',
  `result` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '结果：SUCCESS/FAILED/SKIPPED(订阅授权不足跳过)',
  `error_msg` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_biz` (`tenant_id`, `biz_type`),
  KEY `idx_result` (`result`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息发送记录表(IMessageProvider 留痕)';

-- ----------------------------
-- 2. 企微通讯录同步日志表 wecom_sync_log（T13.2 通讯录同步幂等、失败可重试）
-- ----------------------------
DROP TABLE IF EXISTS `wecom_sync_log`;
CREATE TABLE `wecom_sync_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `sync_type` VARCHAR(20) NOT NULL COMMENT '同步类型：dept(部门)/user(成员)/all',
  `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/PARTIAL/FAILED',
  `total_count` INT DEFAULT 0 COMMENT '总数',
  `success_count` INT DEFAULT 0 COMMENT '成功数',
  `fail_count` INT DEFAULT 0 COMMENT '失败数',
  `error_msg` VARCHAR(2000) DEFAULT NULL COMMENT '错误详情',
  `cost_ms` BIGINT DEFAULT NULL COMMENT '耗时(毫秒)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_type` (`tenant_id`, `sync_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企微通讯录同步日志表(幂等/重试留痕)';

-- ----------------------------
-- 3. 开放平台表（open_api_credential / open_api_call_log / open_api_webhook）已由 M7_03_open_api.sql（T7.8）建表，
--    对应菜单 3340~3347（perms fee:openApi:*）亦由 M7_03 登记，本脚本不再重复定义。
--    T13.5 网关鉴权（AppKey + HMAC-SHA256 签名 + 配额/IP 白名单 + 全量审计）复用上述表，由后端 OpenApiCredentialServiceImpl 实现。
-- ----------------------------

-- ----------------------------
-- 4. 租户收款配置表 tenant_payment_config（T13.6 主体独立与收款分离；分账虚拟账户见 M14）
-- ----------------------------
DROP TABLE IF EXISTS `tenant_payment_config`;
CREATE TABLE `tenant_payment_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `mode` VARCHAR(30) NOT NULL DEFAULT 'platform_proxy' COMMENT '收款模式：self_merchant(自有商户号,收款直达)/platform_proxy(平台代收,按虚拟账户分账)',
  `merchant_id` VARCHAR(100) DEFAULT NULL COMMENT '商户号(自有商户号模式)',
  `merchant_name` VARCHAR(200) DEFAULT NULL COMMENT '商户名称',
  `api_key` VARCHAR(500) DEFAULT NULL COMMENT '商户API密钥(加密存储,接口返回脱敏)',
  `settle_account` VARCHAR(200) DEFAULT NULL COMMENT '结算账户(脱敏展示)',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户收款配置表(主体独立/收款分离)';

-- =====================================================================
-- 5. 菜单：通道与集成（目录 3480，menu_id 占用 3480~3499）
--    注：开放平台管理页归属 M7_03 的 3340~3347（fee/openApi/index），不在此重复登记
-- =====================================================================
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 3480 AND 3499;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3480, '通道与集成', 0, 7, 'channel', NULL, NULL, 1, 0, 'M', '0', '0', '', 'international', 'admin', NOW(), '', NULL, '消息记录/收款配置（M13 访问通道；开放平台见费用管控目录）'),
-- 消息记录（T13.3，基础版标配，全通道留痕查看）
(3488, '消息记录', 3480, 1, 'message', 'biz/message/index', NULL, 1, 0, 'C', '0', '0', 'biz:message:list', 'message', 'admin', NOW(), '', NULL, 'IMessageProvider 发送记录留痕'),
(3489, '消息查询', 3488, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:message:query', '#', 'admin', NOW(), '', NULL, ''),
-- 收款配置（T13.6，高级版+）
(3485, '收款配置', 3480, 2, 'payment', 'biz/payment/index', NULL, 1, 0, 'C', '0', '0', 'biz:payment:list', 'money', 'admin', NOW(), '', NULL, '主体独立/收款分离；自有商户号或平台代收分账'),
(3486, '收款查询', 3485, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:payment:query', '#', 'admin', NOW(), '', NULL, ''),
(3487, '收款保存', 3485, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'biz:payment:edit',  '#', 'admin', NOW(), '', NULL, '');

-- ----------------------------
-- 6. 三版本授权模板登记（消息记录=基础版+；收款配置=高级版+；开放平台=专业版，见 M7_03）
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` BETWEEN 3480 AND 3499;
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 目录 + 消息记录（三版本）
('BASIC', 3480), ('BASIC', 3488), ('BASIC', 3489),
('ADVANCED', 3480), ('ADVANCED', 3488), ('ADVANCED', 3489),
('PROFESSIONAL', 3480), ('PROFESSIONAL', 3488), ('PROFESSIONAL', 3489),
-- 收款配置（高级版+）
('ADVANCED', 3485), ('ADVANCED', 3486), ('ADVANCED', 3487),
('PROFESSIONAL', 3485), ('PROFESSIONAL', 3486), ('PROFESSIONAL', 3487);

-- ----------------------------
-- 7. 角色授权（boss 9201 收款配置+消息记录；finance 9202 消息记录查看）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 3480 AND 3499 AND `role_id` IN (9201, 9202);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9201, 3480), (9201, 3488), (9201, 3489), (9201, 3485), (9201, 3486), (9201, 3487),
(9202, 3480), (9202, 3488), (9202, 3489);

-- ----------------------------
-- 8. 验收查询（执行后人工核对）
-- ----------------------------
-- 8.1 本脚本三表均含 tenant_id（开放平台三表见 M7_03）
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('biz_message_log','wecom_sync_log','tenant_payment_config');  -- 预期 3 行
-- 8.2 通道与集成目录仅登记消息记录(三版本)+收款配置(高级版+)
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template WHERE menu_id BETWEEN 3480 AND 3499 GROUP BY edition;
-- 8.3 开放平台菜单由 M7_03 登记（预期 8 行，perms fee:openApi:*）
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id BETWEEN 3340 AND 3347;
