-- =====================================================================
-- M18_13 语音合规全链路菜单注册（T18.1~T18.4，v1.53 新增，v1.57 扁平化）
-- 版本：v1.1  日期：2026-09-25
-- 依据：《待开发任务清单》第二批 M18 语音合规全链路（4 项）
--       T18.1 建表（M18_01 已交付）；T18.2/T18.3/T18.4 BE+API+Vue 已交付，
--       唯一缺口 = sys_menu 入口未注册（menu_id 7600~7619 预留区段未使用）
-- 前置依赖：M1_04（sys_menu）+ M15_06（client_type）+ M18_01（四张合规表）
--           + M15_17（7500 platform_admin 目录）+ M15_06（7100 老板运营目录）
-- 说明：
--   1. 语音合规菜单使用 7600~7619 预留区段（00_执行顺序.sql [43] 标注预留）
--   2. Admin 语音合规菜单挂 1370（平台合规目录，M18_12 已建）
--   3. 租户管理员菜单挂 7500（platform_admin 目录）
--   4. 老板菜单挂 7100（老板运营目录）
--   5. perms 与 Controller @SaCheckPermission 严格对齐：
--      - Admin: system:compliance:config / system:compliance:alert / system:compliance:sensitiveWord
--      - 租户管理员: micro:compliance:config:rule/policy/sensitiveWord（单菜单含三 Tab，perms 逗号分隔）
--      - 老板: micro:compliance:records
--   6. ★v1.57 扁平化：租户管理员「语音与合规配置」改为 C 类型单菜单（内含三个 Tab），
--      去掉子菜单（合规规则管理/录音留存策略/敏感词管理）和同级重复的「敏感词管理」(7680~7683)；
--      先删后插幂等（DELETE + INSERT），仅操作本脚本自建 menu_id 区段
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、Admin 语音合规管理菜单（menu_id 7600~7605，挂 1370 平台合规目录）
--     ★对应 MicroAdminComplianceController（/system/compliance）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7600 AND 7609;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7600 AND 7609;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7600, '语音合规管理', 1370, 10, 'voiceCompliance', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'voice', 'admin', NOW(), '★Admin 语音合规：总开关+录音留存上限+合规告警（M18 T18.2）'),
(7601, '语音合规配置', 7600, 1, 'config', 'system/compliance/index', NULL, 1, 0, 'C', '0', '0', 'system:compliance:config', 'pc', 'setting', 'admin', NOW(), '语音合规总开关 + 全局录音留存上限设置（MicroAdminComplianceController /globalRetention）'),
(7602, '合规配置保存', 7601, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'system:compliance:config', 'pc', '#', 'admin', NOW(), 'PUT /system/compliance/globalRetention'),
(7603, '合规告警看板', 7600, 2, 'alert', 'system/compliance/index', NULL, 1, 0, 'C', '0', '0', 'system:compliance:alert', 'pc', 'chart', 'admin', NOW(), '近 7 天命中趋势 + 按类型统计 + 待处理数（GET /system/compliance/alertDashboard）'),
(7604, '平台敏感词库', 7600, 3, 'sensitiveWord', 'system/compliance/index', NULL, 1, 0, 'C', '0', '0', 'system:compliance:sensitiveWord', 'pc', 'documentation', 'admin', NOW(), '平台敏感词 CRUD + 批量导入（/system/compliance/sensitiveWord/*）');

-- 授予超管 role_id=1
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7600 AND 7609;

-- =====================================================================
-- 二、租户管理员语音合规配置菜单（menu_id 7610，挂 7500 platform_admin 目录）
--     ★对应 MicroTenantComplianceController（/micro/compliance/config）
--     ★v1.57 扁平化：单 C 类型菜单，内含合规规则/录音留存/敏感词三个 Tab
--     ★同时清理同级重复的「敏感词管理」(7680~7683，M18_12 规划但未交付)
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7610 AND 7617;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7610 AND 7617;
-- ★清理同级重复的「敏感词管理」(7680~7683)
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7680 AND 7683;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7680 AND 7683;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7610, '语音与合规配置', 7500, 40, 'voiceConfig', 'micro/compliance/config/index', NULL, 1, 0, 'C', '0', '0', 'micro:compliance:config:rule,micro:compliance:config:policy,micro:compliance:config:sensitiveWord', 'pc', 'shield', 'admin', NOW(), '★租户管理员：合规规则+录音留存+敏感词管理（三 Tab 单页面，M18 T18.3）');

-- =====================================================================
-- 三、老板语音合规记录菜单（menu_id 7618，挂 7100 老板运营目录）
--     ★对应 MicroComplianceRecordController（/micro/compliance/records）
--     ★只读，无编辑操作；★仅 micro_pro 可用
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7618;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7618;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7618, '语音合规记录', 7100, 95, 'voiceRecords', 'micro/compliance/records/index', NULL, 1, 0, 'C', '0', '0', 'micro:compliance:records', 'pc', 'log', 'admin', NOW(), '★老板只读：合规命中记录（按时间/类型筛选，无编辑）（M18 T18.4）');

-- =====================================================================
-- 四、sys_role_menu 存量补录
-- =====================================================================
-- 4.1 租户管理员菜单（7610）→ platform_admin 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin' AND m.menu_id = 7610;

-- 4.2 老板菜单（7618）→ boss 克隆角色（micro_pro 租户）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r CROSS JOIN `sys_menu` m
WHERE r.role_key = 'boss' AND m.menu_id = 7618;

-- =====================================================================
-- 五、role_template 追加（JSON_CONTAINS 守卫幂等）
-- =====================================================================
-- 5.1 platform_admin(micro/micro_pro) 追加语音合规配置权限（单 key）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:compliance:config:rule')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:compliance:config:rule"');

-- 5.2 boss(micro/micro_pro) 追加语音合规记录只读权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:compliance:records')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:compliance:records"');

-- =====================================================================
-- 六、验收查询
-- =====================================================================
-- 6.1 Admin 语音合规菜单（7600~7605）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--   WHERE menu_id BETWEEN 7600 AND 7609 ORDER BY menu_id;
--   -- 预期 5 行（7600 目录 + 7601/7603/7604 三 C + 7602 F 按钮）
--
-- 6.2 租户管理员语音合规菜单（7610 单菜单，无子菜单）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--   WHERE menu_id = 7610;
--   -- 预期 1 行（7610 C 类型，perms 含 rule+policy+sensitiveWord 三权限逗号分隔）
--
-- 6.3 同级「敏感词管理」已清理
-- SELECT COUNT(*) FROM sys_menu WHERE menu_id BETWEEN 7680 AND 7683;
--   -- 预期 0
--
-- 6.4 老板语音合规记录菜单（7618）
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id = 7618;
--   -- 预期 1 行（micro:compliance:records）
--
-- 6.5 role_template 含新权限
-- SELECT JSON_CONTAINS(menu_keys,'"micro:compliance:config:rule"') AS pa_rule,
--        JSON_CONTAINS(menu_keys,'"micro:compliance:records"')      AS boss_records
--   FROM role_template WHERE version_type='micro_pro'
--    AND role_code IN ('platform_admin','boss');
--   -- platform_admin 行 pa_rule=1；boss 行 boss_records=1
--
-- 6.6 超管授权完整
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1 AND menu_id IN (7600,7601,7602,7603,7604,7610,7618);
--   -- 预期 7
-- =====================================================================
