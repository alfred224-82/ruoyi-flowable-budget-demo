-- =====================================================================
-- M20_02_booking_page_permission_and_menu_fix.sql  v1.0  (2026-09-26)
-- 修复预约列表页权限 403 + 订单详情菜单隐藏
-- =====================================================================
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
-- 修复内容：
--   1. 订单详情菜单（7116）visible 改为 '1'（隐藏），不显示在侧边栏
--      （订单详情仅通过预约列表「查看」按钮 router.push 跳转访问）
--   2. 配套 Java 修复（本脚本不涉及，由代码层完成）：
--      ① ServiceItemController.onSale() → SaMode.OR(micro:service:list, micro:booking:list)
--      ② UserRoleController 新增 /staff 端点 → SaMode.OR(micro:booking:list, micro:settle:list)
--      ③ 前端 booking/settle 页面改调 listStaff 替代 listUserRole
-- =====================================================================

SET NAMES utf8mb4;

-- ─── 1. 订单详情菜单隐藏（visible: '0'→'1'） ─────────────────────
-- menu_id=7116, parent=7110(预约/订单管理,M), 原 visible='0' 导致出现在侧边栏
-- 改为 visible='1'（隐藏），仅通过 router.push('/micro/booking/detail') 访问
UPDATE `sys_menu`
SET `visible` = '1'
WHERE `menu_id` = 7116
  AND `visible` = '0';

-- ── 2. 同步清理存量 sys_role_menu 中 7116 的关联（可选，隐藏菜单保留授权无副作用）──
-- 隐藏菜单保留在 sys_role_menu 中不影响功能，此处不做清理（幂等安全）

-- =====================================================================
-- 验收查询
-- =====================================================================
-- A. 订单详情菜单已隐藏
-- SELECT menu_id, menu_name, visible FROM sys_menu WHERE menu_id = 7116;
-- 预期：visible = '1'
--
-- B. 侧边栏不再出现「订单详情」
-- SELECT menu_id, menu_name, parent_id, visible FROM sys_menu
--  WHERE parent_id = 7110 AND visible = '0';
-- 预期：仅 7111（预约列表），不含 7116
