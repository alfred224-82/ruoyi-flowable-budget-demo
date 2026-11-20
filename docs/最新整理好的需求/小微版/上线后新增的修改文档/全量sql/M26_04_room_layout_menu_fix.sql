-- =====================================================================
-- M26_04 房间布局配置菜单 platform_admin 授权修复脚本
-- 版本：v1.0  日期：2026-10-13
-- 依据：用户反馈租户管理员(platform_admin)看不到「房间布局配置」菜单
-- 根因：菜单 7280（房间布局配置 C）挂在 7100（小微运营，boss 专属目录）下，
--       platform_admin 无 7100 访问权限 → 整棵子树不可见。
--       ★M15_26 明确隔离原则：boss 菜单 ≠ platform_admin 菜单，
--         不能简单把 7280 re-parent 到 7510（会破坏 boss 访问 + 违反隔离）。
-- 修复逻辑（全部幂等，尊重角色隔离原则）：
--   Step 1: 新建菜单 7291（房间布局配置 C）挂 7510 下，指向同一页面组件
--   Step 2: 新建 F 按钮 7292~7294（上传/复制/存为模板）挂 7291 下
--   Step 3: role_template platform_admin 追加 4 个权限键
--   Step 4: 存量 sys_role_menu 补录（platform_admin 7291~7294）
--   Step 5: 超管 role_id=1 补录
-- 前置依赖：M26_01（7280/7281 建菜单 + boss 授权）、M26_02（7280 置显 + 7282/7283）
-- ★menu_id 区段：7291~7294（避让 M26 7280~7289 / M28 7287~7289 / M26 顾客选座 7290）
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 新建菜单 7291（房间布局配置 C，platform_admin 专属）
--         挂在 7510（原子资源管理）下，与 7514（房间/空间管理）、7285（默认房间模板）归入同一目录
-- =====================================================================
-- ★先删后插幂等
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7291, 7292, 7293, 7294);
DELETE FROM `sys_menu` WHERE `menu_id` IN (7291, 7292, 7293, 7294);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7291: 房间布局配置（C 页面，platform_admin PC；与 7280 指向同一组件 micro/room/layout/index）
(7291, '房间布局配置', 7510, 6, 'roomLayout', 'micro/room/layout/index', 1, 0, 'C', '0', '0', 'micro:room:layout', 'pc', 'edit', 'admin', NOW(), 'M26_04 platform_admin 房间布局配置（与 boss 7280 同组件，挂在原子资源管理下）'),
-- 7292: 上传背景图（F 按钮，T26.3 上传前置校验接口鉴权）
(7292, '上传背景图', 7291, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:room:uploadBg', 'pc', '#', 'admin', NOW(), 'M26_04 POST /micro/room/uploadBgImage（格式/大小/尺寸前置校验）'),
-- 7293: 复制布局（F 按钮，T26.5 从其他房间复制布局到当前房间）
(7293, '复制布局', 7291, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:room:copyLayout', 'pc', '#', 'admin', NOW(), 'M26_04 POST /micro/room/layout/copyFromRoom（源房间布局→目标房间，脱钩独立）'),
-- 7294: 存为模板（F 按钮，T26.5 把当前布局保存为私有模板）
(7294, '存为模板', 7291, 3, '', NULL, 1, 0, 'F', '0', '0', 'micro:room:saveTemplate', 'pc', '#', 'admin', NOW(), 'M26_04 POST /micro/room/layout/saveAsTemplate（私有模板 tenant_id=当前租户）');

-- =====================================================================
-- Step 2: role_template platform_admin 追加房间布局权限键
-- =====================================================================
-- ★micro_lite/micro_pro 均授（房间布局配置为租户管理员基本能力）
-- ★JSON_CONTAINS 守卫幂等追加
-- =====================================================================

-- 2.1 micro_pro platform_admin
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:layout')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:layout"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:uploadBg')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:uploadBg"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:copyLayout')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:copyLayout"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:saveTemplate')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:saveTemplate"');

-- 2.2 micro (旧档) platform_admin
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:layout')
WHERE `version_type` = 'micro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:layout"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:uploadBg')
WHERE `version_type` = 'micro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:uploadBg"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:copyLayout')
WHERE `version_type` = 'micro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:copyLayout"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:saveTemplate')
WHERE `version_type` = 'micro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:saveTemplate"');

-- 2.3 micro_lite platform_admin（房间布局为通用能力，不受版本档位限制）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:layout')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:layout"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:room:uploadBg')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:room:uploadBg"');

-- =====================================================================
-- Step 3: 存量 sys_role_menu 补录
-- =====================================================================

-- 3.1 platform_admin（小微版全版本档）：7291（C 页面）+ 7292/7293/7294（F 按钮）
--   ★platform_admin 已有 7510 祖先目录（M15_23 Step 6.2），7291 挂 7510 下可正常显示
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7291, 7292, 7293, 7294);

-- 3.2 超管 role_id=1 补录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` IN (7291, 7292, 7293, 7294);

-- =====================================================================
-- 4. 验收查询（执行后人工核对）
-- =====================================================================
-- 4.1 新菜单 7291~7294 存在且挂在 7510 下
-- SELECT menu_id, menu_name, parent_id, menu_type, visible, perms FROM sys_menu
--  WHERE menu_id IN (7291, 7292, 7293, 7294) ORDER BY menu_id;
-- 预期：4 行（7291 C parent=7510 visible=0 / 7292~7294 F parent=7291）
--
-- 4.2 原菜单 7280 仍在 7100 下（boss 专属，未改动）
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE menu_id = 7280;
-- 预期：parent_id = 7100
--
-- 4.3 role_template platform_admin 三档均含房间布局权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:room:layout"')    AS has_layout,
--        JSON_CONTAINS(menu_keys,'"micro:room:uploadBg"')   AS has_upload,
--        JSON_CONTAINS(menu_keys,'"micro:room:copyLayout"') AS has_copy,
--        JSON_CONTAINS(menu_keys,'"micro:room:saveTemplate"') AS has_save
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 预期：micro/micro_pro/micro_lite 三行均 1/1/1/1
--
-- 4.4 存量 platform_admin 克隆角色已关联 7291~7294
-- SELECT COUNT(DISTINCT r.role_id) AS granted_roles
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id IN (7291, 7292, 7293, 7294);
-- 预期：>= 小微版租户数量
--
-- 4.5 boss 不应有 7291~7294（platform_admin 专属菜单）
-- SELECT rm.menu_id, m.menu_name
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE rm.menu_id IN (7291, 7292, 7293, 7294);
-- 预期：0 行
--
-- 4.6 boss 不应有 7510（M15_26 隔离原则：boss 无 platform_admin 原子资源目录）
-- SELECT COUNT(*) AS boss_with_7510
--   FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'boss' AND r.del_flag = '0'
--  WHERE rm.menu_id = 7510;
-- 预期：0
--
-- ★执行后请重启后端，清除内存缓存；用户需退出重新登录刷新 Sa-Token 权限快照
