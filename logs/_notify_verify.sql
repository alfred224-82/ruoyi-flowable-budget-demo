-- 验证1: micro_lite boss 配方已补 notify:list
SELECT version_type, role_code,
       JSON_CONTAINS(menu_keys, JSON_QUOTE('micro:notify:list')) AS has_notify_list
FROM role_template WHERE version_type='micro_lite' AND role_code='boss';

-- 验证2: 杰杰旅游社 boss 角色已授 7220
SELECT r.role_id, r.role_key, r.tenant_id, rm.menu_id
FROM sys_role r
JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = 7220
WHERE r.tenant_id = 2109653805417828354 AND r.role_key = 'boss';

-- 验证3: 全体小微版租户 boss 角色缺漏复查（应为空）
SELECT r.role_id, r.tenant_id
FROM sys_role r
JOIN sys_tenant t ON t.id = r.tenant_id AND t.tenant_type = 'micro'
WHERE r.role_key = 'boss' AND r.del_flag = '0'
  AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = 7220);
