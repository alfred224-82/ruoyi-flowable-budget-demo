-- =====================================================================
-- M31_01 敏感词功能激活与旧表数据补迁（v1.83 新增）
-- 版本：v1.0  日期：2026-10-17
-- 背景：
--   前端敏感词管理页已切换至新接口 /compliance/tenant/sensitiveWord/*，
--   读写 sensitive_word 表（新引擎），但存在两处数据缺口导致功能未生效：
--   ① FEAT_SENSITIVE_WORD 功能开关可能因早期脚本遗漏或手动修改而处于关闭态；
--   ② 旧表 sensitive_word_library 中用户在 M18_11 执行后通过旧接口手动添加的词
--      尚未迁移到新表，导致管理页面看不到、检测引擎也无法命中。
-- 修复：
--   1. 幂等重assert FEAT_SENSITIVE_WORD 开关（status='0' 启用 + micro_enabled=1）；
--   2. 补迁 sensitive_word_library 中尚未迁入 sensitive_word 的有效词（INSERT IGNORE 幂等）；
--   3. 补录 biz_bundle 业态包 feature_flags 确保含 FEAT_SENSITIVE_WORD（防升级拦截）；
-- 前置依赖：M18_10（sensitive_word 表 + feature_flag 开关）+ M18_11（初始底库 + 首轮迁移）
-- ★幂等：UPDATE WHERE + INSERT IGNORE + NOT JSON_CONTAINS 守卫，可重复执行
-- ★执行后请重启后端清除 SensitiveWordEngine 内存缓存（warmUp 自动重建）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 确保 FEAT_SENSITIVE_WORD 功能开关处于启用态
--    status='0'（启用）、basic/advanced/pro_enabled=1
--    仅当当前值不符合时才更新（条件守卫，幂等安全）
-- ----------------------------
UPDATE `feature_flag`
   SET `status` = '0',
       `basic_enabled` = 1,
       `advanced_enabled` = 1,
       `pro_enabled` = 1,
       `update_time` = NOW()
 WHERE `code` = 'FEAT_SENSITIVE_WORD'
   AND (`status` <> '0' OR IFNULL(`basic_enabled`, 0) <> 1 OR IFNULL(`advanced_enabled`, 0) <> 1 OR IFNULL(`pro_enabled`, 0) <> 1);

-- 1.1 若 micro_enabled 列存在，同步回填 = 1
SET @has_micro_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                      WHERE TABLE_SCHEMA = DATABASE()
                        AND TABLE_NAME = 'feature_flag'
                        AND COLUMN_NAME = 'micro_enabled');

SET @sql_micro = IF(@has_micro_col > 0,
  'UPDATE `feature_flag` SET `micro_enabled` = 1 WHERE `code` = ''FEAT_SENSITIVE_WORD'' AND IFNULL(`micro_enabled`, 0) <> 1',
  'SELECT ''micro_enabled column not present, skip'' AS msg');
PREPARE stmt FROM @sql_micro;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 2. 补迁旧表 sensitive_word_library → sensitive_word
--    仅迁移 status='ENABLED' 且 word 非空的记录；
--    INSERT IGNORE 幂等（uk_tenant_word_category 唯一键保护，已存在的词不会重复插入）；
--    category 映射：POLITICAL/PORN/VIOLENCE/ABUSE 直传，其余归 CUSTOM；
--    tenant_id NULL → 0（平台级归全局底库），非 NULL 保留租户级；
--    source 依 tenant_id 判定：NULL→global，非NULL→tenant
--    ★IF EXISTS 守卫：旧表不存在则 no-op
-- ----------------------------
SET @has_old_table = (SELECT COUNT(*) FROM information_schema.TABLES
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sensitive_word_library');

SET @sql_migrate = IF(@has_old_table > 0,
'INSERT IGNORE INTO `sensitive_word` (`tenant_id`,`word`,`category`,`match_mode`,`industry_scope`,`status`,`source`,`create_by`,`create_time`,`remark`)
 SELECT IFNULL(l.tenant_id, 0),
        l.word,
        CASE WHEN UPPER(IFNULL(l.category,''CUSTOM'')) IN (''POLITICAL'',''PORN'',''VIOLENCE'',''ABUSE'')
             THEN UPPER(l.category) ELSE ''CUSTOM'' END,
        ''exact'',
        NULL,
        ''0'',
        IF(l.tenant_id IS NULL, ''global'', ''tenant''),
        ''migration_m31'',
        NOW(),
        CONCAT(''M31_01 补迁自 sensitive_word_library#'', l.id)
   FROM `sensitive_word_library` l
  WHERE l.status = ''ENABLED''
    AND l.word IS NOT NULL
    AND l.word <> ''''
    AND NOT EXISTS (
        SELECT 1 FROM `sensitive_word` sw
         WHERE sw.tenant_id = IFNULL(l.tenant_id, 0)
           AND sw.word = l.word
           AND sw.category = CASE WHEN UPPER(IFNULL(l.category,''CUSTOM'')) IN (''POLITICAL'',''PORN'',''VIOLENCE'',''ABUSE'')
                  THEN UPPER(l.category) ELSE ''CUSTOM'' END
    )',
'SELECT ''sensitive_word_library not present, skip migration'' AS msg');
PREPARE stmt FROM @sql_migrate;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 3. 补录 biz_bundle 业态包 feature_flags 确保含 FEAT_SENSITIVE_WORD
--    防止 applyBizFilter 第三维剔除导致 @RequireFeature 抛升级拦截
--    NOT JSON_CONTAINS 守卫，已包含的业态包不重复操作
-- ----------------------------
UPDATE `biz_bundle`
   SET `feature_flags` = JSON_ARRAY_APPEND(IFNULL(`feature_flags`, JSON_ARRAY()), '$', 'FEAT_SENSITIVE_WORD'),
       `update_time` = NOW()
 WHERE `feature_flags` IS NOT NULL
   AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_SENSITIVE_WORD"');

-- =====================================================================
-- 4. 验收查询（执行后人工核对）
-- =====================================================================
-- 4.1 功能开关已启用
-- SELECT code, status, basic_enabled, advanced_enabled, pro_enabled, micro_enabled
--   FROM feature_flag WHERE code = 'FEAT_SENSITIVE_WORD';
-- 预期：1 行；status='0'；各 enabled 列均=1
--
-- 4.2 旧表补迁完成（若旧表存在且有 ENABLED 词）
-- SELECT COUNT(*) FROM sensitive_word WHERE create_by = 'migration_m31';
-- 预期：≥0（旧表有新词时 > 0）
--
-- 4.3 业态包均含 FEAT_SENSITIVE_WORD
-- SELECT biz_code, JSON_CONTAINS(feature_flags, '"FEAT_SENSITIVE_WORD"') AS has_flag
--   FROM biz_bundle WHERE feature_flags IS NOT NULL;
-- 预期：每行 has_flag=1
--
-- 4.4 敏感词引擎可加载（重启后端后）
-- SELECT COUNT(*) FROM sensitive_word WHERE status = '0';
-- 预期：≥200（全局底库 + 租户自定义启用词总数）
-- =====================================================================
