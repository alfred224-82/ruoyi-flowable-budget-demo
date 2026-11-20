-- =====================================================================
-- M18_01 语音合规规则与记录建表脚本（T18.1，v1.29 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18 V3.7 菜单骨架缺口补齐
--       T18.1 语音合规规则与记录表
-- 前置依赖：M1_05（平台底座表）、M15_06（micro 菜单 7000~7499 + client_type）
-- 规范：
--   1. voice_compliance_rule / voice_recording_policy / voice_compliance_hit
--      均为【租户级表】(含 tenant_id)，多租户插件自动隔离
--   2. sensitive_word_library 为【混合表】：tenant_id IS NULL = 平台级，非 NULL = 租户自定义
--   3. 所有表继承 BaseEntity 审计列（create_by/create_time/update_by/update_time）
--   4. 先删后建幂等（DROP TABLE IF EXISTS + CREATE TABLE）
--   5. menu_id 占用 7600~7699 区段（避让既有 7500~7509 platform_admin 菜单）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 语音合规规则表 voice_compliance_rule
--    租户级合规规则：按 activity_type 控制语音合规开关 + 敏感词覆盖策略
-- ----------------------------
DROP TABLE IF EXISTS `voice_compliance_rule`;
CREATE TABLE `voice_compliance_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `activity_type` VARCHAR(50) NOT NULL COMMENT '活动类型（如 board_game/fresh/fitness/spa 等，继承行业默认）',
  `enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '语音合规开关（0=关闭 1=开启；继承行业默认值）',
  `sensitive_word_override` VARCHAR(20) NOT NULL DEFAULT 'INHERIT' COMMENT '敏感词策略：INHERIT(继承平台底库)/OVERRIDE(仅用自定义)/MERGE(平台+自定义合并)',
  `transcription_enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '转写留痕开关（0=关闭 1=开启）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态：NORMAL(启用)/DISABLED(停用)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_activity_type` (`tenant_id`, `activity_type`),
  KEY `idx_tenant_enabled` (`tenant_id`, `enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音合规规则表(租户级;按activity_type控制语音开关+敏感词策略;按tenant_id隔离)';

-- ----------------------------
-- 2. 录音留存策略表 voice_recording_policy
--    租户级录音策略：留存天数 + 存储路径（≤ Admin 全局上限）
-- ----------------------------
DROP TABLE IF EXISTS `voice_recording_policy`;
CREATE TABLE `voice_recording_policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID（UNIQUE，每租户一条策略）',
  `max_retention_days` INT NOT NULL DEFAULT 30 COMMENT '最大留存天数（≤ Admin 全局上限；默认 30 天）',
  `storage_path` VARCHAR(500) DEFAULT NULL COMMENT '录音存储路径前缀（OSS 路径或本地路径）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='录音留存策略表(租户级;max_retention_days≤Admin全局上限;每租户一条)';

-- ----------------------------
-- 3. 合规命中记录表 voice_compliance_hit
--    租户级合规命中记录：记录语音内容触发合规规则的明细
-- ----------------------------
DROP TABLE IF EXISTS `voice_compliance_hit`;
CREATE TABLE `voice_compliance_hit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `hit_time` DATETIME NOT NULL COMMENT '命中时间',
  `hit_type` VARCHAR(50) NOT NULL COMMENT '命中类型：SENSITIVE_WORD(敏感词)/NO_TRANSCRIPTION(未转写)/POLICY_VIOLATION(策略违规)',
  `content_summary` VARCHAR(500) DEFAULT NULL COMMENT '内容摘要（脱敏后的语音转写片段，≤200 字）',
  `rule_id` BIGINT DEFAULT NULL COMMENT '关联规则ID（voice_compliance_rule.id，可空=全局策略命中）',
  `activity_id` BIGINT DEFAULT NULL COMMENT '关联活动ID（可空）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING(待处理)/ACKNOWLEDGED(已确认)/DISMISSED(已忽略)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_hit_time` (`tenant_id`, `hit_time`),
  KEY `idx_tenant_hit_type` (`tenant_id`, `hit_type`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_rule` (`tenant_id`, `rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规命中记录表(租户级;记录语音合规命中明细;按tenant_id隔离)';

-- ----------------------------
-- 4. 敏感词库表 sensitive_word_library
--    混合表：tenant_id IS NULL = 平台级敏感词（Admin 管理）
--           tenant_id 非 NULL = 租户自定义敏感词（租户管理员管理）
-- ----------------------------
DROP TABLE IF EXISTS `sensitive_word_library`;
CREATE TABLE `sensitive_word_library` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID（NULL=平台级，非NULL=租户自定义）',
  `word` VARCHAR(100) NOT NULL COMMENT '敏感词内容',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '敏感词分类：POLITICAL(政治)/PORN(色情)/VIOLENCE(暴力)/ABUSE(辱骂)/CUSTOM(自定义)',
  `level` VARCHAR(20) NOT NULL DEFAULT 'WARN' COMMENT '级别：WARN(警告)/BLOCK(拦截)',
  `source` VARCHAR(20) NOT NULL DEFAULT 'MANUAL' COMMENT '来源：MANUAL(手动添加)/IMPORT(批量导入)/SYSTEM(系统内置)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED(启用)/DISABLED(停用)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_word` (`tenant_id`, `word`),
  KEY `idx_tenant_category` (`tenant_id`, `category`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词库表(tenant_id=NULL=平台级;非NULL=租户自定义;分层存储)';

-- ----------------------------
-- 5. 平台级敏感词初始数据（tenant_id=NULL）
--    ★INSERT IGNORE 幂等
-- ----------------------------
INSERT IGNORE INTO `sensitive_word_library` (`tenant_id`, `word`, `category`, `level`, `source`, `create_by`, `create_time`, `remark`) VALUES
(NULL, '赌博', 'POLITICAL', 'BLOCK', 'SYSTEM', 'admin', NOW(), '系统内置-政治敏感'),
(NULL, '代孕', 'POLITICAL', 'BLOCK', 'SYSTEM', 'admin', NOW(), '系统内置-政治敏感'),
(NULL, '枪支', 'VIOLENCE', 'BLOCK', 'SYSTEM', 'admin', NOW(), '系统内置-暴力'),
(NULL, '毒品', 'POLITICAL', 'BLOCK', 'SYSTEM', 'admin', NOW(), '系统内置-政治敏感'),
(NULL, '色情', 'PORN', 'BLOCK', 'SYSTEM', 'admin', NOW(), '系统内置-色情'),
(NULL, '诈骗', 'ABUSE', 'WARN', 'SYSTEM', 'admin', NOW(), '系统内置-辱骂欺诈'),
(NULL, '传销', 'POLITICAL', 'BLOCK', 'SYSTEM', 'admin', NOW(), '系统内置-政治敏感'),
(NULL, '自杀', 'VIOLENCE', 'WARN', 'SYSTEM', 'admin', NOW(), '系统内置-暴力自伤');

-- =====================================================================
-- 验收查询（执行后人工核对）
-- =====================================================================
-- 1) 四张合规表均含 tenant_id 列
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('voice_compliance_rule','voice_recording_policy','voice_compliance_hit','sensitive_word_library');
--   -- 预期 4 行
--
-- 2) voice_compliance_rule 唯一约束（每租户每活动类型一条规则）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='voice_compliance_rule' AND INDEX_NAME='uk_tenant_activity_type';
--   -- 预期 uk_tenant_activity_type
--
-- 3) voice_recording_policy 唯一约束（每租户一条策略）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='voice_recording_policy' AND INDEX_NAME='uk_tenant_id';
--   -- 预期 uk_tenant_id
--
-- 4) sensitive_word_library 唯一约束（同租户/平台下敏感词不重复）
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='sensitive_word_library' AND INDEX_NAME='uk_tenant_word';
--   -- 预期 uk_tenant_word
--
-- 5) 平台级敏感词初始数据
-- SELECT COUNT(*) FROM sensitive_word_library WHERE tenant_id IS NULL;  -- 预期 ≥8
--
-- 6) 四张表均含审计列（create_by/create_time/update_by/update_time）
-- SELECT TABLE_NAME, COUNT(*) AS audit_col_count FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME IN ('voice_compliance_rule','voice_recording_policy','voice_compliance_hit','sensitive_word_library')
--   AND COLUMN_NAME IN ('create_by','create_time','update_by','update_time')
--   GROUP BY TABLE_NAME;
--   -- 预期 4 行，每行 audit_col_count=4
