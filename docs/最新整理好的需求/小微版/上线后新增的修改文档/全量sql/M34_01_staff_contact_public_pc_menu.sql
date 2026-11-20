-- =====================================================================
-- M34_01  T34.5 员工联系方式公开开关：老板 PC 端配置页菜单
-- =====================================================================
-- 需求概述（《小账本 H5 界面开发任务文档 v1.2》§四 M34 T34.5）：
--   老板 PC 端按员工单独设置「公开手机号」/「公开微信号」两个开关，新入职默认全关；
--   开关状态写入 staff_contact_public 表（表已在 [150]M31_04 建好）。
--   顾客端点头像弹名片时按开关显示「打电话」/「加微信」入口（§4.2 名片联系方式矩阵）。
--
-- 本脚本职责：
--   [150]M31_04 已交付 F 级按钮 7059（perms=micro:staff:manage）+ 表 + role_template 授权；
--   本次仅补一个 C 级页面菜单 7070，指向 PC 组件 micro/staff/contactPublic/index，
--   让「员工联系方式公开」页在老板 PC「小微运营」目录下可见可导航。
--
-- 规范：
--   1. 菜单先删后插幂等；授权 INSERT IGNORE 幂等；可重复执行。
--   2. ★menu_id 区段：7070（C 级页，挂 7100 小微运营下）；已核实 7070 空闲
--      （7069 为 [151]M35_01 预估vs实际报表页）。
--   3. 复用 [150]M31_04 已授予的 perms=micro:staff:manage，无需再向 role_template 追加权限键
--      （下方仍保留 JSON_CONTAINS 守卫的幂等追加，缺则补、有则跳过）。
--
-- 前置：
--   [45] M15_06（小微版菜单 7000~7499，含 7100 小微运营目录）
--   [46] M15_07（micro 角色模板三配方 boss/salesman/customer）
--   [150] M31_04（staff_contact_public 表 + F 按钮 7059 + boss/platform_admin 授权 micro:staff:manage）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 菜单：C 级页面「员工联系方式公开」挂 7100（小微运营）下
--    ★先删后插幂等；client_type=pc（老板 PC 端配置页）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7070;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7070;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7070, '员工联系方式公开', 7100, 32, 'staffContactPublic', 'micro/staff/contactPublic/index', 1, 0, 'C', '0', '0', 'micro:staff:manage', 'pc', 'phone', 'admin', NOW(), 'M34 T34.5 §八之二 老板按员工设 show_phone/show_wechat 开关（复用 micro:staff:manage）');

-- =====================================================================
-- 2. role_template：boss + platform_admin 幂等确保含 micro:staff:manage
--    ★[150]M31_04 已追加，此处 JSON_CONTAINS 守卫，缺则补、有则跳过
-- =====================================================================
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staff:manage')
WHERE `role_code` = 'boss' AND `status` = '0'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staff:manage"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staff:manage')
WHERE `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staff:manage"');

-- =====================================================================
-- 3. 存量 sys_role_menu 补录（小微版租户的 boss + platform_admin 克隆角色）
--    ★INSERT IGNORE 幂等
-- =====================================================================
-- 3.1 boss 克隆角色 → 7070
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7070
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro';

-- 3.2 platform_admin 克隆角色 → 7070
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7070
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro';

-- 3.3 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, 7070 FROM DUAL
WHERE EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 7070);

-- =====================================================================
-- 4. 验收查询（执行后人工核对）
-- =====================================================================
-- 4.1 菜单 7070 存在且挂载正确
-- SELECT menu_id, menu_name, parent_id, menu_type, path, component, perms, client_type
--   FROM sys_menu WHERE menu_id = 7070;
-- 预期：7070 | 员工联系方式公开 | 7100 | C | staffContactPublic
--       | micro/staff/contactPublic/index | micro:staff:manage | pc
--
-- 4.2 存量小微版 boss + platform_admin 克隆角色已关联 7070
-- SELECT r.role_key, COUNT(DISTINCT r.role_id) AS granted_roles
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key IN ('boss','platform_admin') AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id = 7070
--  GROUP BY r.role_key;
-- 预期：boss / platform_admin 各 >= 小微版租户数量
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照与前端路由
-- =====================================================================
