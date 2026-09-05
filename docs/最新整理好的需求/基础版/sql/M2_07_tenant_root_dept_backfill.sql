-- =============================================
-- M2_07 存量租户根部门（总公司）补建脚本
-- 背景：早期创建的租户在 sys_dept 中没有任何绑定 tenant_id 的部门，
--       导致租户管理员（如 mmadmin）在"部门管理-新增部门"时上级部门树为空，
--       无法新建"总公司"等组织节点。
-- 说明：脚本可重复执行（NOT EXISTS 幂等保护）；
--       也可不执行，后端 SysDeptServiceImpl 已支持租户用户查询部门时自动补建根部门。
-- =============================================

-- 为所有尚无任何有效部门的租户补建根部门"总公司"
INSERT INTO `sys_dept` (`dept_id`, `tenant_id`, `parent_id`, `ancestors`, `dept_name`,
                        `order_num`, `leader`, `phone`, `email`, `status`, `del_flag`,
                        `create_by`, `create_time`, `update_by`, `update_time`)
SELECT (UNIX_TIMESTAMP(NOW(3)) * 1000) + seq.rn,
       t.`id`, 0, '0', '总公司',
       0, NULL, NULL, NULL, '0', '0',
       'system', NOW(), NULL, NULL
FROM `sys_tenant` t
JOIN (
    SELECT `id`, ROW_NUMBER() OVER (ORDER BY `id`) AS rn
    FROM `sys_tenant`
) seq ON seq.`id` = t.`id`
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_dept` d
    WHERE d.`tenant_id` = t.`id` AND d.`del_flag` = '0'
);

-- 验证：每个租户都应至少有一个根部门（parent_id = 0）
SELECT t.`id`          AS tenant_id,
       t.`tenant_code`,
       t.`company_name`,
       d.`dept_id`     AS root_dept_id,
       d.`dept_name`   AS root_dept_name
FROM `sys_tenant` t
LEFT JOIN `sys_dept` d
       ON d.`tenant_id` = t.`id` AND d.`parent_id` = 0 AND d.`del_flag` = '0'
ORDER BY t.`id`;
