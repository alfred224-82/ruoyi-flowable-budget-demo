SELECT version_type, role_code,
       JSON_CONTAINS(menu_keys, JSON_QUOTE('micro:notify:list')) AS has_notify_list,
       JSON_CONTAINS(menu_keys, JSON_QUOTE('micro:notify:config')) AS has_notify_config
FROM role_template
WHERE role_code IN ('boss','finance','manager','salesman') AND version_type LIKE 'micro%';

SELECT ur.role_id, r.role_key, u.user_id, u.user_name
FROM sys_user_role ur
JOIN sys_role r ON r.role_id = ur.role_id
JOIN sys_user u ON u.user_id = ur.user_id
WHERE r.tenant_id = 2109653805417828354;
