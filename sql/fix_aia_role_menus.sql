SET NAMES utf8mb4;

SELECT 'before' AS step, COUNT(*) AS cnt FROM sys_role_menu WHERE role_id = 7100599326265475075;

DELETE FROM sys_role_menu WHERE role_id = 7100599326265475075 AND menu_id IN (
    SELECT menu_id FROM (
        SELECT menu_id FROM sys_menu WHERE menu_id = 1 OR parent_id = 1
        UNION
        SELECT menu_id FROM sys_menu WHERE parent_id IN (SELECT menu_id FROM sys_menu WHERE parent_id = 1)
    ) AS t
);

DELETE FROM sys_role_menu WHERE role_id = 7100599326265475075 AND menu_id IN (
    SELECT menu_id FROM (
        SELECT menu_id FROM sys_menu WHERE menu_id = 7200 OR parent_id = 7200
        UNION
        SELECT menu_id FROM sys_menu WHERE parent_id IN (SELECT menu_id FROM sys_menu WHERE parent_id = 7200)
    ) AS t
);

SELECT 'after' AS step, COUNT(*) AS cnt FROM sys_role_menu WHERE role_id = 7100599326265475075;

SELECT DISTINCT m.menu_id, m.menu_name, m.parent_id
FROM sys_role_menu rm
JOIN sys_menu m ON rm.menu_id = m.menu_id
WHERE rm.role_id = 7100599326265475075
AND m.parent_id = 0
ORDER BY m.menu_id;

SELECT 'done' AS result;
