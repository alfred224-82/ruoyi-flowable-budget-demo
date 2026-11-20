-- =====================================================================
-- M1_06 sys_tenant 业态包与访问通道扩展脚本（T1.8 / T14.2，v1.12 新增）
-- 版本：v1.1  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档》v1.12 T1.8
--       《资金管控系统设计文档》v1.3 §1.4（三维组合：业态包×版本开关×访问通道）/ §1.8.1（租户实例层）
--       《多业态SaaS平台需求规格说明书》v1.0 §4.1（访问通道与版本绑定）/ §7.2（公众号 AppID 反查）
-- v1.1 变更：新增 grace_period_until 列（T14.2 只读宽限期，到期后 3 天只读访问）
-- 前置依赖：M1_05_platform_foundation_tables.sql（biz_bundle 业态包初始数据）
-- 规范：
--   1. 基于 information_schema 条件判断，可安全重复执行（幂等，参照 M3_05 / BUG-002 教训）
--   2. 存量租户回填 biz_code=fund_control + access_channel=wechat（升级后非空）
--   3. wecom_secret 为企微自建应用凭据，须【加密存储】、接口返回【脱敏】（代码层处理，见 T10.1）
--   4. mp_app_id 为公众号 AppID 反查租户用（引导态白名单，T8.8/T13.1），随本次租户扩展一并建列，
--      避免后续二次 ALTER；引导态识别绝不写入 TenantContext（设计文档 1.8.4 硬约束）
-- =====================================================================

-- ----------------------------
-- 1. sys_tenant 逐列幂等补齐（业态包 / 访问通道 / 企微凭据 / 公众号 AppID / 宽限期）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m1_06_add_tenant_biz_channel`;
DELIMITER $$
CREATE PROCEDURE `m1_06_add_tenant_biz_channel`()
BEGIN
    -- 1.1 业态包编码（默认 fund_control）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'biz_code') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `biz_code` VARCHAR(50) NOT NULL DEFAULT 'fund_control' COMMENT '业态包编码(biz_bundle.biz_code)：fund_control/boardgame/fresh_food' AFTER `edition`;
    END IF;

    -- 1.2 访问通道（默认 wechat）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'access_channel') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `access_channel` VARCHAR(20) NOT NULL DEFAULT 'wechat' COMMENT '访问通道：wechat(公众号+小程序)/wecom(企业微信自建应用)' AFTER `biz_code`;
    END IF;

    -- 1.3 企业微信自建应用凭据（FEAT_WECOM，高级版+；secret 加密存储）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'wecom_corp_id') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `wecom_corp_id`  VARCHAR(100) DEFAULT NULL COMMENT '企业微信 CorpID' AFTER `access_channel`,
            ADD COLUMN `wecom_agent_id` VARCHAR(100) DEFAULT NULL COMMENT '企业微信自建应用 AgentId' AFTER `wecom_corp_id`,
            ADD COLUMN `wecom_secret`   VARCHAR(500) DEFAULT NULL COMMENT '企业微信自建应用 Secret(加密存储,接口返回脱敏)' AFTER `wecom_agent_id`;
    END IF;

    -- 1.4 公众号 AppID（引导态反查租户，T8.8/T13.1）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'mp_app_id') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `mp_app_id` VARCHAR(100) DEFAULT NULL COMMENT '微信公众号 AppID(引导态反查租户,不写 TenantContext)' AFTER `wecom_secret`,
            ADD KEY `idx_tenant_biz_code` (`biz_code`),
            ADD KEY `idx_tenant_mp_app_id` (`mp_app_id`);
    END IF;

    -- 1.5 只读宽限期截止日（T14.2：到期后 3 天内允许只读访问，GRACE 状态专用）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'grace_period_until') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `grace_period_until` DATETIME DEFAULT NULL COMMENT '只读宽限期截止日（T14.2：到期后3天内允许只读访问）' AFTER `status`;
    END IF;
END$$
DELIMITER ;
CALL m1_06_add_tenant_biz_channel();
DROP PROCEDURE IF EXISTS `m1_06_add_tenant_biz_channel`;

-- ----------------------------
-- 2. 存量租户回填（biz_code / access_channel 兜底非空；天然幂等，重复执行影响 0 行）
-- ----------------------------
UPDATE `sys_tenant` SET `biz_code` = 'fund_control' WHERE `biz_code` IS NULL OR `biz_code` = '';
UPDATE `sys_tenant` SET `access_channel` = 'wechat'  WHERE `access_channel` IS NULL OR `access_channel` = '';

-- ----------------------------
-- 3. 验收查询（执行后人工核对）
-- ----------------------------
-- 3.1 存量租户升级后 biz_code / access_channel 非空
-- SELECT COUNT(*) FROM sys_tenant WHERE biz_code IS NULL OR biz_code = '' OR access_channel IS NULL OR access_channel = '';
-- 预期：0
--
-- 3.2 列与索引已建立
-- SHOW CREATE TABLE sys_tenant;
--
-- 3.3 按业态/通道分布
-- SELECT biz_code, access_channel, COUNT(*) FROM sys_tenant GROUP BY biz_code, access_channel;
