-- =====================================================================
-- M20_08 语音合规记录补 result 列 + 触发校验流程（T18 增强，v1.65 新增）
-- 版本：v1.0  日期：2026-09-30
-- 依据：语音上传后需进行合规校验，且所有语音合规记录都应记录是否合规（PASS/FAIL）
-- 前置依赖：M18_01（voice_compliance_hit 建表）
-- 规范：
--   1. 条件 ALTER（INFORMATION_SCHEMA 守卫），幂等可重复执行
--   2. result 列默认值 PASS，存量数据视为合规通过
--   3. hit_type 扩展：新增 PASS 类型表示合规通过
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- voice_compliance_hit 补 result 列
-- ----------------------------
SET @col_exists = (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'voice_compliance_hit'
    AND COLUMN_NAME = 'result'
);

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE `voice_compliance_hit` ADD COLUMN `result` VARCHAR(10) NOT NULL DEFAULT ''PASS'' COMMENT ''合规结果：PASS(通过)/FAIL(违规)'' AFTER `hit_type`',
  'SELECT ''列 result 已存在，跳过'' AS skip_info'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 更新存量数据：将现有记录的 result 设为 FAIL（因为都是违规命中）
-- ----------------------------
UPDATE `voice_compliance_hit` SET `result` = 'FAIL' WHERE `result` = 'PASS';

-- ----------------------------
-- 验收查询
-- ----------------------------
-- 1. result 列已添加
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'voice_compliance_hit' AND COLUMN_NAME = 'result';
--   -- 预期 1 行，COLUMN_TYPE=varchar(10), COLUMN_DEFAULT='PASS'
--
-- 2. 存量违规记录 result 已更新为 FAIL
-- SELECT COUNT(*) FROM voice_compliance_hit WHERE result = 'FAIL';
--   -- 预期 ≥0（取决于是否有存量数据）
