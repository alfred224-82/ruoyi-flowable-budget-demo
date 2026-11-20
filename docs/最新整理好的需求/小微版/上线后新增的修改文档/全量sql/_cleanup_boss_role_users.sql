-- =====================================================================
-- 临时清理脚本：删除所有小微版租户的 boss 克隆角色及其用户关联
-- 日期：2026-09-19
-- 用途：用户要求清除所有 boss 角色的人员，重新建人员
-- ★注意：此脚本会删除 boss 克隆角色、角色分配、菜单关联，不可逆！
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 先查看受影响的数据（执行前先确认）
-- ---------------------------------------------------------------------
-- 1.1 查看当前 boss 克隆角色
SELECT r.role_id, r.tenant_id, r.role_name, r.role_key,
       t.company_name, t.tenant_type
  FROM sys_role r
  JOIN sys_tenant t ON r.tenant_id = t.id
 WHERE r.role_key = 'boss'
   AND r.tenant_id IS NOT NULL
   AND t.tenant_type = 'micro'
   AND r.del_flag = '0';

-- 1.2 查看 boss 角色关联的用户
SELECT ur.user_id, u.user_name, u.nick_name, u.tenant_id, ur.role_id
  FROM sys_user_role ur
  JOIN sys_role r ON ur.role_id = r.role_id
  JOIN sys_user u ON ur.user_id = u.user_id
 WHERE r.role_key = 'boss'
   AND r.tenant_id IS NOT NULL
   AND r.del_flag = '0';

-- 1.3 查看 boss 克隆角色的菜单关联数
SELECT r.role_id, r.tenant_id, COUNT(rm.menu_id) AS menu_count
  FROM sys_role r
  JOIN sys_tenant t ON r.tenant_id = t.id
  LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
 WHERE r.role_key = 'boss'
   AND r.tenant_id IS NOT NULL
   AND t.tenant_type = 'micro'
   AND r.del_flag = '0'
 GROUP BY r.role_id, r.tenant_id;

-- ---------------------------------------------------------------------
-- 2. 删除 boss 角色的用户关联（sys_user_role）
-- ---------------------------------------------------------------------
DELETE FROM sys_user_role
 WHERE role_id IN (
    SELECT r.role_id
      FROM sys_role r
      JOIN sys_tenant t ON r.tenant_id = t.id
     WHERE r.role_key = 'boss'
       AND r.tenant_id IS NOT NULL
       AND t.tenant_type = 'micro'
       AND r.del_flag = '0'
 );

-- ---------------------------------------------------------------------
-- 3. 删除 boss 克隆角色的菜单关联（sys_role_menu）
-- ---------------------------------------------------------------------
DELETE FROM sys_role_menu
 WHERE role_id IN (
    SELECT r.role_id
      FROM sys_role r
      JOIN sys_tenant t ON r.tenant_id = t.id
     WHERE r.role_key = 'boss'
       AND r.tenant_id IS NOT NULL
       AND t.tenant_type = 'micro'
       AND r.del_flag = '0'
 );

-- ---------------------------------------------------------------------
-- 4. 删除 boss 克隆角色本身（sys_role）
-- ---------------------------------------------------------------------
DELETE FROM sys_role
 WHERE role_key = 'boss'
   AND tenant_id IS NOT NULL
   AND del_flag = '0';

-- ---------------------------------------------------------------------
-- 5. 验证清理结果
-- ---------------------------------------------------------------------
-- 5.1 boss 克隆角色应全部删除
SELECT COUNT(*) AS remaining_boss_roles
  FROM sys_role r
  JOIN sys_tenant t ON r.tenant_id = t.id
 WHERE r.role_key = 'boss'
   AND r.tenant_id IS NOT NULL
   AND t.tenant_type = 'micro'
   AND r.del_flag = '0';
-- 预期：0

-- 5.2 boss 用户关联应全部清除
SELECT COUNT(*) AS remaining_boss_user_roles
  FROM sys_user_role ur
  JOIN sys_role r ON ur.role_id = r.role_id
 WHERE r.role_key = 'boss'
   AND r.tenant_id IS NOT NULL;
-- 预期：0

-- 5.3 共享预设角色 boss（tenant_id=NULL）不受影响
SELECT role_id, role_name, role_key, tenant_id
  FROM sys_role WHERE role_key = 'boss' AND tenant_id IS NULL;
-- 预期：1 行（9201, boss, boss, NULL）
