-- =====================================================================
-- M39_01  三张报表菜单：报表中心 + 活动明细/日报/对账（T39.1~T39.5）
-- =====================================================================
-- 需求来源：《小账本H5界面开发任务文档_v1.md》§九 M39 三张报表
--          《小账本H5界面设计方案_v3》§21 三张报表
--
-- 交付范围：
--   菜单：7390~7393（小程序·老板端「报表中心」C-mini + 活动明细/日报/对账 F）
--         perms：micro:report:activityDetail（活动明细表）/ micro:report:daily（经营日报）
--                / micro:report:reconcile（三张报表对账校验 T39.5）
--
-- 说明：
--   ① 三张报表同源 activity_settlement.profit，天然一致；对账接口做校验返回（T39.5）；
--   ② 报表中心 C 级菜单以活动明细为默认落地页签，故 C 与 F(7391) 同用 micro:report:activityDetail；
--   ③ 月报相关权限（micro:report:operating/closing）在 [154]M38_01 交付，本脚本不重复。
--
-- 权限：仅老板 micro_pro（lite/员工不授权，§21 验收4）
-- 幂等：先删后插菜单 + JSON_CONTAINS 守卫追加 + INSERT IGNORE 补录
-- 前置：[45]M15_06（小程序菜单 7000~7499 + 老板小程序根 7200）
--       + [152]M36_01（activity_settlement 结算数据源）+ [154]M38_01（月报/月结）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 菜单：M39 报表中心（7390~7393，小程序·老板端 mini）
--    父节点 7200（小程序·老板，M15_06 [45]），与 7201 经营报表、7380 月度经营报表同级
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7390 AND 7394;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7390 AND 7394;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7390, '报表中心',   7200, 5, 'reportCenter', 'miniBoss/reportCenter', 1, 0, 'C', '0', '0', 'micro:report:activityDetail', 'mini', '#', 'admin', NOW(), 'M39 T39.4 三张报表中心（默认活动明细页签，仅 micro_pro）'),
(7391, '活动明细表', 7390, 1, '',               NULL,                    1, 0, 'F', '0', '0', 'micro:report:activityDetail', 'mini', '#', 'admin', NOW(), 'M39 T39.1 一行一场 + 底部固定合计 + 差额着色'),
(7392, '经营日报',   7390, 2, '',               NULL,                    1, 0, 'F', '0', '0', 'micro:report:daily',          'mini', '#', 'admin', NOW(), 'M39 T39.2 当日汇总 + 本月保本进度'),
(7393, '对账校验',   7390, 3, '',               NULL,                    1, 0, 'F', '0', '0', 'micro:report:reconcile',      'mini', '#', 'admin', NOW(), 'M39 T39.5 日报累计=明细合计=月报毛利');

-- =====================================================================
-- 2. role_template：boss（仅 micro_pro）追加 M39 报表权限键
--    ★lite 不授权（§21 验收4：三张报表仅老板 micro_pro）
-- =====================================================================
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:activityDetail')
WHERE `role_code` = 'boss' AND `version_type` = 'micro_pro' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:activityDetail"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:daily')
WHERE `role_code` = 'boss' AND `version_type` = 'micro_pro' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:daily"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:report:reconcile')
WHERE `role_code` = 'boss' AND `version_type` = 'micro_pro' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:report:reconcile"');

-- =====================================================================
-- 3. 存量 sys_role_menu 补录（仅 micro_pro 老板克隆角色）
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND m.`menu_id` BETWEEN 7390 AND 7393;

-- 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` BETWEEN 7390 AND 7393;

-- =====================================================================
-- 4. 验收查询（执行后人工核对）
-- =====================================================================
-- 4.1 菜单 7390~7393 就绪
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu
--  WHERE menu_id BETWEEN 7390 AND 7393 ORDER BY menu_id;
-- 预期：4 行（7390 C-mini / 7391~7393 F，均 mini）
--
-- 4.2 role_template boss micro_pro 含三张报表权限、micro_lite 不含
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:report:activityDetail"') AS has_detail,
--        JSON_CONTAINS(menu_keys,'"micro:report:daily"')          AS has_daily,
--        JSON_CONTAINS(menu_keys,'"micro:report:reconcile"')      AS has_recon
--   FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：micro_pro=1/1/1；micro_lite=0/0/0
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
