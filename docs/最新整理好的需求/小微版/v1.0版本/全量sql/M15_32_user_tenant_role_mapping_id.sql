-- =====================================================================
-- M15_32  user_tenant_role 加 role_mapping_id 列（记录所选业务角色映射行）
-- =====================================================================
-- 需求概述：
--   [134]M15_31 后 tenant_role_mapping 允许同 role_code 并存多个业务角色名行
--   （旅游 salesman → 导游/司机），「人员管理」新增/修改弹窗的角色下拉应按
--   映射行呈现（4 项：旅行社社长/导游/游客/司机），而非硬编码 3 个标准角色。
--   但 user_tenant_role 仅存 role_code，无法区分"此人是导游还是司机"，
--   保存后列表/回显统一回退 role_code 首个显示名（导游）。本脚本新增
--   role_mapping_id 记录分配时选中的 tenant_role_mapping.id；显示侧
--   （UserTenantRoleServiceImpl#fillDisplay）优先取该映射显示名，空则回退原口径。
--
-- 变更：
--   1. user_tenant_role 加列 role_mapping_id BIGINT NULL（INFORMATION_SCHEMA 守卫幂等）。
--   2. 存量回填：role_mapping_id 为空的行按 (tenant_id, role_code) 取
--      (sort_order, id) 升序首个映射行 id 回填（仅补 NULL，幂等）。
--   3. ★无菜单/权限/开关变更；执行后需重启后端使配套代码生效。
--
-- 前置：
--   [134]M15_31（同 role_code 多业务角色名）
--
-- 幂等：加列带 INFORMATION_SCHEMA 守卫；回填 UPDATE 带 IS NULL 守卫，可重复执行
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 加列 role_mapping_id（幂等守卫）
-- ---------------------------------------------------------------------
SELECT COUNT(*) INTO @col_exists FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_tenant_role'
   AND COLUMN_NAME = 'role_mapping_id';
SET @ddl = IF(@col_exists = 0,
  'ALTER TABLE `user_tenant_role` ADD COLUMN `role_mapping_id` BIGINT NULL DEFAULT NULL COMMENT ''所选租户角色映射ID（tenant_role_mapping.id，空则回退 role_code 首个显示名）'' AFTER `role_code`',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------
-- 2. 存量回填：role_mapping_id 为空的行补 (tenant_id, role_code) 首个映射行
--    首端口径与 fillDisplay 一致：sort_order, id 升序；仅补 NULL，幂等
-- ---------------------------------------------------------------------
UPDATE `user_tenant_role` utr
   SET utr.`role_mapping_id` = (
        SELECT m.`id` FROM `tenant_role_mapping` m
         WHERE m.`tenant_id` = utr.`tenant_id`
           AND m.`role_code` = utr.`role_code`
           AND m.`del_flag` = '0'
         ORDER BY m.`sort_order`, m.`id`
         LIMIT 1)
 WHERE utr.`role_mapping_id` IS NULL
   AND utr.`del_flag` = '0'
   AND EXISTS (SELECT 1 FROM `tenant_role_mapping` x
                WHERE x.`tenant_id` = utr.`tenant_id`
                  AND x.`role_code` = utr.`role_code`
                  AND x.`del_flag` = '0');

-- ---------------------------------------------------------------------
-- 3. 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 3.1 列已存在
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_tenant_role'
--    AND COLUMN_NAME = 'role_mapping_id';
-- 预期：role_mapping_id | bigint | COMMENT 含 'tenant_role_mapping.id'
--
-- 3.2 存量分配均已命中映射行（预期 0 行）
-- SELECT COUNT(*) AS unmapped FROM user_tenant_role utr
--  WHERE utr.del_flag = '0' AND utr.role_mapping_id IS NULL
--    AND EXISTS (SELECT 1 FROM tenant_role_mapping m
--                 WHERE m.tenant_id = utr.tenant_id AND m.role_code = utr.role_code
--                   AND m.del_flag = '0');
--
-- 3.3 抽查 salesman 用户指向首映射行（旅游租户预期 display_name=导游）
-- SELECT utr.user_id, utr.role_code, utr.role_mapping_id, m.display_name
--   FROM user_tenant_role utr
--   LEFT JOIN tenant_role_mapping m ON m.id = utr.role_mapping_id
--  WHERE utr.role_code = 'salesman' AND utr.del_flag = '0' LIMIT 10;
--
-- 3.4 幂等复核：重复执行本脚本后 3.2/3.3 结果不变
