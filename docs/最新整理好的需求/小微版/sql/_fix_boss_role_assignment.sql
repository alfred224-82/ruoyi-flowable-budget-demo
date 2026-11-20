-- =====================================================================
-- 紧急修复：老板用户被错误分配 platform_admin 角色
-- 版本：v1.0  日期：2026-09-19
-- 根因：用户在 sys_user_role 中被错误关联到 platform_admin 克隆角色，
--       而非 boss 克隆角色，导致登录看到平台管理菜单而非经营驾驶舱。
-- 修复逻辑：
--   Step 1: 删除错误的 platform_admin 角色关联
--   Step 2: 补建正确的 boss 角色关联
--   Step 3: 验证菜单树恢复正常
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 删除错误的 platform_admin 角色关联
-- =====================================================================
-- 找到所有属于小微版租户且角色为 platform_admin 的用户，
-- 如果该用户应该是 boss 角色（通过 sys_tenant.admin_user_id 判断），则删除错误关联

DELETE FROM sys_user_role
WHERE role_id IN (
    SELECT r.role_id
    FROM sys_role r
    JOIN sys_tenant t ON r.tenant_id = t.id
    WHERE r.role_key = 'platform_admin'
      AND t.tenant_type = 'micro'
      AND t.admin_user_id IS NOT NULL
)
AND user_id IN (
    SELECT t.admin_user_id
    FROM sys_tenant t
    WHERE t.tenant_type = 'micro'
      AND t.admin_user_id IS NOT NULL
);

-- =====================================================================
-- Step 2: 补建正确的 boss 角色关联
-- =====================================================================
-- 将小微版租户的 admin_user_id 关联到该租户的 boss 克隆角色

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT 
    t.admin_user_id AS user_id,
    r.role_id
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id
WHERE t.tenant_type = 'micro'
  AND t.admin_user_id IS NOT NULL
  AND r.role_key = 'boss'
  AND r.del_flag = '0'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_role ur
      WHERE ur.user_id = t.admin_user_id
        AND ur.role_id = r.role_id
  );

-- =====================================================================
-- Step 3: 验证查询
-- =====================================================================
-- A. 检查 admin_user 是否已关联到 boss 角色（而非 platform_admin）
SELECT 
    t.company_name,
    u.user_name,
    r.role_key,
    COUNT(rm.menu_id) AS menu_count
FROM sys_tenant t
JOIN sys_user u ON u.user_id = t.admin_user_id
JOIN sys_user_role ur ON ur.user_id = u.user_id
JOIN sys_role r ON r.role_id = ur.role_id
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
WHERE t.tenant_type = 'micro'
  AND t.admin_user_id IS NOT NULL
GROUP BY t.company_name, u.user_name, r.role_key;
-- 预期：role_key = 'boss'，menu_count >= 10（含 7100 M目录 + 7101~7162 等）

-- B. 模拟 selectMenuTreeByUserId 验证菜单树完整性
SELECT COUNT(*) AS menu_count FROM (
    SELECT DISTINCT m.menu_id
    FROM sys_menu m
    JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
    JOIN sys_user_role sur ON rm.role_id = sur.role_id
    JOIN sys_role ro ON sur.role_id = ro.role_id
    JOIN sys_user u ON sur.user_id = u.user_id
    WHERE u.user_name = 'lijie'  -- 替换为实际用户名
      AND m.menu_type IN ('M', 'C')
      AND m.status = '0'
      AND ro.status = '0'
) sub;
-- 预期：> 0（应该有 10+ 个菜单，包含 7100/7101 等 boss 菜单）

-- C. 确认无 platform_admin 菜单泄漏
SELECT COUNT(*) AS admin_menu_leak
FROM sys_role_menu rm
JOIN sys_role r ON rm.role_id = r.role_id
JOIN sys_user_role ur ON ur.role_id = r.role_id
JOIN sys_user u ON ur.user_id = u.user_id
WHERE u.user_name = 'lijie'  -- 替换为实际用户名
  AND r.role_key = 'boss'
  AND rm.menu_id BETWEEN 7500 AND 7519;  -- platform_admin 菜单区段
-- 预期：0

-- ★修复后请退出并重新登录，刷新 Sa-Token 权限快照！
