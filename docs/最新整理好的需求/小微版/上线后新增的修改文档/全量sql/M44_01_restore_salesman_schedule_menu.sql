-- =====================================================================
-- M44_01_restore_salesman_schedule_menu.sql
-- 修复：业务员(salesman) H5 首页「我参与的活动」空白 + /micro/staff/schedule
--       与 /micro/staff/myActivities 报 403「无此权限：micro:staff:schedule」
-- =====================================================================
-- 根因：
--   1. micro:staff:schedule / micro:staff:scheduleDetail 原对应菜单
--      7301（我的排班,C,mini）/ 7302（排班详情,F,mini），根目录 7300（小程序·业务员）。
--   2. M31_05（Boss 海报生成 + 游客信息管理）复用了 menu_id 7300~7302，
--      将其覆盖为 7300 生成活动海报(F,pc) / 7301 游客信息管理(C,pc) / 7302 补全游客信息(F,pc)。
--   3. 覆盖后 sys_menu 中不再存在 perms='micro:staff:schedule' 的行；
--      role_template(micro/micro_pro salesman) 仍保留该 perm，但
--      PROD_fix_all_role_menus.sql 按 perms 匹配 sys_menu 重建授权时匹配不到，
--      导致业务员克隆角色始终缺该权限。
--   4. MicroStaffController#schedule / #myActivities 均 @SaCheckPermission("micro:staff:schedule")
--      → 双双 403；staffHome/index.vue loadSchedule()/loadMyActivities() 静默降级为空
--      → 首页「我参与的活动（未结束的）」不显示。
-- 修复策略：
--   ★不触碰已被复用的 7300~7302（属 Boss PC 菜单），改用新的空闲 menu_id 7236/7237
--     重建两个 client_type=mini 的业务员排班菜单，parent 沿用 7300（与 7310/7320/7234 等
--     现存业务员 mini 菜单一致；mini 权限解析不依赖祖先目录，故无需授权 7300）。
--   ★授权范围：micro/micro_pro 租户克隆 salesman + 共享预设 salesman(tenant_id=NULL)；
--     micro_lite 免费版不授权（其 role_template 仅含 micro:activity:browse）。
-- 规范：
--   1. 先删后插幂等（仅删本脚本自身创建的 7236/7237）
--   2. 授权用 INSERT IGNORE，可重复执行
--   3. ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- ---------------------------------------------------------------------
-- 1. 重建业务员排班 mini 菜单（新 ID 7236/7237，避开被复用的 7301/7302）
--    列顺序与 M24_01 一致：
--    menu_id, menu_name, parent_id, order_num, path, component,
--    is_frame, is_cache, menu_type, visible, status, perms, client_type,
--    icon, create_by, create_time, remark
-- ---------------------------------------------------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7236, 7237);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7236: 我的排班（C 页面，业务员小程序）——恢复被 7301 覆盖丢失的 micro:staff:schedule
(7236, '我的排班', 7300, 1, 'schedule', 'miniStaff/schedule', 1, 0, 'C', '0', '0', 'micro:staff:schedule', 'mini', '#', 'admin', NOW(), 'M44 恢复：原 7301 被 Boss PC 菜单复用覆盖，改用 7236；/micro/staff/schedule + /micro/staff/myActivities 权限载体'),
-- 7237: 排班详情（F 按钮，业务员小程序）——恢复被 7302 覆盖丢失的 micro:staff:scheduleDetail
(7237, '排班详情', 7236, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:staff:scheduleDetail', 'mini', '#', 'admin', NOW(), 'M44 恢复：原 7302 被 Boss PC 菜单复用覆盖，改用 7237；/micro/staff/schedule/{id} 权限载体');

-- ---------------------------------------------------------------------
-- 2. 授权给业务员角色（micro/micro_pro 租户克隆 + 共享预设；★不含 micro_lite）
--    ★仅授 7236/7237，不授 7300（7300 现为 Boss PC 的 micro:promo:edit，误授会越权）
-- ---------------------------------------------------------------------
-- 2.1 租户克隆 salesman（tenant_id ≠ NULL，version_type ∈ micro/micro_pro）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
CROSS JOIN `sys_menu` m
WHERE r.`role_key` = 'salesman' AND r.`del_flag` = '0'
  AND t.`version_type` IN ('micro_pro', 'micro')
  AND m.`menu_id` IN (7236, 7237);

-- 2.2 共享预设 salesman（tenant_id = NULL，存量用户 sys_user_role 仍指向它）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.`role_key` = 'salesman' AND r.`del_flag` = '0' AND r.`tenant_id` IS NULL
  AND m.`menu_id` IN (7236, 7237);

-- 2.3 超管 role_id=1 补授（便于平台侧调试）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7236, 7237);

SET SQL_SAFE_UPDATES = 1;

-- ---------------------------------------------------------------------
-- 3. 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 3.1 两个菜单已建且 perms 正确
-- SELECT menu_id, menu_name, perms, menu_type, client_type, parent_id FROM sys_menu WHERE menu_id IN (7236,7237);
-- 预期：2 行，micro:staff:schedule(C,mini) / micro:staff:scheduleDetail(F,mini)
--
-- 3.2 杰杰旅游社(2109653805417828354) salesman 克隆角色已含新菜单
-- SELECT rm.menu_id, m.perms FROM sys_role_menu rm JOIN sys_menu m ON m.menu_id=rm.menu_id
--  WHERE rm.role_id=7109653805417828359 AND rm.menu_id IN (7236,7237);
-- 预期：2 行
--
-- 3.3 业务员重新登录后 /getInfo 的 permissions 应含 micro:staff:schedule / micro:staff:scheduleDetail
-- 3.4 /micro/staff/schedule 与 /micro/staff/myActivities 不再报 403，H5 首页「我参与的活动」正常显示
