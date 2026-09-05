-- =====================================================================
-- M7_03 专业版开放平台 API 脚本（T7.8：对外 RESTful + 签名鉴权 + 配额限流 + 审计 + Webhook）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》T7.8 / 需求文档 4.1「企业微信 + 开放平台」
-- 开关：FEAT_OPEN_API（专业版专属，接口层由 @RequireFeature 拦截；对外网关另按 T13.5 通道化）
-- 前置依赖：M2_02 v2.0（角色×版本模板）/ M2_08（租户级克隆角色）
-- 规范：
--   1. 先删后建 / 先删后插，脚本可重复执行
--   2. 逐表含 tenant_id + 租户索引；建表 ≠ 开放，开放由 FEAT_OPEN_API 控制
--   3. app_secret / webhook secret 密文落库（应用层 AES 加密，同企微密钥策略 T10.1）
--   4. menu_id 顺延占用 3340~3347（3325~3331 属 M7_02 账套，3400+ 属 M11）
-- 说明：
--   配额统计不另设计数表——按 open_api_call_log 的 create_time 聚合当日/当月调用次数，
--   与「全量调用审计日志」共用一份数据，避免计数与审计不一致
-- =====================================================================

-- ----------------------------
-- 1. 开放平台凭证表（FEAT_OPEN_API；AppKey + AppSecret 签名鉴权，按租户日/月配额）
-- ----------------------------
DROP TABLE IF EXISTS `open_api_credential`;
CREATE TABLE `open_api_credential` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '凭证ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `app_name` VARCHAR(100) DEFAULT NULL COMMENT '应用名称',
  `app_key` VARCHAR(64) NOT NULL COMMENT 'AppKey(对外唯一标识)',
  `app_secret` VARCHAR(255) NOT NULL COMMENT 'AppSecret(AES加密存储，HMAC-SHA256签名密钥)',
  `quota_daily` INT DEFAULT 10000 COMMENT '日配额(次/天，NULL或<=0表示不限)',
  `quota_monthly` INT DEFAULT 200000 COMMENT '月配额(次/月，NULL或<=0表示不限)',
  `ip_whitelist` VARCHAR(500) DEFAULT NULL COMMENT 'IP白名单(逗号分隔，空=不限)',
  `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE启用/DISABLED停用)',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间(空=永久有效)',
  `last_call_time` DATETIME DEFAULT NULL COMMENT '最近调用时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_app_key` (`app_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台API凭证表(专业版 FEAT_OPEN_API)';

-- ----------------------------
-- 2. 调用审计日志表（全量留痕 + 配额统计数据来源）
--    仅追加不修改，无 update_* 列；请求/响应体脱敏截断存储
-- ----------------------------
DROP TABLE IF EXISTS `open_api_call_log`;
CREATE TABLE `open_api_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `credential_id` BIGINT DEFAULT NULL COMMENT '凭证ID(open_api_credential)',
  `app_key` VARCHAR(64) DEFAULT NULL COMMENT 'AppKey(冗余，便于凭证删除后仍可追溯)',
  `api_path` VARCHAR(200) DEFAULT NULL COMMENT '接口路径',
  `http_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP方法(GET/POST/...)',
  `request_param` TEXT DEFAULT NULL COMMENT '请求参数(脱敏)',
  `response_code` INT DEFAULT NULL COMMENT 'HTTP响应码',
  `response_body` TEXT DEFAULT NULL COMMENT '响应体(截断)',
  `cost_ms` BIGINT DEFAULT NULL COMMENT '耗时(毫秒)',
  `client_ip` VARCHAR(64) DEFAULT NULL COMMENT '客户端IP',
  `success` TINYINT DEFAULT 1 COMMENT '是否成功(0否 1是)',
  `error_msg` VARCHAR(500) DEFAULT NULL COMMENT '失败原因(签名错误/超配额/...)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_app_key_time` (`app_key`, `create_time`),
  KEY `idx_tenant_time` (`tenant_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台API调用审计日志(FEAT_OPEN_API)';

-- ----------------------------
-- 3. Webhook 回调注册表（审批状态回调等事件订阅）
-- ----------------------------
DROP TABLE IF EXISTS `open_api_webhook`;
CREATE TABLE `open_api_webhook` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Webhook ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `credential_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID(open_api_credential)',
  `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型(APPROVAL_PASSED审批通过/APPROVAL_REJECTED驳回/BILL_CREATED单据创建/BUDGET_ALERT预算预警)',
  `callback_url` VARCHAR(500) NOT NULL COMMENT '回调地址(HTTPS)',
  `secret` VARCHAR(255) DEFAULT NULL COMMENT '回调签名密钥(AES加密，HMAC-SHA256验签)',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用(0否 1是)',
  `retry_times` INT DEFAULT 3 COMMENT '失败重试次数',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_credential` (`credential_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放平台Webhook回调注册表(FEAT_OPEN_API)';

-- ----------------------------
-- 4. 菜单：开放平台管理页（挂费用管控目录 3000，专业版）
--    C 型主菜单 3340 + F 型按钮 3341~3347
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (3340, 3341, 3342, 3343, 3344, 3345, 3346, 3347);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3340, '开放平台', 3000, 47, 'fee/openApi', 'fee/openApi/index', NULL, 1, 0, 'C', '0', '0', 'fee:openApi:list', 'link', 'admin', NOW(), '', NULL, '专业版 FEAT_OPEN_API 对外API凭证/审计/回调');

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3341, '凭证查询', 3340, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:openApi:query',       '#', 'admin', NOW(), '', NULL, ''),
(3342, '凭证新增', 3340, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:openApi:add',         '#', 'admin', NOW(), '', NULL, '生成 AppKey/AppSecret'),
(3343, '凭证编辑', 3340, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:openApi:edit',        '#', 'admin', NOW(), '', NULL, '配额/白名单/状态'),
(3344, '凭证删除', 3340, 4, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:openApi:remove',      '#', 'admin', NOW(), '', NULL, ''),
(3345, '重置密钥', 3340, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:openApi:resetSecret', '#', 'admin', NOW(), '', NULL, '重新生成 AppSecret'),
(3346, '调用日志', 3340, 6, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:openApi:callLog',     '#', 'admin', NOW(), '', NULL, '全量调用审计'),
(3347, 'Webhook配置', 3340, 7, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:openApi:webhook',  '#', 'admin', NOW(), '', NULL, '事件回调注册');

-- ----------------------------
-- 5. 角色×版本模板登记（专业版专属，防串第④层：低版本模板不含）
--    开放平台为财务/系统管理职能，仅授予 FINANCE_PROFESSIONAL
-- ----------------------------
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` IN (3340, 3341, 3342, 3343, 3344, 3345, 3346, 3347);
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('FINANCE_PROFESSIONAL', 3340), ('FINANCE_PROFESSIONAL', 3341), ('FINANCE_PROFESSIONAL', 3342),
('FINANCE_PROFESSIONAL', 3343), ('FINANCE_PROFESSIONAL', 3344), ('FINANCE_PROFESSIONAL', 3345),
('FINANCE_PROFESSIONAL', 3346), ('FINANCE_PROFESSIONAL', 3347);

-- ----------------------------
-- 6. 存量租户：为已克隆的 finance 角色按租户版本模板补授（幂等 NOT EXISTS 去重，同 M5_04）
--    仅专业版租户命中 FINANCE_PROFESSIONAL 模板；基础版/高级版租户无匹配行
-- ----------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, tpl.`menu_id`
FROM `sys_role` r
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
JOIN `sys_edition_menu_template` tpl
  ON tpl.`edition` = CONCAT(UPPER(r.`role_key`), '_',
       UPPER(CASE WHEN t.`edition` = 'ENTERPRISE' THEN 'PROFESSIONAL' ELSE t.`edition` END))
WHERE r.`tenant_id` IS NOT NULL
  AND r.`role_key` = 'finance'
  AND r.`del_flag` = '0'
  AND tpl.`menu_id` IN (3340, 3341, 3342, 3343, 3344, 3345, 3346, 3347)
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
      WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = tpl.`menu_id`
  );

-- ----------------------------
-- 验收查询
-- ----------------------------
-- SHOW CREATE TABLE open_api_credential;
-- SHOW CREATE TABLE open_api_call_log;
-- SHOW CREATE TABLE open_api_webhook;
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 3340 AND 3347;  -- 预期 8 行
-- 防串：低版本模板不得含开放平台菜单（预期 0）
-- SELECT COUNT(*) FROM sys_edition_menu_template
--  WHERE edition IN ('BOSS_BASIC','FINANCE_BASIC','BOSS_ADVANCED','FINANCE_ADVANCED','BOSS_PROFESSIONAL')
--    AND menu_id BETWEEN 3340 AND 3347;
-- 存量专业版租户财务克隆角色应获授（预期每租户 8）
-- SELECT t.tenant_code, r.role_id, COUNT(*) FROM sys_role_menu rm
--  JOIN sys_role r ON r.role_id = rm.role_id JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE rm.menu_id BETWEEN 3340 AND 3347 AND r.role_key = 'finance'
--  GROUP BY t.tenant_code, r.role_id;
