-- =====================================================================
-- M18_10 图片合规与全局敏感词核心建表脚本（T18.1 / T18.8，v1.30 新增）
-- 版本：v1.0  日期：2026-09-19
-- 依据：《资金管控系统开发任务文档》v1.27 M18 图片合规与全局敏感词
--       《V3.8_当日草稿_图片合规与敏感词.md》
-- 前置依赖：M1_04（sys_menu）+ M1_05（feature_flag）+ M15_06（sys_menu.client_type）
-- 规范：
--   1. 先删后建幂等（DROP TABLE IF EXISTS + CREATE TABLE），三张表均为本脚本自建
--   2. sensitive_word / sensitive_word_hit_log / image_compliance_config 均含 tenant_id
--      其中 tenant_id=0 表示平台级全局记录（Admin 维护），非 0 表示租户自定义
--   3. sensitive_word 全局记录跨租户读走「条件查询」而非 IGNORE_TENANT_TABLES 白名单
--      （避免租户 A 修改到租户 B 的自定义词）
--   4. image_compliance_config tenant_id=0 存 3 行全局默认（porn/violence/politics）
--   5. feature_flag 新增 2 项开关（组 F=平台合规底线，三版本 + 小微版均默认启用）
--   6. 全部继承 BaseEntity 审计列（create_by/create_time/update_by/update_time）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 敏感词表 sensitive_word（T18.1 ①）
--    全局底库（tenant_id=0，Admin 唯一维护）+ 租户自定义（tenant_id=当前，租户管理员追加）
--    match_mode：exact(精确/contains) / fuzzy(编辑距离≤2 或拼音相似) / regex(正则匹配)
--    industry_scope：JSON 数组，NULL=全行业通用；非 NULL=仅指定 biz_code 生效
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID（0=全局底库，非0=租户自定义）',
  `word` VARCHAR(100) NOT NULL COMMENT '敏感词文本',
  `category` VARCHAR(20) NOT NULL DEFAULT 'CUSTOM' COMMENT '分类：POLITICAL/PORN/VIOLENCE/AD/SUPERSTITION/ABUSE/RUMOR/DIALECT/CUSTOM',
  `match_mode` VARCHAR(10) NOT NULL DEFAULT 'exact' COMMENT '匹配模式：exact/fuzzy/regex',
  `industry_scope` JSON DEFAULT NULL COMMENT '行业范围（JSON 数组，NULL=全部；非空=仅指定 biz_code 生效）',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态（0启用 1禁用）',
  `source` VARCHAR(10) NOT NULL DEFAULT 'global' COMMENT '来源：global(平台底库)/tenant(租户自定义)',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_word_category` (`tenant_id`, `word`, `category`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_category` (`category`),
  KEY `idx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表(T18.1，全局底库 tenant_id=0 + 租户自定义合并检测)';

-- ----------------------------
-- 2. 敏感词命中日志 sensitive_word_hit_log（T18.1 ②）
--    input_channel：text(文字输入) / voice_ocr(语音 ASR 转写) / image_ocr(图片 OCR)
--    每次调用无论 PASS/FAIL 均写日志（FAIL 记命中词+位置，PASS 仅记调用事实）
--    original_text 脱敏截取前 100 字，避免全量落库
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word_hit_log`;
CREATE TABLE `sensitive_word_hit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '触发用户ID',
  `input_channel` VARCHAR(20) NOT NULL COMMENT '输入渠道：text/voice_ocr/image_ocr',
  `hit_word` VARCHAR(100) DEFAULT NULL COMMENT '命中词（PASS 时为 NULL）',
  `hit_category` VARCHAR(20) DEFAULT NULL COMMENT '命中分类',
  `hit_position` INT DEFAULT NULL COMMENT '命中位置（原文中的字符偏移）',
  `original_text` VARCHAR(200) DEFAULT NULL COMMENT '原文脱敏截取（前 100 字）',
  `result` VARCHAR(10) NOT NULL COMMENT '结果：PASS/FAIL',
  `biz_type` VARCHAR(30) DEFAULT NULL COMMENT '业务类型：rating/activity/poster/voice/chat',
  `biz_id` VARCHAR(60) DEFAULT NULL COMMENT '业务主键',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_time` (`tenant_id`, `create_time`),
  KEY `idx_biz` (`biz_type`, `biz_id`),
  KEY `idx_channel_result` (`input_channel`, `result`),
  KEY `idx_hit_word` (`hit_word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词命中日志(T18.1，三路共用；PASS/FAIL 均记录)';

-- ----------------------------
-- 3. 图片合规配置 image_compliance_config（T18.1 ③）
--    tenant_id=0 存 3 行全局默认（porn/violence/politics）；租户可覆盖
--    confidence_threshold：视觉审核置信度阈值，低于此值判 FAIL 打回重传
--    ocr_enabled / visual_api_enabled：两路并行开关（Admin 可分别启停）
--    max_file_size_mb / allowed_formats：上传硬性约束（V3.8 §二）
-- ----------------------------
DROP TABLE IF EXISTS `image_compliance_config`;
CREATE TABLE `image_compliance_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID（0=全局默认，非0=租户覆盖）',
  `violation_type` VARCHAR(20) NOT NULL COMMENT '违规类型：porn(色情)/violence(暴恐)/politics(政治标志)',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用该类型检测（0关闭 1启用）',
  `confidence_threshold` DECIMAL(3,2) NOT NULL DEFAULT 0.85 COMMENT '置信度阈值（0.00~1.00，低于此值打回）',
  `ocr_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用 OCR 文字提取（0关闭 1启用）',
  `visual_api_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用视觉审核 API（0关闭 1启用）',
  `max_file_size_mb` INT NOT NULL DEFAULT 5 COMMENT '最大文件大小（MB）',
  `allowed_formats` VARCHAR(100) NOT NULL DEFAULT 'jpg,jpeg,png,bmp,webp' COMMENT '允许的格式（逗号分隔）',
  `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_violation_type` (`tenant_id`, `violation_type`),
  KEY `idx_tenant_enabled` (`tenant_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片合规配置(T18.1，全局默认+租户覆盖；三种违规类型 × 两路开关)';

-- ----------------------------
-- 4. image_compliance_config 全局默认三行（tenant_id=0）
--    INSERT IGNORE 幂等；不 DELETE 全表（合规硬约束）
-- ----------------------------
INSERT IGNORE INTO `image_compliance_config`
  (`tenant_id`, `violation_type`, `enabled`, `confidence_threshold`, `ocr_enabled`, `visual_api_enabled`,
   `max_file_size_mb`, `allowed_formats`, `create_by`, `remark`) VALUES
(0, 'porn',      1, 0.85, 1, 1, 5, 'jpg,jpeg,png,bmp,webp', 'admin', '全局默认：色情/低俗检测（V3.8 §一）'),
(0, 'violence',  1, 0.85, 1, 1, 5, 'jpg,jpeg,png,bmp,webp', 'admin', '全局默认：暴恐/违禁检测'),
(0, 'politics',  1, 0.90, 1, 1, 5, 'jpg,jpeg,png,bmp,webp', 'admin', '全局默认：政治标志检测（阈值提高至 0.90，避免误伤）');

-- ----------------------------
-- 5. feature_flag 新增 2 项开关（T18.8）
--    组 F = 平台合规底线（全版本通用，Admin 可全局关停）
--    basic_enabled=1 / advanced_enabled=1 / pro_enabled=1 / micro_enabled=1（若列存在）
--    先删后插幂等（仅删本 code，不动前 35 项）
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` IN ('FEAT_IMAGE_COMPLIANCE', 'FEAT_SENSITIVE_WORD');

-- 5.1 检测 micro_enabled 列是否存在（v1.29 M15_10 之后新增），存在则同时回填
SET @has_micro_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                        AND TABLE_NAME = 'feature_flag'
                        AND COLUMN_NAME = 'micro_enabled');

INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_IMAGE_COMPLIANCE', '图片合规两路并行(OCR+视觉审核)', 'F', NULL, 1, 1, 1, '0', 'admin', NOW(),
 'T18.8；★全版本通用（平台级合规底线），Admin 可通过 T18.4 管理界面全局关停'),
('FEAT_SENSITIVE_WORD',   '统一敏感词引擎(文本/语音/OCR三路共用)', 'F', NULL, 1, 1, 1, '0', 'admin', NOW(),
 'T18.8；★全版本通用（平台级合规底线），一套引擎管三路，全局底库+租户自定义合并检测');

-- 5.2 若 micro_enabled 列存在，回填 = 1（小微版也启用）
SET @sql_micro = IF(@has_micro_col > 0,
  'UPDATE `feature_flag` SET `micro_enabled` = 1 WHERE `code` IN (''FEAT_IMAGE_COMPLIANCE'', ''FEAT_SENSITIVE_WORD'')',
  'SELECT ''micro_enabled column not present, skip'' AS msg');
PREPARE stmt FROM @sql_micro;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 6. 验收查询（执行后人工核对）
-- =====================================================================
-- 6.1 三张新表建成
-- SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('sensitive_word','sensitive_word_hit_log','image_compliance_config');
-- 预期：3 行
--
-- 6.2 image_compliance_config 全局默认三行（tenant_id=0）
-- SELECT violation_type, enabled, confidence_threshold, ocr_enabled, visual_api_enabled, max_file_size_mb
--   FROM image_compliance_config WHERE tenant_id=0 ORDER BY violation_type;
-- 预期：porn(0.85) / politics(0.90) / violence(0.85) 三行，enabled/ocr/visual 均=1
--
-- 6.3 feature_flag 新增两项开关，三列均 enabled=1
-- SELECT code, flag_group, basic_enabled, advanced_enabled, pro_enabled, status
--   FROM feature_flag WHERE code IN ('FEAT_IMAGE_COMPLIANCE','FEAT_SENSITIVE_WORD');
-- 预期：2 行；flag_group='F'；三列 enabled 均=1；status='0'
--
-- 6.4 开关总数由 35 → 37
-- SELECT COUNT(*) FROM feature_flag;
-- 预期：37
--
-- 6.5 UNIQUE 约束生效
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='sensitive_word' AND INDEX_NAME='uk_tenant_word_category';
-- 预期：uk_tenant_word_category
-- =====================================================================
