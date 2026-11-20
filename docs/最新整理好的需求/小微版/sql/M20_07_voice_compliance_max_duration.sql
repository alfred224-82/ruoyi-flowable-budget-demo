-- =====================================================================
-- M20_07 语音合规规则补列：max_voice_duration（T18 增强，v1.64 新增）
-- 版本：v1.0  日期：2026-09-30
-- 依据：租户管理员可配置活动语音最大时长（秒），Boss 上传语音时前端校验时长不超限
-- 前置依赖：M18_01（voice_compliance_rule 建表）
-- 规范：
--   1. 条件 ALTER（INFORMATION_SCHEMA 守卫），幂等可重复执行
--   2. 默认值 180 秒（3 分钟），与既有"约 3 分钟"文案对齐
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- voice_compliance_rule 补 max_voice_duration 列
-- ----------------------------
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'voice_compliance_rule'
    AND COLUMN_NAME = 'max_voice_duration'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `voice_compliance_rule` ADD COLUMN `max_voice_duration` INT NOT NULL DEFAULT 180 COMMENT ''最大语音时长（秒；默认180=3分钟；0=不限）'' AFTER `transcription_enabled`',
  'SELECT ''列 max_voice_duration 已存在，跳过'' AS skip_info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1) 列已添加
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voice_compliance_rule'
--   AND COLUMN_NAME = 'max_voice_duration';
--   -- 预期 1 行：INT, 180, '最大语音时长（秒；...）'
--
-- 2) 存量规则默认值
-- SELECT id, tenant_id, activity_type, max_voice_duration
--   FROM voice_compliance_rule;
--   -- 预期所有行 max_voice_duration = 180
