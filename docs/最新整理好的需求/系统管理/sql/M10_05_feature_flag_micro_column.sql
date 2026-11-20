-- =====================================================================
-- M10_05 feature_flag 补 micro_enabled 列（小微版功能开关列，v1.31 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档》v1.31 T10.3 缺口封闭
-- 前置依赖：M1_05（feature_flag 建表 + 33 项开关）、M16_03（第 34 项 FEAT_PROMOTION_DISTRIBUTION）、
--           M17_01（第 35 项 FEAT_RATING）
-- 规范：
--   1. ALTER TABLE 使用 INFORMATION_SCHEMA 条件判断列是否已存在（幂等，重复执行不报 1060）
--   2. 组 E（小微版专属）开关回填 micro_enabled=1；组 A~D（企业版）回填 micro_enabled=0
--   3. 不修改既有 basic_enabled / advanced_enabled / pro_enabled 列数据
-- =====================================================================

-- ----------------------------
-- 1. 补 micro_enabled 列（幂等：列已存在则跳过）
-- ----------------------------
DROP PROCEDURE IF EXISTS `_m10_05_add_micro_enabled`;
DELIMITER $$
CREATE PROCEDURE `_m10_05_add_micro_enabled`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'feature_flag'
          AND COLUMN_NAME  = 'micro_enabled'
    ) THEN
        ALTER TABLE `feature_flag`
            ADD COLUMN `micro_enabled` TINYINT DEFAULT 0
            COMMENT '小微版默认是否开启（0否 1是；micro_lite/micro_pro 共用本列，档位区分由 sys_tenant.version_type 控制）'
            AFTER `pro_enabled`;
    END IF;
END$$
DELIMITER ;
CALL `_m10_05_add_micro_enabled`();
DROP PROCEDURE IF EXISTS `_m10_05_add_micro_enabled`;

-- ----------------------------
-- 2. 组 E（小微版专属）开关回填 micro_enabled=1
--    FEAT_PROMOTION_DISTRIBUTION（M16_03 第 34 项）
--    FEAT_RATING（M17_01 第 35 项）
-- ----------------------------
UPDATE `feature_flag` SET `micro_enabled` = 1 WHERE `code` = 'FEAT_PROMOTION_DISTRIBUTION' AND `micro_enabled` <> 1;
UPDATE `feature_flag` SET `micro_enabled` = 1 WHERE `code` = 'FEAT_RATING'                  AND `micro_enabled` <> 1;

-- ----------------------------
-- 3. 组 A~D（企业版专属）开关确保 micro_enabled=0（默认值已为 0，此处显式兜底）
-- ----------------------------
UPDATE `feature_flag` SET `micro_enabled` = 0 WHERE `flag_group` IN ('A','B','C','D') AND `micro_enabled` <> 0;

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 micro_enabled 列已存在
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'feature_flag' AND COLUMN_NAME = 'micro_enabled';
-- 预期：1 行，tinyint，默认 0
--
-- 4.2 组 E 开关 micro_enabled=1
-- SELECT code, flag_group, micro_enabled FROM feature_flag WHERE flag_group = 'E';
-- 预期：FEAT_PROMOTION_DISTRIBUTION / FEAT_RATING 两行，micro_enabled=1
--
-- 4.3 组 A~D 开关 micro_enabled=0
-- SELECT flag_group, SUM(micro_enabled) AS micro_on FROM feature_flag WHERE flag_group IN ('A','B','C','D') GROUP BY flag_group;
-- 预期：各组 micro_on=0
--
-- 4.4 开关总数不变（35 项）
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 35
