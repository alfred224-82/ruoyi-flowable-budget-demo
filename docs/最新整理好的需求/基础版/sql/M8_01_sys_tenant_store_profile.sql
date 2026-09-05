-- =============================================================
-- 脚本：M8_01_sys_tenant_store_profile.sql
-- 版本：v1.0
-- 目标：资金管控系统 sys_tenant 门店引导态公开信息扩展（city/address/经纬度/logo_url/store_intro）
-- 依据：《资金管控系统开发任务文档》v1.12 T8.10🆕；《资金管控系统设计文档》v2.7 第八章 8.10
-- 范围：
--   1) sys_tenant 补充门店公开信息列（引导态搜索/定位/公开展示，绝不含 contact_phone/wecom_secret/mp_app_id 等敏感凭据）
--   2) 建 city / (status,city) 索引支撑 PortalServiceImpl.searchStores 就近检索
-- 关键约束：
--   ★ 幂等可重跑：通过 information_schema 判断列/索引是否存在，不存在才新增；
--   ★ 存量零影响：新增列默认 NULL，回填不动存量业务数据；
--   ★ 说明：SysTenant 实体已声明这 6 个字段（T8.10），MyBatis-Plus selectById/selectList 会 SELECT 全部实体列，
--     故本迁移是 sys_tenant 全链路查询（含 M2 角色数校验 selectById）的前置依赖，缺列将导致查询报错。
-- =============================================================

-- ============ 1) sys_tenant 补充门店引导态公开信息列（T8.10） ============
DROP PROCEDURE IF EXISTS m8_01_add_tenant_store_profile;
DELIMITER $$
CREATE PROCEDURE m8_01_add_tenant_store_profile()
BEGIN
    -- 所在城市（引导态门店搜索/定位拒绝时城市降级选择）
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'city') THEN
        ALTER TABLE sys_tenant ADD COLUMN city varchar(64) DEFAULT NULL COMMENT '所在城市（引导态门店搜索/城市降级选择，T8.10）';
    END IF;
    -- 门店地址（引导态公开展示）
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'address') THEN
        ALTER TABLE sys_tenant ADD COLUMN address varchar(255) DEFAULT NULL COMMENT '门店地址（引导态公开展示，T8.10）';
    END IF;
    -- 门店经度（定位就近排序，-180~180）
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'longitude') THEN
        ALTER TABLE sys_tenant ADD COLUMN longitude decimal(10,7) DEFAULT NULL COMMENT '门店经度（定位就近排序，T8.10）';
    END IF;
    -- 门店纬度（定位就近排序，-90~90）
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'latitude') THEN
        ALTER TABLE sys_tenant ADD COLUMN latitude decimal(10,7) DEFAULT NULL COMMENT '门店纬度（定位就近排序，T8.10）';
    END IF;
    -- 门店Logo URL（引导态公开展示）
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'logo_url') THEN
        ALTER TABLE sys_tenant ADD COLUMN logo_url varchar(500) DEFAULT NULL COMMENT '门店Logo URL（引导态公开展示，T8.10）';
    END IF;
    -- 门店公开简介（未选定门店时的平台介绍页/公开展示）
    IF NOT EXISTS(SELECT 1 FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'store_intro') THEN
        ALTER TABLE sys_tenant ADD COLUMN store_intro varchar(1000) DEFAULT NULL COMMENT '门店公开简介（引导态公开展示，T8.10）';
    END IF;
    -- 城市检索索引（searchStores eq(city)）
    IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND INDEX_NAME = 'idx_city') THEN
        ALTER TABLE sys_tenant ADD INDEX idx_city (city);
    END IF;
    -- 状态+城市复合索引（引导态仅展示 NORMAL 门店）
    IF NOT EXISTS(SELECT 1 FROM information_schema.STATISTICS
                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND INDEX_NAME = 'idx_status_city') THEN
        ALTER TABLE sys_tenant ADD INDEX idx_status_city (status, city);
    END IF;
END$$
DELIMITER ;
CALL m8_01_add_tenant_store_profile();
DROP PROCEDURE IF EXISTS m8_01_add_tenant_store_profile;

-- ============ 验收查询 ============
-- 期望：返回 city/address/longitude/latitude/logo_url/store_intro 六列
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_tenant'
--    AND COLUMN_NAME IN ('city','address','longitude','latitude','logo_url','store_intro');
-- 期望：idx_city 与 idx_status_city 均已建
-- SELECT INDEX_NAME FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sys_tenant' AND INDEX_NAME IN ('idx_city','idx_status_city') GROUP BY INDEX_NAME;
