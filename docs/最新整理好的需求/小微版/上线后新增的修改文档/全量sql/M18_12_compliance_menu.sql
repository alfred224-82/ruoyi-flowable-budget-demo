-- =====================================================================
-- M18_12 Admin 平台合规菜单 + 租户敏感词管理 + 老板合规命中记录（v1.55 补建）
-- 版本：v1.0  日期：2026-09-21
-- 依据：《资金管控系统开发任务文档（小微版）》v1.36 M18
--       00_执行顺序.sql [56] 引用本脚本但文件缺失，本次补建
-- 前置依赖：M1_04（sys_menu）+ M15_06（client_type）+ M18_10（三表）
--           + M15_17（7500 platform_admin 目录）+ M15_06（7100 老板运营目录）
-- 说明：
--   1. Admin 平台合规目录 1370~1379（挂 100 系统管理下）
--      ★M18_13 语音合规菜单(7600~7604) parent_id=1370 依赖本目录
--   2. 租户管理员敏感词管理 7680~7686（挂 7500 platform_admin 目录下）
--      ★对应 MicroTenantComplianceController /micro/compliance/config/sensitiveWord
--   3. 老板合规命中记录 7690（挂 7100 老板运营目录下）
--      ★只读，无编辑操作
--   4. 先删后插幂等，仅操作本脚本自建 menu_id 区段
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、Admin 平台合规管理目录（menu_id 1370~1379，挂 100 系统管理下）
--     ★对应 MicroAdminComplianceController（/system/compliance）
--     ★M18_13 语音合规菜单(7600~7604) parent_id=1370 依赖本目录
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1370 AND 1379;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 1370 AND 1379;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(1370, '平台合规管理', 100, 70, 'compliance', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'shield', 'admin', NOW(), '★Admin 平台合规：敏感词库+图片合规+合规告警（M18 T18.1/T18.8）'),
(1371, '敏感词库管理', 1370, 1, 'sensitiveWord', 'system/compliance/sensitiveWord/index', NULL, 1, 0, 'C', '0', '0', 'system:compliance:sensitiveWord', 'pc', 'documentation', 'admin', NOW(), '平台全局敏感词 CRUD + 批量导入（/system/compliance/sensitiveWord/*）'),
(1372, '图片合规配置', 1370, 2, 'imageConfig', 'system/compliance/imageConfig/index', NULL, 1, 0, 'C', '0', '0', 'system:compliance:imageConfig', 'pc', 'image', 'admin', NOW(), '图片合规三类配置（porn/violence/politics）× 两路开关（/system/compliance/imageConfig/*）'),
(1373, '合规告警看板', 1370, 3, 'complianceAlert', 'system/compliance/alert/index', NULL, 1, 0, 'C', '0', '0', 'system:compliance:alert', 'pc', 'chart', 'admin', NOW(), '合规告警总览（/system/compliance/alertDashboard）');

-- 授予超管 role_id=1
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 1370 AND 1379;

-- =====================================================================
-- 二、租户管理员敏感词管理菜单（menu_id 7680~7686，挂 7500 platform_admin 目录）
--     ★对应 MicroTenantComplianceController（/micro/compliance/config/sensitiveWord）
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7680 AND 7686;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7680 AND 7686;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7680, '敏感词管理', 7500, 50, 'sensitiveWord', 'micro/sensitiveWord/index', NULL, 1, 0, 'C', '0', '0', 'micro:sensitiveWord:list', 'pc', 'edit', 'admin', NOW(), '★租户管理员：继承平台底库（只读）+ 自定义敏感词 CRUD（/micro/compliance/config/sensitiveWord/*）'),
(7681, '敏感词新增', 7680, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:add', 'pc', '#', 'admin', NOW(), 'POST /micro/compliance/config/sensitiveWord'),
(7682, '敏感词修改', 7680, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:edit', 'pc', '#', 'admin', NOW(), 'PUT /micro/compliance/config/sensitiveWord'),
(7683, '敏感词删除', 7680, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:sensitiveWord:remove', 'pc', '#', 'admin', NOW(), 'DELETE /micro/compliance/config/sensitiveWord/{id}');

-- =====================================================================
-- 三、老板合规命中记录菜单（menu_id 7690，挂 7100 老板运营目录）
--     ★只读，无编辑操作
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7690;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7690;

INSERT INTO `sys_menu` (`menu_id`,`menu_name`,`parent_id`,`order_num`,`path`,`component`,`query_param`,`is_frame`,`is_cache`,`menu_type`,`visible`,`status`,`perms`,`client_type`,`icon`,`create_by`,`create_time`,`remark`) VALUES
(7690, '合规命中记录', 7100, 96, 'complianceHit', 'micro/compliance/hit/index', NULL, 1, 0, 'C', '0', '0', 'micro:boss:complianceHit:list', 'pc', 'log', 'admin', NOW(), '★老板只读：敏感词/图片合规命中记录（按时间/类型筛选，无编辑）（M18 T18.8）');

-- =====================================================================
-- 四、sys_role_menu 存量补录
-- =====================================================================
-- 4.1 租户管理员菜单（7680~7683）→ platform_admin 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin' AND m.menu_id BETWEEN 7680 AND 7683;

-- 4.2 老板菜单（7690）→ boss 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r CROSS JOIN `sys_menu` m
WHERE r.role_key = 'boss' AND m.menu_id = 7690;

-- =====================================================================
-- 五、role_template 追加（JSON_CONTAINS 守卫幂等）
-- =====================================================================
-- 5.1 platform_admin(micro/micro_pro) 追加敏感词管理权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
      JSON_ARRAY_APPEND(
        `menu_keys`,
        '$', 'micro:sensitiveWord:list'),
      '$', 'micro:sensitiveWord:add'),
    '$', 'micro:sensitiveWord:edit'
)
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:list"');

-- ★补丁：对已含 list 但缺 remove 的存量 role_template 单独追加（幂等）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:sensitiveWord:remove')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:list"')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:sensitiveWord:remove"');

-- 5.2 boss(micro/micro_pro) 追加合规命中记录只读权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:boss:complianceHit:list')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:boss:complianceHit:list"');

-- 5.3 admin 追加平台合规管理权限
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
      JSON_ARRAY_APPEND(
        `menu_keys`,
        '$', 'system:compliance:sensitiveWord'),
      '$', 'system:compliance:imageConfig'),
    '$', 'system:compliance:alert'
)
WHERE `version_type` IS NULL
  AND `role_code` = 'admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:compliance:sensitiveWord"');

-- =====================================================================
-- 六、验收查询
-- =====================================================================
-- 6.1 Admin 平台合规菜单（1370~1373）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--   WHERE menu_id BETWEEN 1370 AND 1379 ORDER BY menu_id;
--   -- 预期 4 行（1370 目录 + 1371/1372/1373 三 C）
--
-- 6.2 租户管理员敏感词管理（7680~7683）
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu
--   WHERE menu_id BETWEEN 7680 AND 7686 ORDER BY menu_id;
--   -- 预期 4 行（7680 C + 7681/7682/7683 三 F）
--
-- 6.3 老板合规命中记录（7690）
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id = 7690;
--   -- 预期 1 行（micro:boss:complianceHit:list）
--
-- 6.4 role_template 含新权限
-- SELECT JSON_CONTAINS(menu_keys,'"micro:sensitiveWord:list"')    AS pa_sw,
--        JSON_CONTAINS(menu_keys,'"micro:boss:complianceHit:list"') AS boss_hit,
--        JSON_CONTAINS(menu_keys,'"system:compliance:sensitiveWord"') AS admin_sw
--   FROM role_template
--  WHERE (version_type='micro_pro' AND role_code='platform_admin')  -- pa_sw=1
--     OR (version_type='micro_pro' AND role_code='boss')            -- boss_hit=1
--     OR (version_type IS NULL AND role_code='admin');              -- admin_sw=1
--
-- 6.5 超管授权完整（1370~1373 + 7600~7604 由 M18_13 建）
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id=1 AND menu_id BETWEEN 1370 AND 1379;
--   -- 预期 4
-- =====================================================================
