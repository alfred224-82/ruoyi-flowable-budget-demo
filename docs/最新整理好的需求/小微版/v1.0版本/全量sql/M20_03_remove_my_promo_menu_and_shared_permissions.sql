-- =====================================================================
-- M20_03  移除「我的推广码」菜单 + 公用功能去除权限校验（v1.61，2026-09-26）
-- =====================================================================
-- 依据：用户新增需求——① 「我的推广码」菜单不需要，移除；
--       ② 免费版和付费版公用的功能去除权限码校验，避免反复 403。
--
-- 修复内容：
--   1. 删除菜单 7159（我的推广码）及 sys_role_menu 关联
--   2. role_template 各版本 boss 移除 micro:myPromo:view
--   3. 后端公用功能去除 @SaCheckPermission（Java 代码层面）：
--      - MicroActivitySettingController：全部端点（微信推广码/项目/房间/员工选择器等）
--      - MicroRewardConfigController：拉新奖励配置 GET/PUT
--      - MicroPromotionCodeController：generate/my 端点移除 micro:myPromo:view OR 逻辑
--
-- 前置依赖：
--   [71]M20_01（micro_pro boss 菜单对齐，含 7159 重建）
--
-- 幂等：DELETE WHERE + JSON_REMOVE + 条件判断，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 删除菜单 7159（我的推广码）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7159;
DELETE FROM `sys_menu` WHERE `menu_id` = 7159;

-- =====================================================================
-- 2. role_template 各版本 boss 移除 micro:myPromo:view
-- =====================================================================

-- 2.1 micro / micro_pro boss
UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'micro:myPromo:view')))
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND JSON_CONTAINS(`menu_keys`, '"micro:myPromo:view"');

-- 2.2 micro_lite boss
UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'micro:myPromo:view')))
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND JSON_CONTAINS(`menu_keys`, '"micro:myPromo:view"');

-- =====================================================================
-- 3. 后端公用功能去除权限校验（Java 代码层面，此处仅做登记说明）
--
-- 3.1 MicroActivitySettingController（/micro/activity/setting/*）
--     移除全部 @SaCheckPermission（微信推广码 + 项目/房间/员工选择器 + 标题预览 + 语音开关 + 通用小程序码）
--     ★免费版和付费版老板均可访问活动发布页与微信推广码页
--
-- 3.2 MicroRewardConfigController（/micro/rewardConfig）
--     移除 GET/PUT @SaCheckPermission("micro:distribution:rewardConfig")
--     ★拉新奖励配置为老板公用功能（唯一配置入口），免费版+付费版均可见
--
-- 3.3 MicroPromotionCodeController（/micro/promo/code）
--     generate/my 端点：移除 SaMode.OR(micro:distribution:*, micro:myPromo:view)，
--     仅保留 @RequireFeature("FEAT_PROMOTION_DISTRIBUTION") 功能开关校验
--     ★list/toggle 端点保留 micro:distribution:* 权限（仅付费版完整推广管理）
--
-- =====================================================================

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1. 菜单 7159 已删除
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id = 7159;
-- 预期：0
--
-- 2. role_template 不含 micro:myPromo:view
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"micro:myPromo:view"') AS has_my_promo
--   FROM role_template
--  WHERE role_code = 'boss' AND version_type IN ('micro', 'micro_pro', 'micro_lite');
-- 预期：3 行，has_my_promo 均为 0
--
-- 3. sys_role_menu 无 7159 关联
-- SELECT COUNT(*) FROM sys_role_menu WHERE menu_id = 7159;
-- 预期：0
