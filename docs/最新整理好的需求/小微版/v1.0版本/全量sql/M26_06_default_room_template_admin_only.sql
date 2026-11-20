-- =====================================================================
-- M26_06  默认房间模板菜单归属修正：platform_admin → admin（T26.6 修正）
-- 版本：v1.0  日期：2026-10-13
-- 依据：《小微版 V3.9 开发文档》v1.10 T26.6 修正
-- 归属版本：小微版（MICRO）
-- 前置依赖：
--   M26_02（菜单 7285/7286 首建 + platform_admin 授权）
--   M15_06（sys_menu 7000 行业模板管理 C，admin 专属）
--   M15_12（1300 资金平台管理菜单目录，admin 专属）
-- ---------------------------------------------------------------------
-- 根因：
--   M26_02 将 7285（默认房间模板 C）挂在 7510（原子资源管理，platform_admin 专属）下，
--   并授给了 platform_admin 角色。但"默认房间模板"是平台级兜底配置
--   （tenant_id=1 + is_industry_default=1，跨租户共享），应由 admin（超管）统一维护，
--   不应出现在租户管理员菜单中。
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. sys_menu 7285/7286 parent_id 从 7510 改为 7000（行业模板管理，admin 专属）
--   2. role_template platform_admin 三档移除 system:platformRoomTemplate:view/edit
--   3. 存量 sys_role_menu 清理：移除所有 platform_admin 克隆角色对 7285/7286 的授权
--   4. admin role_id=1 保留 7285/7286 授权（M26_02 已授，本脚本不重复）
-- ---------------------------------------------------------------------
-- 规范：
--   1. 先删后插幂等 + JSON_REMOVE 守卫，重复执行不报错
--   2. UPDATE 包裹 SQL_SAFE_UPDATES 开关
--   3. ★执行后请重启后端，用户需退出重新登录刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. sys_menu 7285/7286 parent_id 从 7510 改为 7000
--    ★7000 = 行业模板管理（C，admin 专属，parent=1300 资金平台管理菜单）
--    ★7285 与行业模板管理同属 admin 平台级配置，语义一致
--    ★幂等：仅当 parent_id=7510 时更新
-- ----------------------------
UPDATE `sys_menu`
SET `parent_id` = 7000, `order_num` = 4
WHERE `menu_id` IN (7285, 7286) AND `parent_id` = 7510;

-- ----------------------------
-- 2. role_template platform_admin 三档移除 system:platformRoomTemplate:view/edit
--    ★JSON_REMOVE 按值删除，NOT JSON_CONTAINS 守卫保证幂等
--    ★micro_lite 也移除（模板维护为平台级能力，租户管理员不应看到）
-- ----------------------------
SET SQL_SAFE_UPDATES = 0;

-- 2.1 micro_pro platform_admin
UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:platformRoomTemplate:view')))
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:platformRoomTemplate:edit')))
WHERE `version_type` = 'micro_pro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:edit"');

-- 2.2 micro (旧档) platform_admin
UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:platformRoomTemplate:view')))
WHERE `version_type` = 'micro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:platformRoomTemplate:edit')))
WHERE `version_type` = 'micro' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:edit"');

-- 2.3 micro_lite platform_admin
UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:platformRoomTemplate:view')))
WHERE `version_type` = 'micro_lite' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_REMOVE(`menu_keys`, JSON_UNQUOTE(JSON_SEARCH(`menu_keys`, 'one', 'system:platformRoomTemplate:edit')))
WHERE `version_type` = 'micro_lite' AND `role_code` = 'platform_admin' AND `status` = '0'
  AND JSON_CONTAINS(`menu_keys`, '"system:platformRoomTemplate:edit"');

SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 3. 存量 sys_role_menu 清理：移除 platform_admin 克隆角色对 7285/7286 的授权
--    ★不影响 admin role_id=1（M26_02 已授，本脚本不碰）
--    ★不影响 boss 角色（boss 从未授 7285/7286）
-- ----------------------------
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON rm.`role_id` = r.`role_id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE rm.`menu_id` IN (7285, 7286);

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 菜单 7285/7286 已迁移到 7000 下
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu WHERE menu_id IN (7285, 7286);
-- 预期：parent_id=7000
--
-- 4.2 platform_admin role_template 不再含平台模板权限
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys,'"system:platformRoomTemplate:view"') AS has_view,
--        JSON_CONTAINS(menu_keys,'"system:platformRoomTemplate:edit"') AS has_edit
--   FROM role_template WHERE role_code = 'platform_admin' AND status = '0';
-- 预期：三档均 0/0
--
-- 4.3 存量 platform_admin 克隆角色不再持有 7285/7286
-- SELECT COUNT(*) AS remaining FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'platform_admin'
--  WHERE rm.menu_id IN (7285, 7286);
-- 预期：0
--
-- 4.4 admin role_id=1 仍持有 7285/7286
-- SELECT menu_id FROM sys_role_menu WHERE role_id = 1 AND menu_id IN (7285, 7286) ORDER BY menu_id;
-- 预期：2 行（7285, 7286）
--
-- 4.5 端到端：admin 重新登录后访问 /system/platformRoomTemplate/list 返回 200
--     platform_admin 重新登录后左侧菜单"原子资源管理"下不再出现"默认房间模板"
--     platform_admin 直接访问 /system/platformRoomTemplate/list 返回 403
--
-- ★执行后请重启后端；用户需退出重新登录刷新 Sa-Token 权限快照
