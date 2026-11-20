-- =====================================================================
-- 修复: micro 租户用户缺少 salesman 角色分配 (T19.2 员工选择器数据源)
-- 问题: 发布活动"选择员工"下拉框无数据
-- 根因: 租户创建时 clonePresetRoles() 克隆了 salesman 角色到 sys_role,
--       但 sys_user_role 中没有将租户用户关联到该角色
-- 策略: 为每个 micro 租户下"无任何角色"的启用用户分配本租户的 salesman 角色
--       (已有 boss/platform_admin 等角色的用户不受影响, INSERT IGNORE 幂等)
-- =====================================================================

-- Step 1: 批量分配 - 所有 micro 租户下无角色的启用用户 -> salesman
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u
JOIN sys_tenant t ON u.tenant_id = t.id AND t.tenant_type = 'micro'
JOIN sys_role r ON r.role_key = 'salesman' AND r.tenant_id = t.id
WHERE u.status = '0'
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.user_id
  );

-- Step 2: 验收 - 每个 micro 租户的 salesman 用户数
SELECT t.id, t.company_name, t.tenant_type,
       COUNT(DISTINCT u.user_id) AS total_users,
       COUNT(DISTINCT CASE WHEN r.role_key = 'salesman' THEN u.user_id END) AS salesman_count
FROM sys_tenant t
JOIN sys_user u ON u.tenant_id = t.id AND u.status = '0'
LEFT JOIN sys_user_role ur ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON ur.role_id = r.role_id
WHERE t.tenant_type = 'micro'
GROUP BY t.id, t.company_name, t.tenant_type;
