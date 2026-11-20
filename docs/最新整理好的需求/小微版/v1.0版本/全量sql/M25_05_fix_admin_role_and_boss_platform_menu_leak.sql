-- =====================================================================
-- M25_05 租户管理员角色错配修复 + boss 平台侧菜单泄漏清理（v1.84）
-- 版本：v1.0  日期：2026-10-12
-- ---------------------------------------------------------------------
-- 【问题背景】
--   1. _fix_boss_role_assignment.sql v1.0（全量 [79]）误把 sys_tenant.admin_user_id
--      当作老板账号：Step 1 删除了租户管理员的 platform_admin 关联、Step 2 反而把
--      boss 克隆角色挂给租户管理员；[80] M15_27 Step 3a 随后又为 admin_user_id 回补
--      platform_admin → 租户管理员变成「老板 + 租户组织管理员」双角色，登录后
--      侧边栏同时出现 boss 的「小微运营」与租户管理员的「租户管理菜单」。
--      ★正确设计（insertTenant Step 5 / M15_27 Step 3）：admin_user_id = 租户管理员
--        账号，专属 platform_admin 克隆角色；boss 克隆角色只属于非 admin 用户。
--   2. M25_01 v1.0 把门店侧投诉菜单 7261/7262（perms=micro:complaint:*，boss 配方
--      含此二键）挂在平台监管目录 7260（1300 资金平台管理菜单的子目录）下，且 §7.2
--      直接给 boss 授 7260/7000；叠加克隆/修复脚本的「M 类型祖先目录补全」
--      （copyMenuTemplateForMicro / M15_27 Step 2），boss 克隆角色沿
--      7261→7260→1300 祖先链拿到 1300 → 租户侧侧边栏出现平台专属目录。
--      （M25_04 仅清理 platform_admin 侧，boss 侧泄漏由本脚本收尾。）
-- 【修复内容】（全部幂等，可重复执行）
--   Step 1: 门店侧投诉菜单 7261 改挂 7100 小微运营（7262 仍挂 7261）；
--           7260 投诉监管仅保留平台监管页 7265/7270/7271（超管专属）。
--   Step 2: 除超管 role_id=1 外，全部角色清除 1300/6000 两棵子树授权
--           （含 7260/7000/7005/1310~1359/6120/6160 等，杜绝平台菜单泄漏到租户侧）。
--   Step 3: 祖先兜底：持有 7261 但缺 7100 的角色补授 7100 目录，防孤儿菜单。
--   Step 4: 用户-角色错配修复：
--           4a. 小微版租户 admin_user_id 移除 boss 克隆角色（修复 [79] v1.0 误挂）；
--           4b. admin_user_id 回补 platform_admin 克隆角色（兜底，同 M15_27 Step 3a）。
-- 【执行后必做】
--   1. 重启后端 ruoyi-admin（清除 Sa-Token 权限快照）
--   2. 租户管理员 / boss 退出重新登录
-- =====================================================================

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- ---------------------------------------------------------------------
-- Step 1: 门店侧投诉菜单 7261 改挂 7100 小微运营（守卫：仅当仍挂 7260 时更新）
-- ---------------------------------------------------------------------
UPDATE `sys_menu`
SET `parent_id` = 7100, `order_num` = 20
WHERE `menu_id` = 7261 AND `parent_id` = 7260;

-- ---------------------------------------------------------------------
-- Step 2: 非超管角色清除 1300/6000 整棵子树授权（递归 CTE，Step 1 之后 7261/7262 已不在子树内）
-- ---------------------------------------------------------------------
WITH RECURSIVE `pm` AS (
    SELECT `menu_id` FROM `sys_menu` WHERE `menu_id` IN (1300, 6000)
    UNION ALL
    SELECT m.`menu_id` FROM `sys_menu` m JOIN `pm` ON m.`parent_id` = `pm`.`menu_id`
)
DELETE rm FROM `sys_role_menu` rm
JOIN `pm` ON `pm`.`menu_id` = rm.`menu_id`
WHERE rm.`role_id` <> 1;

-- ---------------------------------------------------------------------
-- Step 3: 祖先兜底：持有 7261 却缺 7100 的角色补授 7100（防菜单树断裂）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT rm.`role_id`, 7100
FROM `sys_role_menu` rm
WHERE rm.`menu_id` = 7261
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` x
      WHERE x.`role_id` = rm.`role_id` AND x.`menu_id` = 7100
  );

-- ---------------------------------------------------------------------
-- Step 4a: 小微版租户 admin_user_id 移除 boss 克隆角色（修复 [79] v1.0 误挂）
-- ---------------------------------------------------------------------
DELETE ur FROM `sys_user_role` ur
JOIN `sys_role` r ON r.`role_id` = ur.`role_id`
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id` AND t.`tenant_type` = 'micro'
WHERE t.`admin_user_id` IS NOT NULL
  AND ur.`user_id` = t.`admin_user_id`;

-- ---------------------------------------------------------------------
-- Step 4b: admin_user_id 回补 platform_admin 克隆角色（兜底）
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

SET SQL_SAFE_UPDATES = 1;

-- =====================================================================
-- 验收查询（执行后务必核对）
-- =====================================================================
-- V1. 租户管理员仅持 platform_admin（预期每租户 1 行 role_key=platform_admin）
-- SELECT t.company_name, u.user_name, r.role_key
--   FROM sys_tenant t
--   JOIN sys_user u ON u.user_id = t.admin_user_id
--   JOIN sys_user_role ur ON ur.user_id = u.user_id
--   JOIN sys_role r ON r.role_id = ur.role_id
--  WHERE t.tenant_type = 'micro';
--
-- V2. 非超管角色不再持有平台目录（预期 0）
-- SELECT COUNT(*) FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id
--  WHERE r.role_id <> 1 AND rm.menu_id IN (1300, 6000, 7260, 7000, 7005);
--
-- V3. 7261 已挂 7100、7262 仍挂 7261（预期 7261→7100 / 7262→7261）
-- SELECT menu_id, menu_name, parent_id FROM sys_menu WHERE menu_id IN (7261, 7262);
--
-- V4. boss 角色投诉菜单可用且祖先完整（预期每 boss 角色含 7100/7261，不含 7260）
-- SELECT r.tenant_id, r.role_key,
--        SUM(rm.menu_id = 7100) AS has_7100,
--        SUM(rm.menu_id = 7261) AS has_7261,
--        SUM(rm.menu_id = 7260) AS has_7260
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.role_key = 'boss' AND r.del_flag = '0'
--  GROUP BY r.tenant_id, r.role_key;
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
