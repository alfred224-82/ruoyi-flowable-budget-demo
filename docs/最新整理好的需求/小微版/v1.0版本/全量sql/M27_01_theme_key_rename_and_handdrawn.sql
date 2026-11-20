-- =====================================================================
-- M27_01  主题命名对齐 V3.9 + 存量数据迁移（T27.1）
-- 版本：v1.0  日期：2026-10-13
-- 依据：《小微版 V3.9 开发文档》M27 主题风格与背景图联动 T27.1 / §2.2 / §2.3
-- 归属版本：小微版（MICRO，micro_lite + micro_pro 通用）
-- 前置依赖：M15_20（sys_tenant.app_theme_key 补列，默认 theme_default）、
--           M12_02（biz_theme_preset 系统级共享表已建）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. sys_tenant.app_theme_key 存量值迁移（旧 4 值 → V3.9 新 4 值，幂等）：
--        theme_default(经典蓝) → theme_fresh(清新简约风)
--        theme_warm(暖阳橙)    → theme_vivid(活力渐变风)
--        theme_elegant(典雅紫) → theme_dark(暗黑科技风)
--        theme_fresh(旧清新绿) → 保持 theme_fresh（key 复用，语义由「清新绿」变为「清新简约风」）
--        NULL / 空串           → theme_fresh（新默认主题）
--      ★theme_handdrawn(温暖手绘风) 为 V3.9 新增主题，无存量来源，由 platform_admin 选定后写入
--   2. sys_tenant.app_theme_key 列默认值 theme_default → theme_fresh + 注释更新（INFORMATION_SCHEMA 守卫）
--   3. biz_theme_preset 补 theme_handdrawn Design Token 记录（幂等 ON DUPLICATE KEY UPDATE）
-- ---------------------------------------------------------------------
-- 代码联动（同批交付，无需 SQL）：
--   - BE MicroPlatformThemeController.PRESET_THEMES 常量：4 套主题 key/name/色板对齐 §2.3
--   - MP ruoyi-app/src/utils/theme.js PRESET_THEMES + DEFAULT_THEME 对齐
--   - PC ruoyi-ui/src/views/micro/platform/theme/index.vue 默认 key theme_fresh
--   - 实体注释 SysTenant/SysTenantVo/PromotionActivity(Bo) 值域同步
-- ---------------------------------------------------------------------
-- 规范：
--   1. SET NAMES utf8mb4 开头；UPDATE 用 SQL_SAFE_UPDATES 包裹
--   2. ALTER 使用 INFORMATION_SCHEMA 条件判断（PREPARE 动态执行），幂等可重复
--   3. biz_theme_preset 补录使用 ON DUPLICATE KEY UPDATE，非破坏性幂等
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. sys_tenant.app_theme_key 存量值迁移（幂等；旧值不存在时 UPDATE 影响 0 行，无副作用）
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE `sys_tenant` SET `app_theme_key` = 'theme_fresh' WHERE `app_theme_key` = 'theme_default';
UPDATE `sys_tenant` SET `app_theme_key` = 'theme_vivid'  WHERE `app_theme_key` = 'theme_warm';
UPDATE `sys_tenant` SET `app_theme_key` = 'theme_dark'   WHERE `app_theme_key` = 'theme_elegant';
-- NULL / 空串回落到新默认主题（MySQL 列 DEFAULT 仅对新插入行生效，存量 NULL 需显式回填）
UPDATE `sys_tenant` SET `app_theme_key` = 'theme_fresh'  WHERE `app_theme_key` IS NULL OR `app_theme_key` = '';

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 2. app_theme_key 列默认值 theme_default → theme_fresh + 注释更新（INFORMATION_SCHEMA 守卫，幂等）
-- ----------------------------
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key');
SET @sql = IF(@col_exists > 0,
    'ALTER TABLE `sys_tenant` MODIFY COLUMN `app_theme_key` varchar(32) DEFAULT ''theme_fresh'' COMMENT ''小程序全局主题key（M27 T27.1 命名对齐 V3.9：theme_fresh清新简约风/theme_vivid活力渐变风/theme_dark暗黑科技风/theme_handdrawn温暖手绘风）''',
    'SELECT ''app_theme_key not exists, skip modify''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ----------------------------
-- 3. biz_theme_preset 补 theme_handdrawn Design Token 记录（V3.9 §四第 4 套主题，幂等）
--    ★M12_02 已建表并内置 dark_boardgame/warm_party/fresh_japan/neon_cyber 四套 Design Token；
--      本脚本追加与 app_theme_key 命名对齐的 theme_handdrawn（温暖手绘风），供图片渲染管线识别新主题。
-- ----------------------------
INSERT INTO `biz_theme_preset`
  (`preset_key`, `name`, `tokens`, `contrast_ratio`, `sort`, `status`, `create_by`, `remark`) VALUES
('theme_handdrawn', '温暖手绘风',
 JSON_OBJECT('--primary', '#F4A261', '--secondary', '#FFE8CC', '--bg', '#FFFBF2', '--text', '#3D3D3D', '--radius', '12px', '--shadow', '0 2px 8px rgba(244,162,97,0.2)'),
 'AA', 5, '0', 'admin', 'M27 T27.1 温暖手绘风（V3.9 §四第 4 套主题，暖色底 + 手绘插画）')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `tokens` = VALUES(`tokens`),
  `contrast_ratio` = VALUES(`contrast_ratio`),
  `sort` = VALUES(`sort`),
  `status` = '0',
  `update_by` = 'admin',
  `remark` = VALUES(`remark`);

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 无残留旧值（预期：0 行）
-- SELECT id, company_name, app_theme_key FROM sys_tenant
--  WHERE app_theme_key IN ('theme_default', 'theme_warm', 'theme_elegant');
--
-- 4.2 值域分布（预期仅含 theme_fresh/theme_vivid/theme_dark/theme_handdrawn/NULL 之外的旧值已清空）
-- SELECT app_theme_key, COUNT(*) AS cnt FROM sys_tenant GROUP BY app_theme_key ORDER BY cnt DESC;
--
-- 4.3 列默认值已改为 theme_fresh
-- SELECT COLUMN_NAME, COLUMN_DEFAULT, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'app_theme_key';
-- 预期：COLUMN_DEFAULT = theme_fresh
--
-- 4.4 biz_theme_preset 含 theme_handdrawn（预期 1 行）
-- SELECT preset_key, name, contrast_ratio, sort FROM biz_theme_preset WHERE preset_key = 'theme_handdrawn';
--
-- ★执行后请重启后端 + 小程序端清除缓存重新冷启动，读取最新主题色板
