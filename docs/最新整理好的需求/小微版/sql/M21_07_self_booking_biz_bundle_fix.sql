-- =====================================================================
-- M21_07  FEAT_SELF_BOOKING 业态包能力开关补全（修复「小微版无此功能权限」，v1.77 新增）
-- 版本：v1.0  日期：2026-10-10
-- 依据：用户反馈「助教状态管理 / 房间状态管理 / 资源价格设置 三个菜单在小微版付费版
--       （micro_pro，lijie boss）报『小微版无此功能权限』」
-- ---------------------------------------------------------------------
-- 根因（与 M20_05 修复 FEAT_RATING 升级拦截同源）：
--   LicenseServiceImpl.getTenantActiveFeatures() 三维交集 = License ∩ feature_flag ∩ biz_bundle。
--   ① License 维：parseFeatures(MICRO) = MICRO_FEATURES ∪ 库存，含 FEAT_SELF_BOOKING，正常；
--   ② feature_flag 维：M21_01 已落第 38 项 FEAT_SELF_BOOKING（组 E，micro_enabled=1），正常；
--   ③ biz_bundle 维：★三个业态包(boardgame/fresh_food/fund_control)的 feature_flags 仅在 M20_05
--      补齐过 FEAT_PROMOTION_DISTRIBUTION/FEAT_RATING/FEAT_SENSITIVE_WORD/FEAT_IMAGE_COMPLIANCE 四个
--      小微开关，M21_01 新增 FEAT_SELF_BOOKING 时未同步补录 → applyBizFilter 第三维剔除 →
--      FeatureInterceptor.checkFeature 在 MICRO 分支抛「小微版无此功能权限」。
--   受影响接口（类级 @RequireFeature("FEAT_SELF_BOOKING")）：
--     GET /micro/resourceStatus/staff/list（助教状态管理 MicroResourceStatusController）
--     GET /micro/resourceStatus/room/list （房间状态管理 MicroResourceStatusController）
--     GET /micro/resourcePrice/staff/list （资源价格设置 MicroResourcePriceController）
--     以及 /micro/resourceStatus/release|log/list、/micro/resourcePrice/*、/micro/selfBooking/*
--   ★后端实现齐备（MicroResourceStatusController/MicroResourcePriceController +
--     MicroSelfBookingServiceImpl/StaffPricingServiceImpl + PC 3 Vue 页面），无代码缺口，
--     唯一缺口 = biz_bundle.feature_flags 数据。
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. feature_flag FEAT_SELF_BOOKING 落表兜底（先删后插幂等，micro_enabled=1，防 M21_01 漏执行）
--   2. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_SELF_BOOKING（根因修复）
--   3. 防御性重补 M20_05 四个小微开关（覆盖 M20_05 之后新建的自定义业态包）
-- 规范：
--   1. 先删后插幂等 + NOT JSON_CONTAINS 守卫，重复执行不报错
--   2. UPDATE 包裹 SQL_SAFE_UPDATES 开关
--   3. ★执行后请重启后端，清除内存缓存（BizBundleServiceImpl.flagCache /
--      FeatureFlagServiceImpl.flagsCache / LicenseServiceImpl.tenantFeaturesCache），
--      否则三维交集仍按旧缓存计算
-- =====================================================================

SET NAMES utf8mb4;

-- ─── 1. feature_flag FEAT_SELF_BOOKING 落表兜底（第 38 项，组 E=小微版专属）────
-- 先删后插幂等（与 M21_01 同口径）；micro_enabled=1，basic/advanced/pro 三列均 0
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_SELF_BOOKING';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_SELF_BOOKING', '自助预约(项目/房间/助教三类+提前一天限制)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T21.4；★仅小微版启用(tenant_type=micro/version_type∈micro_lite,micro_pro)；M21_07 兜底重补');

-- ─── 2. 【根因】biz_bundle 业态包补齐 FEAT_SELF_BOOKING ────────────────────
-- 不限定 biz_code：覆盖三个标准业态包 + 后续新建的自定义业态包；
-- NOT JSON_CONTAINS 保证幂等（已存在则跳过）；feature_flags 为 NULL 的包
-- applyBizFilter 本就跳过过滤，无需处理
SET SQL_SAFE_UPDATES = 0;

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_SELF_BOOKING')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_SELF_BOOKING"');

-- ─── 3. 防御性重补 M20_05 四个小微开关（幂等，覆盖新建自定义业态包）──────────
UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_PROMOTION_DISTRIBUTION')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_PROMOTION_DISTRIBUTION"');

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_RATING')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_RATING"');

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_SENSITIVE_WORD')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_SENSITIVE_WORD"');

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_IMAGE_COMPLIANCE')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_IMAGE_COMPLIANCE"');

SET SQL_SAFE_UPDATES = 1;

-- ─── 4. 验收查询（执行后人工核对）─────────────────────────────────────────
-- 4.1 FEAT_SELF_BOOKING 落表且 micro_enabled=1
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_SELF_BOOKING';
-- 预期：1 行，E / micro_enabled=1 / basic_enabled=0 / status=0
--
-- 4.2 每个业态包均含 5 个小微开关（预期每行 5 个 1）
-- SELECT biz_code,
--   JSON_CONTAINS(feature_flags,'"FEAT_SELF_BOOKING"')           AS f_self_booking,
--   JSON_CONTAINS(feature_flags,'"FEAT_PROMOTION_DISTRIBUTION"') AS f_promo,
--   JSON_CONTAINS(feature_flags,'"FEAT_RATING"')                 AS f_rating,
--   JSON_CONTAINS(feature_flags,'"FEAT_SENSITIVE_WORD"')         AS f_word,
--   JSON_CONTAINS(feature_flags,'"FEAT_IMAGE_COMPLIANCE"')       AS f_image
--   FROM biz_bundle;
-- 预期：所有 feature_flags 非 NULL 的业态包 5 列全 1
--
-- 4.3 功能开关总数不变（38 项）
-- SELECT COUNT(*) FROM feature_flag;  -- 预期 38
--
-- 4.4 端到端：lijie（micro_pro boss）重新登录后访问
--     GET /micro/resourceStatus/staff/list、/micro/resourceStatus/room/list、
--     GET /micro/resourcePrice/staff/list 均返回 200 数据，不再抛「小微版无此功能权限」
