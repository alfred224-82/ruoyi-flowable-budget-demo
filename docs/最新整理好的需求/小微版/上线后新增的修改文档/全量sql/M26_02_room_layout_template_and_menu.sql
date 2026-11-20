-- =====================================================================
-- M26_02  房间布局模板菜单 + FEAT_ROOM_SEAT_LAYOUT 功能开关（T26.4 / T26.5 / T26.6，v1.0）
-- 版本：v1.0  日期：2026-10-13
-- 依据：《小微版 V3.9 开发文档》v1.2 M26 里程碑 T26.4（拖拽定位）/ T26.5（复制布局+存为模板）
--        / T26.6（平台默认房间模板）；§1.2 菜单段位表 + §附录 B menu_id 段位规划
-- 归属版本：小微版（MICRO，micro_pro 专属；micro_lite 不启用座位布局）
-- 前置依赖：
--   M26_01（room_seat_layout / room_seat / room_seat_layout_template 三表 + service_item 二态列 + 菜单 7280/7281）
--   M26_03（activity_seat_assignment / activity_seat_operation_log 两表 + 菜单 7290）
--   M1_05（feature_flag 表 + biz_bundle 表 + License 三维交集）
--   M15_06（sys_menu.client_type + 小微版菜单 7000~7499 段位；7100 小微运营目录 / 7500 平台管理目录）
--   M15_07/M15_10（micro / micro_lite / micro_pro 三档 role_template 配方）
--   M15_23（原子资源管理目录 7510，parent=7500，platform_admin 专属）
--   M21_07（FEAT_SELF_BOOKING biz_bundle 全量补录参考模式）
--   M25_01（FEAT_COMPLAINT 组 E feature_flag 参考模式）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. feature_flag 新增第 41 项 FEAT_ROOM_SEAT_LAYOUT（组 E，micro_enabled=1，仅 micro_pro 启用）
--   2. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_ROOM_SEAT_LAYOUT（三维交集根因防御）
--   3. sys_menu 新增 7282（复制布局 F）/ 7283（存为模板 F）/ 7285（默认房间模板 C）/ 7286（新增/编辑/删除 F）
--   4. sys_menu 7280（房间布局配置 C）visible='1' → '0'（T26.4 前端页面同批交付，置显）
--   5. role_template boss(micro/micro_pro) 追加 micro:room:copyLayout / micro:room:saveTemplate（micro_lite 不授）
--   6. role_template platform_admin(micro/micro_pro/micro_lite) 追加 system:platformRoomTemplate:view / edit
--      ★T26.6 平台默认房间模板归属租户 platform_admin 侧的原子资源管理目录（7510），
--        Service 层强制 tenant_id=1 + is_industry_default=1 保证跨租户共享（详见 RoomSeatLayoutServiceImpl）
--   7. 存量 sys_role_menu 补录：boss（排除 micro_lite）+ platform_admin（全档）+ 超管 role_id=1
-- ---------------------------------------------------------------------
-- menu_id 段位说明（§附录 B）：
--   7280~7299：M26 房间布局（7280/7281/7282/7283）+ M26 顾客选座（7290 由 M26_03 占用）+ M28 活动锁定（7287~7289 预留）
--   7285/7286：M26 平台默认房间模板（parent=7510 原子资源管理目录，platform_admin 专属）
--   7517~7519：预留给 M26 后续扩展（若需要拆分 add/edit/remove 三 F 按钮，可后续 M26_04 迁移）
-- ---------------------------------------------------------------------
-- 规范：
--   1. 先删后插幂等 + NOT JSON_CONTAINS 守卫，重复执行不报错
--   2. UPDATE 包裹 SQL_SAFE_UPDATES 开关
--   3. Controller @SaCheckPermission keys 与 sys_menu.perms 一一对应；
--      7286 F 按钮 perms 采用逗号分隔多 key（system:platformRoomTemplate:add,edit,remove），
--      避免"单页面多 Tab 陷阱"（memory fcefcccf-6807-464b-a2a6-bb0a077229ba）
--   4. ★执行后请重启后端，清除 BizBundleServiceImpl.flagCache / FeatureFlagServiceImpl.flagsCache /
--      LicenseServiceImpl.tenantFeaturesCache；用户需退出重新登录刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. feature_flag 新增第 41 项 FEAT_ROOM_SEAT_LAYOUT（组 E=小微版专属）
--    ★micro_enabled=1；basic/advanced/pro 三列均 0（企业版不生效）
--    ★座位布局为 micro_pro 专属能力，micro_lite 不启用（role_template 层控制）
--    ★类级 @RequireFeature("FEAT_ROOM_SEAT_LAYOUT") 标注 MicroRoomLayoutController +
--      MicroPlatformRoomTemplateController（MicroSeatAssignmentController 保持不变，
--      T26.7 顾客选座为 micro_lite 也需支持的基本能力，走防御式降级）
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_ROOM_SEAT_LAYOUT';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `micro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_ROOM_SEAT_LAYOUT', '房间座位布局与选座(拖拽定位+复制布局+存为模板+平台默认模板)', 'E', NULL, 0, 0, 0, 1, '0', 'admin', NOW(),
 'T26.4~T26.6；★仅小微版启用；micro_pro 专属（micro_lite 不启用布局能力，仅保留 T26.7 顾客选座降级）；M26_02 首建');

-- ----------------------------
-- 2. biz_bundle 全部业态包 feature_flags 幂等补录 FEAT_ROOM_SEAT_LAYOUT
--    ★三维交集根因防御（License ∩ feature_flag ∩ biz_bundle）；
--      参考 M21_07 修复模式，避免 applyBizFilter 第三维剔除导致「小微版无此功能权限」
--    ★不限定 biz_code：覆盖三个标准业态包 + 后续新建的自定义业态包
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

UPDATE `biz_bundle`
SET `feature_flags` = JSON_ARRAY_APPEND(`feature_flags`, '$', 'FEAT_ROOM_SEAT_LAYOUT')
WHERE `feature_flags` IS NOT NULL
  AND NOT JSON_CONTAINS(`feature_flags`, '"FEAT_ROOM_SEAT_LAYOUT"');

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. sys_menu：房间布局 7282/7283（Boss F）+ 平台默认模板 7285/7286（platform_admin C+F）
--    ★7282/7283 挂 7280（房间布局配置 C）；先删后插幂等
--    ★7285 挂 7510（原子资源管理 M，parent=7500 平台管理目录，platform_admin 专属）
--    ★7286 F 按钮 perms 采用逗号分隔多 key，覆盖 Controller 三处 @SaCheckPermission
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7282, 7283, 7285, 7286);
DELETE FROM `sys_menu` WHERE `menu_id` IN (7282, 7283, 7285, 7286);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7282: 复制布局（F 按钮，Boss PC；T26.5 从其他房间复制布局到当前房间）
(7282, '复制布局', 7280, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:room:copyLayout', 'pc', '#', 'admin', NOW(), 'T26.5 POST /micro/room/layout/copyFromRoom（源房间布局→目标房间，脱钩独立）'),
-- 7283: 存为模板（F 按钮，Boss PC；T26.5 把当前布局保存为 Boss 私有模板）
(7283, '存为模板', 7280, 3, '', NULL, 1, 0, 'F', '0', '0', 'micro:room:saveTemplate', 'pc', '#', 'admin', NOW(), 'T26.5 POST /micro/room/layout/saveAsTemplate（Boss 私有模板 tenant_id=当前租户，is_industry_default=0）'),
-- 7285: 默认房间模板（C 页面，platform_admin PC；挂 7510 原子资源管理目录）
(7285, '默认房间模板', 7510, 5, 'platformRoomTemplate', 'micro/platform/roomTemplate/index', 1, 0, 'C', '0', '0', 'system:platformRoomTemplate:view', 'pc', 'build', 'admin', NOW(), 'T26.6 平台默认房间模板管理（tenant_id=1 + is_industry_default=1，跨租户共享，按 industry_code 匹配）'),
-- 7286: 新增/编辑/删除（F 按钮，perms 逗号分隔多 key 覆盖 Controller 三处 @SaCheckPermission）
(7286, '新增/编辑/删除', 7285, 1, '', NULL, 1, 0, 'F', '0', '0', 'system:platformRoomTemplate:add,system:platformRoomTemplate:edit,system:platformRoomTemplate:remove', 'pc', '#', 'admin', NOW(), 'T26.6 平台默认房间模板增删改（Service 层强制 tenant_id=1 + is_industry_default=1，防前端篡改）');

-- ----------------------------
-- 4. sys_menu 7280（房间布局配置 C）保持 visible='1'（隐藏）
--    ★房间布局配置为 platform_admin 功能（平行菜单 7291 挂 7510），Boss 不应在侧边栏看到 7280
--    ★M26_01 创建时 visible='1'，此处不做变更
-- ----------------------------

-- ----------------------------
-- 5. role_template：boss(micro_pro) 追加 micro:room:copyLayout / micro:room:saveTemplate
--    ★micro_lite 不授（FEAT_ROOM_SEAT_LAYOUT micro_pro 专属，座位布局为付费档能力）
--    ★micro:room:layout / micro:room:uploadBg 已由 M26_01 追加，本脚本仅补 T26.5 两权限键
--    ★JSON_CONTAINS 守卫幂等追加
-- ----------------------------
-- 5.1 micro_pro boss
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:copyLayout')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:copyLayout"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:saveTemplate')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:saveTemplate"');

-- 5.2 micro_lite boss（★不授 copyLayout/saveTemplate，座位布局为 micro_pro 专属）
--    无操作：micro_lite 不授此权限

-- ----------------------------
-- 6. role_template：platform_admin(micro_pro/micro_lite) 追加 system:platformRoomTemplate:view / edit
--    ★T26.6 平台默认房间模板由租户 platform_admin 侧维护（挂 7510 原子资源管理目录）
--    ★add/edit/remove 三 F 按钮 perms 统一以 edit 权限键入配方（7286 F 按钮 perms 逗号分隔三 key，
--      Sa-Token 校验时任一命中即通过；role_template 只需登记一个代表键即可覆盖）
--    ★micro_lite platform_admin 也授（模板维护为租户管理员基本能力，不受版本档位限制）
-- ----------------------------
-- 6.1 micro_pro platform_admin
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'system:platformRoomTemplate:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'system:platformRoomTemplate:edit')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:edit"');

-- 6.2 micro_lite platform_admin
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'system:platformRoomTemplate:view')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'system:platformRoomTemplate:edit')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:edit"');

-- ----------------------------
-- 7. 存量 sys_role_menu 补录
-- ----------------------------
-- 7.1 boss（小微版 micro_pro，★排除 micro_lite）：7282（复制布局 F）+ 7283（存为模板 F）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND (t.`version_type` IS NULL OR t.`version_type` <> 'micro_lite')
  AND m.`menu_id` IN (7282, 7283);

-- 7.2 platform_admin（小微版全版本档）：7285（C 页面）+ 7286（F 按钮）
--   ★F 按钮无需祖先补录（parent 7285 已同批授予）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7285, 7286);

-- 7.3 超管 role_id=1 全量补录（便于平台侧联调 + 兜底 7510 祖先目录授权）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7282, 7283, 7285, 7286);

-- ----------------------------
-- 8. 验收查询（执行后人工核对）
-- ----------------------------
-- 8.1 FEAT_ROOM_SEAT_LAYOUT 落表且 micro_enabled=1
-- SELECT code, flag_group, micro_enabled, basic_enabled, status FROM feature_flag WHERE code = 'FEAT_ROOM_SEAT_LAYOUT';
-- 预期：1 行，E / micro_enabled=1 / basic_enabled=0 / status=0
--
-- 8.2 每个业态包均含 FEAT_ROOM_SEAT_LAYOUT
-- SELECT biz_code, JSON_CONTAINS(feature_flags,'"FEAT_ROOM_SEAT_LAYOUT"') AS f_room_layout FROM biz_bundle;
-- 预期：所有 feature_flags 非 NULL 的业态包 f_room_layout=1
--
-- 8.3 菜单 7282/7283/7285/7286 存在且 7280 visible='1'（隐藏，Boss 侧边栏不显示）
-- SELECT menu_id, menu_name, parent_id, menu_type, visible, perms, client_type FROM sys_menu
--  WHERE menu_id IN (7280, 7282, 7283, 7285, 7286) ORDER BY menu_id;
-- 预期：5 行
--   7280 C visible=1 micro:room:layout pc（隐藏，Boss 侧边栏不显示）
--   7282 F micro:room:copyLayout pc（parent=7280）
--   7283 F micro:room:saveTemplate pc（parent=7280）
--   7285 C system:platformRoomTemplate:view pc（parent=7510）
--   7286 F system:platformRoomTemplate:add,edit,remove pc（parent=7285）
--
-- 8.4 role_template boss(micro_pro) 含布局权限，micro_lite 不含
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:room:copyLayout"')   AS has_copy,
--        JSON_CONTAINS(menu_keys,'"micro:room:saveTemplate"') AS has_save_tpl
--   FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro_pro','micro_lite');
-- 预期：micro_pro 为 1/1，micro_lite 为 0/0
--
-- 8.5 role_template platform_admin 两档均含平台模板权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"system:platformRoomTemplate:view"') AS has_view,
--        JSON_CONTAINS(menu_keys,'"system:platformRoomTemplate:edit"') AS has_edit
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 预期：micro_pro/micro_lite 两行均 1/1
--
-- 8.6 存量小微版租户 boss 克隆角色已授权 7282/7283（排除 micro_lite）
-- SELECT t.version_type, COUNT(DISTINCT r.role_id) AS granted_roles, COUNT(*) AS granted_rows
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (7282, 7283)
--  GROUP BY t.version_type;
-- 预期：micro_pro 有记录，micro_lite 无记录
--
-- 8.7 存量小微版租户 platform_admin 克隆角色已授权 7285/7286
-- SELECT COUNT(DISTINCT r.role_id) AS granted_roles FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (7285, 7286);
-- 预期：>= 小微版租户数量
--
-- 8.8 端到端：micro_pro boss 重新登录后访问
--     GET /micro/room/layout/{roomId}、POST /micro/room/layout/copyFromRoom、
--     POST /micro/room/layout/saveAsTemplate、POST /micro/room/layout/applyTemplate 均返回 200
--     platform_admin 重新登录后访问 /system/platformRoomTemplate/list 返回 200
--     不再抛「小微版无此功能权限」
--
-- ★执行后请重启后端，清除 BizBundleServiceImpl.flagCache / FeatureFlagServiceImpl.flagsCache /
--   LicenseServiceImpl.tenantFeaturesCache；用户需退出重新登录刷新 Sa-Token 权限快照
