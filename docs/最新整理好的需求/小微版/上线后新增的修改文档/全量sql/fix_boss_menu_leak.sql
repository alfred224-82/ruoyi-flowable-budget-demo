-- =====================================================================
-- fix_boss_menu_leak.sql
-- 修复 boss 角色菜单泄漏：清理 7100（小微运营）树之外的所有菜单关联
-- =====================================================================
-- 根因：PROD_fix_all_role_menus.sql 步骤 1.3 只清理了 7000~7099 和
--       7500~7699 区间，漏掉了 menu_id=1（系统管理）和 menu_id=7400（小程序·顾客）
--       等不在这些区间内的根级菜单，导致 boss 侧边栏出现多余菜单。
-- ★铁律：boss 角色仅保留 7100 子树内的菜单
-- ★幂等：可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- =====================================================================
-- 1. 查看修复前 boss 角色的根级菜单（parent_id=0）
-- =====================================================================
SELECT '=== 修复前：boss 角色根级菜单 ===' AS info;
SELECT rm.menu_id, m.menu_name, m.parent_id
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.menu_id
JOIN sys_role r ON rm.role_id = r.role_id
WHERE r.role_key = 'boss' AND r.del_flag = '0' AND r.tenant_id IS NOT NULL
  AND m.menu_type = 'M' AND m.parent_id = 0
ORDER BY rm.menu_id;

-- =====================================================================
-- 2. 清理 boss 角色 7100 树之外的所有菜单
--    策略：先收集 7100 子树内所有 menu_id，再删除不在其中的记录
-- =====================================================================

-- 2.1 创建临时表存储 7100 子树的所有 menu_id
DROP TEMPORARY TABLE IF EXISTS tmp_boss_7100_tree;
CREATE TEMPORARY TABLE tmp_boss_7100_tree (menu_id BIGINT PRIMARY KEY);

-- 2.2 先放入 7100 自身
INSERT INTO tmp_boss_7100_tree VALUES (7100);

-- 2.3 递归收集 7100 的所有后代菜单
INSERT INTO tmp_boss_7100_tree
WITH RECURSIVE tree AS (
    SELECT menu_id FROM sys_menu WHERE menu_id = 7100
    UNION ALL
    SELECT m.menu_id FROM sys_menu m JOIN tree t ON m.parent_id = t.menu_id
)
SELECT menu_id FROM tree WHERE menu_id != 7100;

-- 2.4 删除 boss 角色不在 7100 子树内的所有菜单关联
DELETE rm FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.role_id
WHERE r.role_key = 'boss' AND r.del_flag = '0' AND r.tenant_id IS NOT NULL
  AND rm.menu_id NOT IN (SELECT menu_id FROM tmp_boss_7100_tree);

-- 2.5 清理临时表
DROP TEMPORARY TABLE IF EXISTS tmp_boss_7100_tree;

-- =====================================================================
-- 3. 验证：修复后 boss 角色的根级菜单应仅剩 7100
-- =====================================================================
SELECT '=== 修复后：boss 角色根级菜单（应仅 7100）===' AS info;
SELECT rm.menu_id, m.menu_name, m.parent_id
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.menu_id
JOIN sys_role r ON rm.role_id = r.role_id
WHERE r.role_key = 'boss' AND r.del_flag = '0' AND r.tenant_id IS NOT NULL
  AND m.menu_type = 'M' AND m.parent_id = 0
ORDER BY rm.menu_id;

-- 3.1 验证：boss 角色菜单总数（应 = 7100 子树内的菜单数）
SELECT '=== 修复后：boss 角色菜单总数 ===' AS info;
SELECT COUNT(*) AS total_menus FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.role_id
WHERE r.role_key = 'boss' AND r.del_flag = '0' AND r.tenant_id IS NOT NULL;

-- 3.2 验证：确认没有 7100 树之外的菜单
SELECT '=== 修复后：7100 树外菜单数（应为 0）===' AS info;
SELECT COUNT(*) AS leak_count FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.role_id
JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE r.role_key = 'boss' AND r.del_flag = '0' AND r.tenant_id IS NOT NULL
  AND m.menu_id != 7100
  AND m.parent_id != 7100
  AND m.parent_id NOT IN (SELECT menu_id FROM sys_menu WHERE parent_id = 7100);

SET SQL_SAFE_UPDATES = 1;
