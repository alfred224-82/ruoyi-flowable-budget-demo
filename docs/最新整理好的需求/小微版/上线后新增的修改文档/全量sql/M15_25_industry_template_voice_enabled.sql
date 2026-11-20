-- =====================================================================
-- M15_25 行业模板补 voice_enabled 列（T1.7/T18.1 三级语音控制总开关）
-- 版本：v1.0  日期：2026-09-25
-- 依据：《资金管控系统开发任务文档》v1.33 T1.7
-- 背景：
--   行业模板新增「是否可语音」开关（industry_template.voice_enabled），
--   admin 在行业模板管理中为行业开启/关闭语音能力，这是三级控制
--   （admin→platform_admin→boss）的总开关；
--   系统管理员决定「能不能」，租户管理员决定「用不用」。
--   承载于 M18 语音合规全链路（T18.1~T18.3）。
-- 本脚本：
--   幂等 ALTER（INFORMATION_SCHEMA 守卫，列已存在则跳过）；
--   存量数据回填 voice_enabled=1（默认允许语音，兼容既有行业）。
-- 前置依赖：M15_02（industry_template 建表）
-- =====================================================================

-- ----------------------------
-- 1. industry_template 补 voice_enabled 列（幂等 ALTER）
-- ----------------------------
SET @col_exists = (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'industry_template'
    AND COLUMN_NAME = 'voice_enabled'
);

SET @sql_alter = IF(@col_exists = 0,
  'ALTER TABLE `industry_template` ADD COLUMN `voice_enabled` TINYINT DEFAULT 1 COMMENT ''是否可语音（0否 1是，三级控制总开关：admin→platform_admin→boss）'' AFTER `status`',
  'SELECT ''列 voice_enabled 已存在，跳过 ALTER'' AS skip_info'
);

PREPARE stmt FROM @sql_alter;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. 存量数据回填：未设置的行默认 voice_enabled=1（兼容既有行业）
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;
UPDATE `industry_template`
SET `voice_enabled` = 1
WHERE `voice_enabled` IS NULL;
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. 验收查询
-- ----------------------------
-- 3.1 列已存在
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'industry_template' AND COLUMN_NAME = 'voice_enabled';
-- 预期：1 行（voice_enabled, tinyint, 1）
-- 3.2 全部行业 voice_enabled 非 NULL
-- SELECT industry_code, industry_name, voice_enabled FROM industry_template ORDER BY sort_order;
-- 预期：全部 voice_enabled = 1
