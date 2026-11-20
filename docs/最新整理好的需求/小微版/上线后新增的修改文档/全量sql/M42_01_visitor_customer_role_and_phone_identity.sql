-- =====================================================================
-- M42_01 游客扫码登录：身份锚点改为「手机号 + 顾客角色」（生产修复）
-- =====================================================================
-- 根因：H5 扫码登录（VisitorLoginController.issueVisitorSession/createTempVisitor）
--       ① 建 sys_user 时【从不写 sys_user_role】→ 游客落地人员无任何角色，权限链断裂；
--       ② 幂等复用按 temp_visitor_account(tenant_id + phone_last4) 定位，
--          且该表 UNIQUE KEY uk_tenant_phone=(tenant_id, phone_last4)，
--          同租户下两个不同手机号只要【后 4 位相同】即碰撞 → 后登录者被误复用为前者账号。
-- 修复（与 SysLoginService.registerByPhone 口径统一）：
--   ① 代码侧已改为：身份锚点=完整手机号（selectUserByPhonenumberAndTenant），
--      新建/复用均【固定】确保 customer(9207) 顾客角色（仅当用户无任何角色时补挂，不污染 boss/salesman）；
--   ② 本脚本：temp_visitor_account 唯一键 (tenant_id, phone_last4) → (tenant_id, phone)；
--      并为存量「有临时账号但无角色」的游客补挂顾客角色 9207。
-- ★幂等：DDL 用 information_schema 守卫；数据修复用 INSERT IGNORE / WHERE NOT EXISTS，可重复执行
-- ★前置：M31_05（temp_visitor_account 建表）、M15_07（customer 角色 9207）
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 历史 phone 空值回填（phone 列可空，加唯一键前必须先补非空占位，避免 NULL 冲突）
--    ★正常登录流程都写了完整手机号，此处仅兜底历史脏数据
-- ---------------------------------------------------------------------
UPDATE `temp_visitor_account`
   SET `phone` = CONCAT('LEGACY_', `id`)
 WHERE `phone` IS NULL OR `phone` = '';

-- ---------------------------------------------------------------------
-- 2. 去重：同 (tenant_id, phone) 仅保留最小 id（新身份口径下一个手机号一条临时账号）
--    ★legacy 按 phone_last4 复用，理论上不会产生同完整手机号重复，此为安全网
-- ---------------------------------------------------------------------
DELETE t1 FROM `temp_visitor_account` t1
  JOIN `temp_visitor_account` t2
    ON t1.`tenant_id` = t2.`tenant_id`
   AND t1.`phone` = t2.`phone`
   AND t1.`id` > t2.`id`;

-- ---------------------------------------------------------------------
-- 3. 删除旧唯一键 uk_tenant_phone=(tenant_id, phone_last4)（幂等守卫）
-- ---------------------------------------------------------------------
SET @idx_old := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temp_visitor_account' AND INDEX_NAME = 'uk_tenant_phone');
SET @sql_old := IF(@idx_old > 0,
  'ALTER TABLE `temp_visitor_account` DROP INDEX `uk_tenant_phone`', 'SELECT 1');
PREPARE stmt_old FROM @sql_old; EXECUTE stmt_old; DEALLOCATE PREPARE stmt_old;

-- ---------------------------------------------------------------------
-- 4. 新增唯一键 uk_tenant_phone_full=(tenant_id, phone)（幂等守卫）
-- ---------------------------------------------------------------------
SET @idx_new := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temp_visitor_account' AND INDEX_NAME = 'uk_tenant_phone_full');
SET @sql_new := IF(@idx_new = 0,
  'ALTER TABLE `temp_visitor_account` ADD UNIQUE KEY `uk_tenant_phone_full` (`tenant_id`, `phone`)', 'SELECT 1');
PREPARE stmt_new FROM @sql_new; EXECUTE stmt_new; DEALLOCATE PREPARE stmt_new;

-- ---------------------------------------------------------------------
-- 5. 存量游客补挂顾客角色（customer=9207）：有临时账号但 sys_user_role 无任何记录者
--    ★仅补「无角色」用户，已有角色（boss/salesman/customer）者不动，避免污染
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT tva.`user_id`, 9207
  FROM `temp_visitor_account` tva
  JOIN `sys_user` u ON u.`user_id` = tva.`user_id` AND u.`del_flag` = '0'
 WHERE NOT EXISTS (SELECT 1 FROM `sys_user_role` sur WHERE sur.`user_id` = tva.`user_id`);

-- ---------------------------------------------------------------------
-- 6. 验收查询（执行后人工核对）
-- ---------------------------------------------------------------------
-- 6.1 唯一键已切换为 (tenant_id, phone)
-- SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
--   FROM information_schema.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'temp_visitor_account'
--    AND INDEX_NAME IN ('uk_tenant_phone','uk_tenant_phone_full')
--  GROUP BY INDEX_NAME;
-- 预期：仅 uk_tenant_phone_full=tenant_id,phone 一行（旧 uk_tenant_phone 已删除）
--
-- 6.2 不应再有「有临时账号但无角色」的游客
-- SELECT COUNT(*) AS role_less_visitor
--   FROM temp_visitor_account tva
--   JOIN sys_user u ON u.user_id = tva.user_id AND u.del_flag = '0'
--  WHERE NOT EXISTS (SELECT 1 FROM sys_user_role sur WHERE sur.user_id = tva.user_id);
-- 预期：0
