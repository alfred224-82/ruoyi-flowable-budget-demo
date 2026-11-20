-- =====================================================================
-- M22_03  员工排班·按钮权限与功能开关兜底修复（T22.3/T22.6 补漏）
-- 版本：v1.0  日期：2026-10-10
-- 依据：《资金管控系统开发任务文档（小微版）》M22 里程碑
-- ---------------------------------------------------------------------
-- 现象：
--   ① GET /micro/staffSchedule/* 报「小微版无此功能权限」
--      → 代码侧根因已修：EditionFeatureConfig.MICRO_FEATURES 补 FEAT_STAFF_SCHEDULE；
--        本脚本兜底确保 feature_flag / biz_bundle 两维数据齐备（三维交集另两维）。
--   ② Boss PC 排班页只有「刷新/修正日志」，缺「批量创建/发布/班次时间设定」按钮
--      → 根因：boss 角色 sys_role_menu 缺 7231(创建)/7232(发布)/7233(修正) F 按钮，
--        前端 v-hasPermi 不通过故隐藏；本脚本幂等补录。
-- 前置依赖：[93] M22_01_staff_schedule.sql（菜单 7230~7235 已建）
-- 规范：先删后插/INSERT IGNORE 幂等；UPDATE 包裹 SQL_SAFE_UPDATES；可重复执行
-- ★执行后请重启后端（清 FeatureFlag/BizBundle/License 缓存）并退出重新登录刷新权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. feature_flag 兜底：确保 FEAT_STAFF_SCHEDULE 存在且 micro_enabled=1
--    （三维交集第②维：featureFlagService.getEditionFeatureSet(MICRO)）
-- ----------------------------
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`)
SELECT 'FEAT_STAFF_SCHEDULE', '员工排班管理(周/月+早晚班+Boss 自定时间+发布后可修正)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
       'T22.6 兜底补录；仅小微版启用'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `feature_flag` WHERE `code` = 'FEAT_STAFF_SCHEDULE');

SET SQL_SAFE_UPDATES = 0;
UPDATE `feature_flag`
   SET `micro_enabled` = 1, `status` = '0'
 WHERE `code` = 'FEAT_STAFF_SCHEDULE';

-- ----------------------------
-- 2. biz_bundle 兜底：全部业态包 feature_flags 幂等补录 FEAT_STAFF_SCHEDULE
--    （三维交集第③维：applyBizFilter）
-- ----------------------------
UPDATE `biz_bundle`
   SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_STAFF_SCHEDULE')
 WHERE `feature_flags` IS NOT NULL
   AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_STAFF_SCHEDULE"');
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. ★按钮权限补录：micro_pro / micro 档 boss 补 7230~7233 + 祖先 7100
--    （前端 v-hasPermi 依赖 sys_role_menu；INSERT IGNORE 幂等）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
  FROM `sys_role` r
  CROSS JOIN `sys_menu` m
  JOIN `sys_tenant` t ON r.`tenant_id` = t.`id`
 WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
   AND t.`tenant_type` = 'micro'
   AND t.`version_type` IN ('micro_pro', 'micro')
   AND m.`menu_id` IN (7230, 7231, 7232, 7233, 7100);

-- 3.1 员工小程序「我的排班」7234 + 祖先 7300
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
  FROM `sys_role` r
  CROSS JOIN `sys_menu` m
  JOIN `sys_tenant` t ON r.`tenant_id` = t.`id`
 WHERE r.`role_key` = 'salesman' AND r.`del_flag` = '0'
   AND t.`tenant_type` = 'micro'
   AND t.`version_type` IN ('micro_pro', 'micro')
   AND m.`menu_id` IN (7234, 7300);

-- 3.2 顾客小程序「门店排班」7235 + 祖先 7400
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
  FROM `sys_role` r
  CROSS JOIN `sys_menu` m
  JOIN `sys_tenant` t ON r.`tenant_id` = t.`id`
 WHERE r.`role_key` = 'customer' AND r.`del_flag` = '0'
   AND t.`tenant_type` = 'micro'
   AND t.`version_type` IN ('micro_pro', 'micro')
   AND m.`menu_id` IN (7235, 7400);

-- 3.3 超管 role_id=1 全量补录（平台侧调试）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7230, 7231, 7232, 7233, 7234, 7235);

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 功能开关齐备
-- SELECT code, micro_enabled, status FROM feature_flag WHERE code = 'FEAT_STAFF_SCHEDULE';
-- 预期：1 行，micro_enabled=1 / status=0
-- 4.2 boss 已含 4 个排班菜单（含 3 个 F 按钮）
-- SELECT r.role_id, t.version_type, COUNT(DISTINCT rm.menu_id) AS cnt
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_tenant t ON t.id = r.tenant_id
--  WHERE r.role_key = 'boss' AND rm.menu_id IN (7230,7231,7232,7233)
--  GROUP BY r.role_id, t.version_type;
-- 预期：每个 micro_pro/micro 版 boss cnt=4
-- ★执行后重启后端 + 退出重新登录，排班页即显示「批量创建/发布本期/班次时间设定」按钮
