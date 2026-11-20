SET NAMES utf8mb4;

UPDATE sys_tenant
SET version_type = 'micro_pro',
    edition = 'MICRO',
    update_by = 'fix-version-type'
WHERE id = 2100599326265475074;

INSERT INTO sys_tenant_version_log (tenant_id, from_edition, to_edition, change_type, operator, remark, create_time)
VALUES (2100599326265475074, 'micro_lite', 'micro_pro', 'UPGRADE', 'fix-script', 'fix version_type micro_lite to micro_pro', NOW());

SELECT id, company_name, version_type, edition, status FROM sys_tenant WHERE id = 2100599326265475074;
