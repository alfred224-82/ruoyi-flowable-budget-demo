-- =====================================================================
-- M8_01 引导态门店搜索 + 门户访问审计脚本（T8.8 / T8.10，v1.12）
-- 版本：v1.0  日期：2026-09-05
-- 依据：《资金管控系统开发任务文档》v1.12
--       T8.8  共用小程序租户识别三方式：/portal/** 白名单接口需「独立限流 + 审计日志」
--       T8.10 门店选择/搜索首页：门店列表（定位就近排序）+ 关键字搜索 + 城市降级选择
--       《多业态SaaS平台需求规格说明书》v1.0 §7.2/§7.3（引导态只返回公开展示字段）
-- 前置依赖：M1_06_sys_tenant_biz_channel.sql（sys_tenant.biz_code/access_channel/mp_app_id 已建）
-- 规范：
--   1. 补列基于 information_schema 条件判断，可安全重复执行（幂等，参照 M1_06 / M3_05）
--   2. 门店公开列仅用于「引导态公开展示」（logo/简介/城市/地址/经纬度），
--      【严禁】在此暴露 contact_phone / wecom_secret / mp_app_id 等敏感凭据（代码层 PortalStoreVo 只取公开字段）
--   3. portal_access_log 为系统级共享表（引导态跨租户、无登录态），不含 tenant_id，
--      须纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES；仅追加（无 update_* 列）
-- =====================================================================

-- ----------------------------
-- 1. sys_tenant 门店公开档案列逐列幂等补齐（城市 / 地址 / 经纬度 / Logo / 简介）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m8_01_add_tenant_store_profile`;
DELIMITER $$
CREATE PROCEDURE `m8_01_add_tenant_store_profile`()
BEGIN
    -- 1.1 城市（关键字搜索降级：定位授权拒绝时按城市选择）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'city') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `city` VARCHAR(50) DEFAULT NULL COMMENT '所在城市(引导态门店搜索/城市降级选择,T8.10)' AFTER `company_name`;
    END IF;

    -- 1.2 详细地址（公开展示）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'address') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `address` VARCHAR(255) DEFAULT NULL COMMENT '门店地址(引导态公开展示,T8.10)' AFTER `city`;
    END IF;

    -- 1.3 经纬度（定位就近排序）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'longitude') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '门店经度(就近排序,T8.10)' AFTER `address`,
            ADD COLUMN `latitude`  DECIMAL(10, 6) DEFAULT NULL COMMENT '门店纬度(就近排序,T8.10)' AFTER `longitude`;
    END IF;

    -- 1.4 Logo 与门店简介（未选定门店时的公开展示 / 平台介绍页，避免空白页）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'logo_url') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `logo_url`    VARCHAR(500) DEFAULT NULL COMMENT '门店Logo URL(引导态公开展示,T8.10)' AFTER `latitude`,
            ADD COLUMN `store_intro` VARCHAR(1000) DEFAULT NULL COMMENT '门店公开简介(平台介绍页/公开展示,T8.10)' AFTER `logo_url`,
            ADD KEY `idx_tenant_city` (`city`);
    END IF;
END$$
DELIMITER ;
CALL m8_01_add_tenant_store_profile();
DROP PROCEDURE IF EXISTS `m8_01_add_tenant_store_profile`;

-- ----------------------------
-- 2. portal_access_log 引导态门户访问审计表（系统级共享，仅追加，无 tenant_id）
--    记录每一次匿名 /portal/** 门店反查：来源IP / 端点 / 入参 / openid / 命中租户 / 结果，
--    用于安全审查（检测篡改 ?tenant= 探测、批量爬取公开展示数据）。
-- ----------------------------
DROP TABLE IF EXISTS `portal_access_log`;
CREATE TABLE `portal_access_log`
(
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `client_ip`          VARCHAR(64)  DEFAULT NULL COMMENT '来源IP',
    `endpoint`           VARCHAR(200) DEFAULT NULL COMMENT '访问端点(requestURI)',
    `request_param`      VARCHAR(2000) DEFAULT NULL COMMENT '请求参数(queryString,截断)',
    `openid`             VARCHAR(100) DEFAULT NULL COMMENT '微信openid(按openid反查时记录)',
    `resolved_tenant_id` BIGINT       DEFAULT NULL COMMENT '命中的租户ID(未命中为NULL)',
    `success`            TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功命中(0否 1是)',
    `error_msg`          VARCHAR(500) DEFAULT NULL COMMENT '失败/异常原因',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`id`),
    KEY `idx_pal_client_ip` (`client_ip`),
    KEY `idx_pal_tenant` (`resolved_tenant_id`),
    KEY `idx_pal_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='引导态门户访问审计表(T8.8,系统级共享,仅追加)';

-- ----------------------------
-- 3. 验收查询（执行后人工核对）
-- ----------------------------
-- 3.1 门店公开列已建立
-- SHOW CREATE TABLE sys_tenant;
--
-- 3.2 审计表已建立
-- SHOW CREATE TABLE portal_access_log;
--
-- 3.3 按城市分布（关键字搜索/城市降级选择基础数据）
-- SELECT city, COUNT(*) FROM sys_tenant WHERE status = 'NORMAL' GROUP BY city;
