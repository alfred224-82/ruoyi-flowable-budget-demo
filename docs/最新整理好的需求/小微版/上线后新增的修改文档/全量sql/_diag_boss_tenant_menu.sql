-- =====================================================================
-- 诊断：boss 角色为何仍显示「租户管理菜单」（7500）
-- =====================================================================
SET NAMES utf8mb4;

-- D1: 查看当前登录用户（替换为你的用户名）的角色关联
-- ★把 'lijie' 替换为实际登录的 boss 用户名
SELECT u.user_id, u.user_name, r.role_id, r.role_key, r.role_name, r.tenant_id
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.user_id
JOIN sys_role r ON r.role_id = ur.role_id
WHERE u.user_name = 'lijie'
  AND r.del_flag = '0';

-- D2: 查看该用户所有角色的菜单（重点看 7500~7509）
SELECT r.role_id, r.role_key, rm.menu_id, m.menu_name, m.menu_type, m.parent_id
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.user_id
JOIN sys_role r ON r.role_id = ur.role_id
JOIN sys_role_menu rm ON rm.role_id = r.role_id
LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE u.user_name = 'lijie'
  AND r.del_flag = '0'
  AND rm.menu_id BETWEEN 7500 AND 7509
ORDER BY r.role_key, rm.menu_id;

-- D3: 所有小微版 boss 克隆角色是否仍持有 7500~7509
SELECT t.id AS tenant_id, t.company_name, r.role_id, r.role_key,
       GROUP_CONCAT(rm.menu_id ORDER BY rm.menu_id) AS menu_7500_ids
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
JOIN sys_role_menu rm ON rm.role_id = r.role_id
WHERE t.tenant_type = 'micro'
  AND rm.menu_id BETWEEN 7500 AND 7509
GROUP BY t.id, t.company_name, r.role_id, r.role_key;

-- D4: 所有小微版 platform_admin 克隆角色的 7500 关联（确认 platform_admin 正常）
SELECT t.id AS tenant_id, t.company_name, r.role_id, r.role_key,
       GROUP_CONCAT(rm.menu_id ORDER BY rm.menu_id) AS menu_7500_ids
FROM sys_tenant t
JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin' AND r.del_flag = '0'
JOIN sys_role_menu rm ON rm.role_id = r.role_id
WHERE t.tenant_type = 'micro'
  AND rm.menu_id BETWEEN 7500 AND 7509
GROUP BY t.id, t.company_name, r.role_id, r.role_key;

-- D5: 查看该用户通过 selectMenuTreeByUserId 会拿到哪些 M/C 菜单（模拟前端渲染）
SELECT DISTINCT m.menu_id, m.menu_name, m.menu_type, m.parent_id, m.perms
FROM sys_user u
JOIN sys_user_role ur ON ur.user_id = u.user_id
JOIN sys_role r ON r.role_id = ur.role_id AND r.status = '0'
JOIN sys_role_menu rm ON rm.role_id = r.role_id
JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.status = '0'
WHERE u.user_name = 'lijie'
  AND m.menu_type IN ('M', 'C')
ORDER BY m.menu_id;
