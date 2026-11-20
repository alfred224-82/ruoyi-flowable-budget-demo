-- =====================================================================
-- PROD_fix_all_role_menus.sql
-- 统一重建小微版所有租户克隆角色的 sys_role_menu（从 role_template 同步）
-- =====================================================================
-- 根因：M43_01 Step 9 只重建了 boss 角色的 sys_role_menu，
--       salesman/customer 租户克隆角色的 sys_role_menu 未同步。
--       本脚本按 role_template menu_keys 重建 micro_pro/micro_lite 下
--       所有租户克隆角色（tenant_id ≠ NULL）的 sys_role_menu。
-- ★铁律：所有角色均为租户克隆（sys_role.tenant_id = 租户ID），
--       通过行业角色映射（tenant_role_mapping）分配，不操作共享预设角色
-- ★幂等：先删后插 + INSERT IGNORE，可重复执行
-- ★前置：[163]M43_01（boss 菜单树重构 + role_template 重写）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- ★后续权限修复直接追加到本脚本
-- =====================================================================

SET NAMES utf8mb4;
SET SQL_SAFE_UPDATES = 0;

-- =====================================================================
-- 1. micro_pro boss 租户克隆角色 sys_role_menu 重建
--    ★boss 菜单应仅含 7100（小微运营）及其子树，不含 7000/7500/7010 等其他根目录
-- =====================================================================
-- 1.1 清理旧关联（7000~7699 区间）
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7699;

-- 1.2 按 role_template menu_keys 匹配 sys_menu.perms 重建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- 1.3 ★兜底：清理 boss 在 7100 树之外的菜单（防止 role_template 含跨树权限）
--     保留 7100~7699 区间内 parent_id 链最终到达 7100 的菜单，删除其他
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND r.`tenant_id` IS NOT NULL
  AND (
    -- 删除 7000~7099 区间（系统管理/店铺配置等非小微运营菜单）
    rm.`menu_id` BETWEEN 7000 AND 7099
    -- 删除 7500~7699 区间（租户管理菜单及其子树）
    OR rm.`menu_id` BETWEEN 7500 AND 7699
    -- 删除 7100~7499 区间中 parent_id 不在 7100 子树内的菜单
    OR (rm.`menu_id` BETWEEN 7100 AND 7499 AND m.`parent_id` NOT IN (
        SELECT menu_id FROM sys_menu WHERE parent_id = 7100 OR menu_id = 7100
    ))
  );

-- =====================================================================
-- 2. micro_pro salesman 租户克隆角色 sys_role_menu 重建
-- =====================================================================
-- 2.1 清理旧关联
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND r.`role_key` = 'salesman' AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7699;

-- 2.2 按 role_template 重建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'salesman' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- =====================================================================
-- 3. micro_pro customer 租户克隆角色 sys_role_menu 重建
-- =====================================================================
-- 3.1 清理旧关联
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND r.`role_key` = 'customer' AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7699;

-- 3.2 按 role_template 重建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'customer' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_pro'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- =====================================================================
-- 4. micro_lite boss 租户克隆角色 sys_role_menu 重建
-- =====================================================================
-- 4.1 清理旧关联
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_lite'
  AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7699;

-- 4.2 按 role_template 重建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_lite'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- =====================================================================
-- 5. micro_lite salesman 租户克隆角色 sys_role_menu 重建
-- =====================================================================
-- 5.1 清理旧关联
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_lite'
  AND r.`role_key` = 'salesman' AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7699;

-- 5.2 按 role_template 重建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'salesman' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_lite'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- =====================================================================
-- 6. micro_lite customer 租户克隆角色 sys_role_menu 重建
-- =====================================================================
-- 6.1 清理旧关联
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_lite'
  AND r.`role_key` = 'customer' AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7699;

-- 6.2 按 role_template 重建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'customer' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro' AND t.`version_type` = 'micro_lite'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- =====================================================================
-- 7. 共享预设 salesman/customer 角色 sys_role_menu 补建
--    部分存量用户的 sys_user_role 仍指向共享预设角色（tenant_id=NULL），
--    步骤 1~6 的租户克隆重建匹配不到它们，需单独补建 sys_role_menu。
--    ★仅追加（INSERT IGNORE），不删除旧记录，避免影响其他产品线租户
-- =====================================================================
-- 7.1 共享 salesman 按 micro_pro role_template 补建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `role_template` rt ON rt.`version_type` = 'micro_pro' AND rt.`role_code` = 'salesman' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE r.`role_key` = 'salesman' AND r.`tenant_id` IS NULL AND r.`del_flag` = '0'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- 7.2 共享 customer 按 micro_pro role_template 补建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
JOIN `role_template` rt ON rt.`version_type` = 'micro_pro' AND rt.`role_code` = 'customer' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE r.`role_key` = 'customer' AND r.`tenant_id` IS NULL AND r.`del_flag` = '0'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- =====================================================================
-- 8. platform_admin 租户克隆角色 sys_role_menu 重建
--    ★根因：部分存量 platform_admin 克隆角色的 sys_role_menu 残留了
--      7010（店铺配置，原 platform_admin PC 根目录）和 7100（小微运营，boss 专属），
--      导致 platform_admin 侧边栏出现多余菜单。
--      按 M37_01 重构后的 role_template 重建，仅保留 7500~7559 + 7610/7630/7631。
-- =====================================================================
-- 8.1 清理旧关联：删除 7000~7699 区间中不属于 M37_01 重构后菜单的记录
--      ★显式排除 7100（小微运营，boss 专属根目录）和 7010（店铺配置，历史根目录），
--        这两个菜单不应出现在 platform_admin 侧边栏
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro'
  AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND (
    rm.`menu_id` IN (7010, 7100)
    OR (rm.`menu_id` BETWEEN 7000 AND 7699
        AND NOT (rm.`menu_id` BETWEEN 7500 AND 7559)
        AND rm.`menu_id` NOT IN (7610, 7630, 7631))
  );

-- 8.2 按 role_template menu_keys 匹配 sys_menu.perms 重建
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type` AND rt.`role_code` = r.`role_key` AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON FIND_IN_SET(j.`perm`, m.`perms`) > 0
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_type` IN ('M', 'C', 'F')
  AND m.`perms` IS NOT NULL AND m.`perms` <> '';

-- 8.3 补全 M 类型祖先目录（7540~7545，perms 为空无法通过 perms 匹配）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7540, 7541, 7542, 7543, 7544, 7545)
  AND m.`menu_type` = 'M';

-- =====================================================================
-- 9. 统一补全所有角色的 M 类型祖先目录菜单
--    递归 CTE：从步骤 1~8 已插入的 C/F 菜单向上遍历 parent_id 链，
--    补全所有 M 类型祖先目录（菜单树渲染需要完整祖先链才能显示）
--    ★不限 7000~7699 区间，因为祖先可能在此范围外（如 7150 活动管理父目录）
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
WITH RECURSIVE ancestor_tree AS (
    -- 锚点：步骤 1~8 已插入的非 M 类型菜单
    SELECT DISTINCT rm.`role_id`, m.`parent_id` AS `menu_id`
    FROM `sys_role_menu` rm
    JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
    WHERE m.`menu_type` IN ('C', 'F')
      AND m.`parent_id` > 0
    UNION
    -- 递归：继续向上查找父菜单
    SELECT at.`role_id`, m2.`parent_id`
    FROM ancestor_tree at
    JOIN `sys_menu` m2 ON m2.`menu_id` = at.`menu_id`
    WHERE m2.`parent_id` > 0
)
SELECT DISTINCT at.`role_id`, at.`menu_id`
FROM ancestor_tree at
JOIN `sys_menu` m ON m.`menu_id` = at.`menu_id`
WHERE m.`menu_type` = 'M';

-- =====================================================================
-- 10. ★兜底：显式清理 platform_admin 克隆角色的 7010/7100
--     步骤 8.1 可能因 JOIN 条件未匹配到某些记录，或步骤 9 CTE 重新加回，
--     此处做最终清理确保 platform_admin 侧边栏不含 boss 专属根菜单。
-- =====================================================================
DELETE rm FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
WHERE r.`role_key` = 'platform_admin' AND r.`del_flag` = '0'
  AND r.`tenant_id` IS NOT NULL
  AND rm.`menu_id` IN (7010, 7100);

SET SQL_SAFE_UPDATES = 1;
