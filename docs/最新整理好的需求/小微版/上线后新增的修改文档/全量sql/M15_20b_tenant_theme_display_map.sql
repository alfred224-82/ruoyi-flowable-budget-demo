-- =====================================================================
-- M15_20b 租户主题显示名映射 + 默认 Logo 扩展（T15.36 §五，v1.31 新增 / 对齐《多业态SaaS平台需求规格说明书》V1.0 §5.2）
-- 版本：v1.0  日期：2026-09-16
-- 依据：《资金管控系统开发任务文档（小微版）》v1.31 T15.36 §五「数据层改动」
--       《多业态SaaS平台需求规格说明书》V1.0 §5.2（海报与主题系统：主题营销名 + Logo 兜底）
-- 概述：在 M15_20（sys_tenant.app_theme_key）基础上追加两列，支撑：
--       ① theme_display_name_map —— theme_key → 展示名映射（营销名「暗夜桌游/暖光聚会/清新日系/霓虹赛博」
--          与代码名「经典蓝/暖阳橙/清新绿/雅致金」双口径，租户级二次自定义，theme_key 本身不变）；
--       ② default_logo_url —— 租户默认 Logo，活动 logo_url 为空时兜底（对齐 T15.35 §6.4 三层渲染兜底）。
-- 变更内容：
--   1. sys_tenant 补列：theme_display_name_map json、default_logo_url varchar(500)
--   2. 幂等：存储过程 + INFORMATION_SCHEMA 条件 ALTER
-- 前置依赖：M15_06（sys_tenant 表已存在）、M15_20（app_theme_key 已补）
-- 规范：
--   1. ★物理表为 sys_tenant（与 M15_20 一致）；任务文档中「tenant_config」为逻辑称谓，落库统一走 sys_tenant
--   2. ★不覆盖既有列/数据：仅追加列，不修改 M15_20 已建 app_theme_key（硬规定 v1.27 (1)）
--   3. theme_display_name_map 为 JSON，默认 NULL（NULL 时前端回退 industry_template.theme_display_column 行业默认口径）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. sys_tenant 追加 2 列（幂等条件 ALTER）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_20b_extend_sys_tenant`;
DELIMITER $$
CREATE PROCEDURE `m15_20b_extend_sys_tenant`()
BEGIN
    -- 1.1 theme_display_name_map（theme_key → 展示名映射，租户级二次自定义）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'theme_display_name_map') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `theme_display_name_map` JSON DEFAULT NULL
                COMMENT '主题显示名映射JSON(theme_key->展示名;营销名与代码名双口径,租户级自定义;NULL则回退行业模板默认,T15.36§五)' AFTER `app_theme_key`;
    END IF;
    -- 1.2 default_logo_url（租户默认 Logo，活动 logo_url 空时兜底）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant' AND COLUMN_NAME = 'default_logo_url') THEN
        ALTER TABLE `sys_tenant`
            ADD COLUMN `default_logo_url` VARCHAR(500) DEFAULT NULL
                COMMENT '租户默认Logo URL(活动logo_url为空时兜底显示,对齐T15.35§6.4三层渲染兜底)' AFTER `theme_display_name_map`;
    END IF;
END$$
DELIMITER ;
CALL m15_20b_extend_sys_tenant();
DROP PROCEDURE IF EXISTS `m15_20b_extend_sys_tenant`;

-- ----------------------------
-- 2. 验收查询（执行后人工核对）
-- ----------------------------
-- 2.1 sys_tenant 2 列已补
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant'
--    AND COLUMN_NAME IN ('theme_display_name_map','default_logo_url');
-- 预期：2 行（theme_display_name_map json, default_logo_url varchar(500)）
--
-- 2.2 ★既有列未被误删（M15_20 app_theme_key + M15_10 version_type 仍在）
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_tenant'
--    AND COLUMN_NAME IN ('app_theme_key','version_type');
-- 预期：2
--
-- 2.3 ★运行时验证（T15.36 §五/§六 部署后）：
--   ① platform_admin 选主题 → 写 app_theme_key（M15_20）
--   ② 若租户需以营销名展示（如桌游店 theme_default 显示「暗夜桌游」）→ 写 theme_display_name_map
--   ③ /page/config 下发 app_theme_key + theme_display_name_map → 小程序按映射渲染主题名
--   ④ 活动 logo_url 为空 → 前端回退 default_logo_url；两者皆空 → 主题色占位（T15.35 §6.4）
