-- =====================================================================
-- 新增租户状态字典 sys_tenant_status（版本升级/租户管理等页面状态中文回显）
-- 幂等脚本：字典类型已存在时跳过插入
-- 状态枚举对齐 sys_tenant.status：PENDING/NORMAL/GRACE/DISABLED/EXPIRED
-- =====================================================================

SET NAMES utf8mb4;

-- 1. 字典类型（已存在则跳过）
SET @type_exists = (SELECT COUNT(*) FROM sys_dict_type WHERE dict_type = 'sys_tenant_status');
SET @next_dict_id = (SELECT IFNULL(MAX(dict_id), 0) + 1 FROM sys_dict_type);
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, create_by, create_time, remark)
SELECT @next_dict_id, '租户状态', 'sys_tenant_status', '0', 'admin', NOW(), '租户状态列表（PENDING/NORMAL/GRACE/DISABLED/EXPIRED）'
FROM DUAL WHERE @type_exists = 0;

-- 2. 字典数据（随字典类型一并初始化，已存在则跳过）
SET @data_exists = (SELECT COUNT(*) FROM sys_dict_data WHERE dict_type = 'sys_tenant_status');
SET @next_code = (SELECT IFNULL(MAX(dict_code), 0) + 1 FROM sys_dict_data);
INSERT INTO sys_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT * FROM (
    SELECT @next_code     AS dict_code, 1 AS dict_sort, '待启用' AS dict_label, 'PENDING'  AS dict_value, 'sys_tenant_status' AS dict_type, '' AS css_class, 'info'    AS list_class, 'N' AS is_default, '0' AS status, 'admin' AS create_by, NOW() AS create_time, '租户已创建待启用' AS remark
    UNION ALL SELECT @next_code + 1, 2, '正常',   'NORMAL',   'sys_tenant_status', '', 'success', 'Y', '0', 'admin', NOW(), '租户正常使用'
    UNION ALL SELECT @next_code + 2, 3, '宽限期', 'GRACE',    'sys_tenant_status', '', 'warning', 'N', '0', 'admin', NOW(), '到期后只读宽限期内（T14.2）'
    UNION ALL SELECT @next_code + 3, 4, '已停用', 'DISABLED', 'sys_tenant_status', '', 'danger',  'N', '0', 'admin', NOW(), '租户被平台停用'
    UNION ALL SELECT @next_code + 4, 5, '已到期', 'EXPIRED',  'sys_tenant_status', '', 'danger',  'N', '0', 'admin', NOW(), '租户授权已到期'
) t
WHERE @data_exists = 0;

SELECT 'sys_tenant_status 租户状态字典初始化完成' AS result;
