-- =====================================================================
-- M15_26  boss 克隆角色菜单清理：移除残留的 platform_admin 菜单（v1.42，2026-09-19）
-- =====================================================================
-- 根因：历史修复脚本（M15_15/M15_16/M18_09 等）在补写 sys_role_menu 时，
--       部分 boss 克隆角色被错误关联了 platform_admin 菜单（7500~7509/7620~7639），
--       导致 boss 登录后侧边栏出现租户管理员菜单（版本升级/角色映射/人员管理/固定支出配置等），
--       违反 V3.7 五角色菜单骨架中 boss 与 platform_admin 完全隔离的原则。
-- 修复：
--   ① 删除 boss 克隆角色中属于 platform_admin 的菜单关联（7500~7509 管理三件套 + 7620~7639 配置菜单）；
--   ② 确保 boss 克隆角色拥有正确的 M 类型祖先目录 7100（小微运营）；
--   ③ 验收查询确认 boss 菜单不含 platform_admin 菜单。
-- 依据：《五角色菜单骨架_V3.7_最终完整版.md》§3 老板（经营者）/ §2 租户管理员
--       boss 菜单 = 7100~7162（小微运营）+ 7200~7220（小程序）+ 7170~7199（推广/评价/固定支出）
--       platform_admin 菜单 = 7500~7509（管理三件套）+ 7510~7516（原子资源）+ 7620~7639（配置）
-- 前置：[21]M15_06（菜单 7000~7499）+ [34]M15_17（7500~7509）+ [52]M18_09（7620~7639）
-- 幂等：DELETE ... WHERE EXISTS 幂等，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 删除 boss 克隆角色中残留的 platform_admin 菜单关联
-- ---------------------------------------------------------------------
-- 1.1 清理 7500~7509（platform_admin 管理三件套：版本升级/角色映射/人员管理/主题等）
DELETE FROM `sys_role_menu`
 WHERE `menu_id` BETWEEN 7500 AND 7509
   AND `role_id` IN (
       SELECT r.`role_id`
         FROM `sys_tenant` t
         JOIN `sys_role` r ON r.`tenant_id` = t.`id`
        WHERE t.`tenant_type` = 'micro'
          AND r.`role_key` = 'boss'
          AND r.`del_flag` = '0'
   );

-- 1.2 清理 7510~7519（platform_admin 原子资源管理：项目/房间/角色/员工）
DELETE FROM `sys_role_menu`
 WHERE `menu_id` BETWEEN 7510 AND 7519
   AND `role_id` IN (
       SELECT r.`role_id`
         FROM `sys_tenant` t
         JOIN `sys_role` r ON r.`tenant_id` = t.`id`
        WHERE t.`tenant_type` = 'micro'
          AND r.`role_key` = 'boss'
          AND r.`del_flag` = '0'
   );

-- 1.3 清理 7620~7639（platform_admin 固定支出配置/周期规则/预约策略）
DELETE FROM `sys_role_menu`
 WHERE `menu_id` BETWEEN 7620 AND 7639
   AND `role_id` IN (
       SELECT r.`role_id`
         FROM `sys_tenant` t
         JOIN `sys_role` r ON r.`tenant_id` = t.`id`
        WHERE t.`tenant_type` = 'micro'
          AND r.`role_key` = 'boss'
          AND r.`del_flag` = '0'
   );

-- ---------------------------------------------------------------------
-- 2. 确保 boss 克隆角色拥有 M 类型祖先目录 7100（小微运营）
-- ---------------------------------------------------------------------
-- 7100 perms 为空，不会被 role_template.menu_keys→perms 匹配，
-- 但 selectMenuTreeByUserId 依赖 sys_role_menu 找到根节点才能组装完整菜单树。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7100
  FROM `sys_tenant` t
  JOIN `sys_role` r ON r.`tenant_id` = t.`id`
 WHERE t.`tenant_type` = 'micro'
   AND r.`role_key` = 'boss'
   AND r.`del_flag` = '0'
   AND NOT EXISTS (
       SELECT 1 FROM `sys_role_menu` rm
        WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 7100
   );

-- ---------------------------------------------------------------------
-- 3. 验收查询
-- ---------------------------------------------------------------------
-- A. boss 克隆角色不含 platform_admin 菜单（7500~7519/7620~7639）
-- SELECT r.role_id, t.company_name, rm.menu_id
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--    AND (rm.menu_id BETWEEN 7500 AND 7519 OR rm.menu_id BETWEEN 7620 AND 7639);
--   -- 预期：0 行
--
-- B. boss 克隆角色拥有正确的菜单（7100 系列 + 7200 系列等）
-- SELECT t.company_name, r.role_id, COUNT(rm.menu_id) AS boss_menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.company_name, r.role_id;
--   -- 预期：boss_menu_count >= 10（7100+7101+7110~7162 等）
--
-- C. M 类型目录 7100 已关联
-- SELECT COUNT(*) AS missing_7100
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--  WHERE t.tenant_type = 'micro'
--    AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = r.role_id AND rm.menu_id = 7100);
--   -- 预期：0
