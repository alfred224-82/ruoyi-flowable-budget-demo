SELECT industry_code, biz_role_name, role_code, default_flag, sort_order FROM industry_role_map WHERE industry_code='spa' ORDER BY sort_order;
SELECT '--- standard micro roles ---' AS info;
SELECT role_id, role_name, role_key, tenant_id, tenant_type_scope, status FROM sys_role WHERE role_key IN ('boss','salesman','customer') ORDER BY role_id;
SELECT '--- industry_role_map dup role_code per industry ---' AS info;
SELECT industry_code, role_code, COUNT(*) c FROM industry_role_map GROUP BY industry_code, role_code HAVING c > 1;
