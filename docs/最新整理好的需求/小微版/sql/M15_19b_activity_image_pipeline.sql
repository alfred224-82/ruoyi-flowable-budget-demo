-- =====================================================================
-- M15_19b 活动图片管线主色落库扩展（T15.35 §六，v1.31 新增 / 对齐《多业态SaaS平台需求规格说明书》V1.0 §5.2/§8）
-- 版本：v2.0  日期：2026-09-16
-- 依据：《资金管控系统开发任务文档（小微版）》v1.31 T15.35 §六「图片校验与渲染管线」
--       《多业态SaaS平台需求规格说明书》V1.0 §5.2（海报与主题系统）/ §8（非功能性需求：图片四重校验）
-- ---------------------------------------------------------------------
-- ★架构对齐说明（v2.0 重写，去冗余）：
--   经代码库核查，图片校验与渲染管线的基础设施在 T12.x（企业版/多业态线）已完整建成，
--   小微版 T15.35 §六 应【复用既有扩展点】而非新建平行表，故本脚本从 v1.0 的
--   「promotion_activity 补 6 列 + 新建 img_sec_check_log」收敛为「tenant_theme 补主色/辅色 2 列」：
--     ① imgSecCheck 合规审计 → 复用既有 biz_content_security_log（provider=wechat_imgSecCheck），
--        由 IContentSecurityService.checkImage() 统一留痕；新增 @Primary WechatImgSecCheckSecurity
--        真调 wxa/img_sec_check（覆盖 ContentSecurityBlacklistChecker 桩）。★不再新建 img_sec_check_log。
--     ② 衍生图任务/成品 URL → 复用既有 biz_image_task（status/result_url/retry_count）
--        + tenant_theme.logo_rendered_url / rendered_poster_url（M1_05 已建，按 activity_id 关联活动）；
--        新增 @Primary OssRenderImagePipeline 真实渲染（覆盖 ImagePipelineStub 桩）。★promotion_activity 不再补渲染列。
--     ③ 唯一新增数据需求：k-means 主色/辅色 hex 落库，供小程序 activity/browse.vue 卡片配色直接消费
--        → tenant_theme 补 poster_dominant_color / poster_accent_color 两列（本脚本）。
--   对应实体：TenantTheme / TenantThemeVo 已补 posterDominantColor / posterAccentColor 字段。
-- 变更内容：
--   1. tenant_theme 补列：poster_dominant_color varchar(16)、poster_accent_color varchar(16)
--   2. 幂等：存储过程 + INFORMATION_SCHEMA 条件 ALTER
-- 前置依赖：M1_05（tenant_theme 表已建立，含 logo_rendered_url/rendered_poster_url）
-- 规范：
--   1. ★不覆盖既有列/数据：仅追加列（硬规定 v1.27 (1)）
--   2. ★复用优先：imgSecCheck 走 biz_content_security_log，管线走 biz_image_task，不新建平行表
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. tenant_theme 追加 2 列（幂等条件 ALTER）
-- ----------------------------
DROP PROCEDURE IF EXISTS `m15_19b_extend_tenant_theme`;
DELIMITER $$
CREATE PROCEDURE `m15_19b_extend_tenant_theme`()
BEGIN
    -- 1.1 poster_dominant_color（海报主色 hex，k-means k=3 提取）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_theme' AND COLUMN_NAME = 'poster_dominant_color') THEN
        ALTER TABLE `tenant_theme`
            ADD COLUMN `poster_dominant_color` VARCHAR(16) DEFAULT NULL
                COMMENT '海报主色hex(如#2B6CFF;k-means k=3提取,推导卡片配色,T15.35§6.3)' AFTER `rendered_poster_url`;
    END IF;
    -- 1.2 poster_accent_color（海报辅色 hex）
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_theme' AND COLUMN_NAME = 'poster_accent_color') THEN
        ALTER TABLE `tenant_theme`
            ADD COLUMN `poster_accent_color` VARCHAR(16) DEFAULT NULL
                COMMENT '海报辅色hex(k-means k=3提取,内容卡片强调色,T15.35§6.3)' AFTER `poster_dominant_color`;
    END IF;
END$$
DELIMITER ;
CALL m15_19b_extend_tenant_theme();
DROP PROCEDURE IF EXISTS `m15_19b_extend_tenant_theme`;

-- ----------------------------
-- 2. 验收查询（执行后人工核对）
-- ----------------------------
-- 2.1 tenant_theme 主色/辅色 2 列已补
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
--   FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_theme'
--    AND COLUMN_NAME IN ('poster_dominant_color','poster_accent_color');
-- 预期：2 行（poster_dominant_color varchar(16), poster_accent_color varchar(16)）
--
-- 2.2 ★既有列未被误删（M1_05 logo_rendered_url/rendered_poster_url 仍在）
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tenant_theme'
--    AND COLUMN_NAME IN ('logo_rendered_url','rendered_poster_url');
-- 预期：2
--
-- 2.3 ★复用的既有表就绪（无需本脚本创建，仅核对存在）
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('biz_content_security_log','biz_image_task');  -- 预期：2
--
-- 2.4 ★运行时管线验证（T15.35 §六 部署后）：
--   ① boss 上传 logo/海报 → ThemeDecorationServiceImpl.decorate() 先经 IContentSecurityService.checkImage()
--      （@Primary WechatImgSecCheckSecurity 真调 wxa/img_sec_check）→ 留痕 biz_content_security_log(result=PASS/REJECT)
--   ② 审核通过 → 落 OSS 原始 URL → 触发 IImagePipeline.submit()（@Primary OssRenderImagePipeline）
--   ③ 管线渲染：Logo 80x80 加主题主色衬底 / 海报 cover 填充 + 顶部渐变遮罩 + k-means 主色提取
--   ④ 成品 PNG 上传 OSS → 回写 biz_image_task.result_url + tenant_theme.logo_rendered_url/rendered_poster_url
--      + poster_dominant_color/poster_accent_color
--   ⑤ imgSecCheck 命中违规(87014) → biz_content_security_log.result=REJECT → decorate 抛异常拦截，不落库不发布
--   ⑥ 微信侧基础设施异常（未配置 appid/网络/token 失效）→ 按 content.security.img-check-fail-open 策略降级（默认放行）
