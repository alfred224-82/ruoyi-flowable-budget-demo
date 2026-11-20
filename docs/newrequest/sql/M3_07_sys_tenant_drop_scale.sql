-- =====================================================================
-- M3_07 sys_tenant 移除 scale 列（v1.28 T3.2 变更）
-- 版本：v1.0  日期：2026-09-16
-- 依据：《资金管控系统开发任务文档（系统管理）》v1.28 M3 note
--       ★v1.28 变更：移除新建租户表单中的「规模」（scale）字段——
--       租户分类完全由产品线（tenant_type）+ 版本档位（version_type）决定，
--       不再需要规模维度；提交后自动写入字段同步移除 scale。
-- 前置：无（幂等，即使 scale 列不存在也不报错）
-- 规范：
--   1. 基于 INFORMATION_SCHEMA 条件判断，可安全重复执行（幂等，参照 M1_06 / M3_05 范式）
--   2. 严禁无条件 DROP COLUMN（v1.27 硬规定 (5)：禁止 DROP TABLE / DROP COLUMN 无条件执行）
--   3. 数据丢弃已获产品明确确认，无需归档；如后续需回溯可从数据库备份恢复
--   4. 建议部署顺序：先发版（Java Entity/BO/VO 已移除 scale 字段）→ 再执行本 SQL DROP 列
-- =====================================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS `m3_07_drop_tenant_scale`;
DELIMITER $$
CREATE PROCEDURE `m3_07_drop_tenant_scale`()
BEGIN
    -- 仅当 scale 列存在时才执行 DROP，保证幂等
    IF EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME   = 'sys_tenant'
                 AND COLUMN_NAME  = 'scale') THEN
        ALTER TABLE `sys_tenant` DROP COLUMN `scale`;
    END IF;
END$$
DELIMITER ;
CALL m3_07_drop_tenant_scale();
DROP PROCEDURE IF EXISTS `m3_07_drop_tenant_scale`;

-- =====================================================================
-- 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 1) scale 列已从 sys_tenant 移除
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE()
--    AND TABLE_NAME   = 'sys_tenant'
--    AND COLUMN_NAME  = 'scale';
-- 预期：0
--
-- 2) 表结构完整性核对（其余列不受影响）
-- SHOW CREATE TABLE sys_tenant;
-- 预期含：tenant_code / company_name / industry / edition / tenant_type / version_type
--         / biz_code / access_channel / status 等；不含 scale
-- =====================================================================
