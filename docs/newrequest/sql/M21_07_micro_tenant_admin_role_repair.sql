-- =====================================================================
-- M21_07 小微版租户管理员（platform_admin）角色链路丢失修复（v1.0，2026-10-10）
-- =====================================================================
-- 现象：AIA桌游店（tenant=2100599326265475074）管理员账号 aia 登录后，
--       首页落到企业版综合看板（「当前版本：MICRO」+ 解锁企业高管管控能力），
--       侧边栏菜单为空，未进入租户管理员页面（企业概览 tenant-overview）。
-- 根因：fix_boss_remove_tenant_admin_menu.sql（2026-10-10）Step 2.1 删除了所有
--       小微版用户在 sys_user_role 中的克隆角色关联，Step 2.2 仅按 user_tenant_role
--       回放；而租户管理员账号（sys_tenant.admin_user_id）的 platform_admin 关联
--       历史上只存在于 sys_user_role（insertTenant Step 5 / M15_27 Step 3a 写入），
--       user_tenant_role 中无记录 → 删后无回放 → 管理员变成「无角色用户」：
--       getInfo 返回 roles 为空 → 前端 index.vue 的 admin/boss/platform_admin/BASIC
--       四个分支全部不命中 → 兜底落到企业版综合看板（与租户产品线无关，表现为
--       「没有按租户区分开」）。
-- 修复：① 回补 admin_user_id → platform_admin 克隆角色的 sys_user_role 关联
--         （与 M15_27 Step 3a 同源逻辑）；
--       ② 为 admin_user_id 补 user_tenant_role 的 platform_admin 记录，使双表一致，
--         后续 syncSysUserRole / 双表同步脚本以 user_tenant_role 为准时不再误删；
--       ③ 配套 Java 修复：UserTenantRoleServiceImpl.syncSysUserRole 改为按
--         「user_tenant_role 全量启用角色」重写 sys_user_role，而非仅本次传入 codes。
-- 前置依赖：M15_27（克隆角色补建）、fix_boss_remove_tenant_admin_menu（已执行）
-- 幂等：INSERT IGNORE + NOT EXISTS，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Step 1: 回补小微版租户管理员的 platform_admin 克隆角色（sys_user_role）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT t.`admin_user_id`, r.`role_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id`
  AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
WHERE t.`tenant_type` = 'micro'
  AND t.`admin_user_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `sys_user_role` ur
      WHERE ur.`user_id` = t.`admin_user_id` AND ur.`role_id` = r.`role_id`
  );

-- ---------------------------------------------------------------------
-- Step 2: 补 user_tenant_role 的 platform_admin 记录（双表一致，防再被同步误删）
--         is_primary：仅当该用户在本租户无其它主角色时置 1
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `user_tenant_role`
    (`tenant_id`, `user_id`, `role_code`, `is_primary`, `status`, `del_flag`,
     `create_by`, `create_time`, `remark`)
SELECT t.`id`, t.`admin_user_id`, 'platform_admin',
       IF(pri.`cnt` > 0, 0, 1),
       '0', '0', 'system', NOW(), '租户管理员固有角色（M21_07 回补）'
FROM `sys_tenant` t
LEFT JOIN (
    SELECT x.`tenant_id`, x.`user_id`, COUNT(*) AS `cnt`
    FROM `user_tenant_role` x
    WHERE x.`is_primary` = 1 AND x.`del_flag` = '0'
    GROUP BY x.`tenant_id`, x.`user_id`
) pri ON pri.`tenant_id` = t.`id` AND pri.`user_id` = t.`admin_user_id`
WHERE t.`tenant_type` = 'micro'
  AND t.`admin_user_id` IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM `user_tenant_role` u
      WHERE u.`tenant_id` = t.`id` AND u.`user_id` = t.`admin_user_id`
        AND u.`role_code` = 'platform_admin' AND u.`del_flag` = '0'
  );

-- ---------------------------------------------------------------------
-- 验收查询（执行后务必核对）
-- ---------------------------------------------------------------------
-- A. 每个小微版租户管理员的 platform_admin 双表链接均存在
-- SELECT t.id, t.company_name, t.admin_user_id, u.user_name,
--        EXISTS(SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
--               WHERE ur.user_id = t.admin_user_id
--                 AND r.role_key = 'platform_admin' AND r.del_flag = '0') AS has_sur,
--        EXISTS(SELECT 1 FROM user_tenant_role x
--               WHERE x.tenant_id = t.id AND x.user_id = t.admin_user_id
--                 AND x.role_code = 'platform_admin' AND x.del_flag = '0') AS has_utr
--   FROM sys_tenant t
--   JOIN sys_user u ON u.user_id = t.admin_user_id
--  WHERE t.tenant_type = 'micro';
--   -- 预期：每行 has_sur=1 且 has_utr=1
--
-- B. 模拟 aia 登录角色解析（selectRolePermissionByUserId 同源）
-- SELECT r.role_key
--   FROM sys_user_role sur
--   JOIN sys_role r ON r.role_id = sur.role_id AND r.del_flag = '0'
--   JOIN sys_user u ON u.user_id = sur.user_id
--  WHERE u.user_name = 'aia';
--   -- 预期：含 platform_admin（前端据此进入租户管理员首页 tenant-overview）
--
-- C. 双表角色数一致（沿用 fix_boss_remove_tenant_admin_menu 验收 D）
-- SELECT u.user_name,
--   (SELECT COUNT(*) FROM sys_user_role sur JOIN sys_role r ON r.role_id = sur.role_id
--     JOIN sys_tenant t ON t.id = r.tenant_id
--    WHERE sur.user_id = u.user_id AND t.tenant_type = 'micro' AND r.del_flag = '0') AS sur_cnt,
--   (SELECT COUNT(*) FROM user_tenant_role utr
--    WHERE utr.user_id = u.user_id AND utr.del_flag = '0' AND utr.status = '0') AS utr_cnt
--   FROM sys_user u
--   JOIN sys_tenant t ON t.id = u.tenant_id AND t.tenant_type = 'micro'
--  HAVING sur_cnt != utr_cnt;
--   -- 预期：0 行
--
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照！
