-- =====================================================================
-- M31_03_boss_room_layout_menu_hide.sql
-- 修复：Boss 侧边栏不应显示「房间布局配置」菜单（7280）
-- =====================================================================
-- 根因：
--   M26_02 将菜单 7280（房间布局配置 C，parent=7100 boss 专属目录）
--   从 visible='1'（隐藏）改为 visible='0'（显示），导致 Boss 侧边栏出现该菜单。
--   但房间布局配置为 platform_admin 功能（平行菜单 7291 挂 7510），Boss 不应看到。
--
-- 修复：
--   1. sys_menu 7280 visible='0' → '1'（隐藏，Boss 侧边栏不再显示）
--   2. 同步修正 M26_02 源脚本，防止重新执行时再次置显
--
-- ★幂等：仅当当前 visible='0' 时更新
-- ★执行后请退出重新登录，刷新菜单缓存
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 菜单 7280 改回隐藏
UPDATE `sys_menu` SET `visible` = '1' WHERE `menu_id` = 7280 AND `visible` = '0';

-- ----------------------------
-- 验收查询
-- ----------------------------
-- 1.1 菜单 7280 已隐藏（预期 visible='1'）
-- SELECT menu_id, menu_name, parent_id, visible FROM sys_menu WHERE menu_id = 7280;
-- 预期：7280 | 房间布局配置 | 7100 | 1
--
-- 1.2 platform_admin 平行菜单 7291 不受影响
-- SELECT menu_id, menu_name, parent_id, visible FROM sys_menu WHERE menu_id = 7291;
-- 预期：7291 | 房间布局配置 | 7510 | 0（platform_admin 可见）
