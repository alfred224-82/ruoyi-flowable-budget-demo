-- =====================================================================
-- M19_03  micro_lite(免费版) boss 菜单精简：仅保留经营概览 + 活动管理（v1.1，2026-09-19）
-- =====================================================================
-- 需求：免费版 boss 侧边栏只保留「经营概览」和「活动管理」，首页显示经营概览，
--       去掉系统管理/推广发布/店铺管理/推广分销/小程序等所有其他菜单。
--
-- 修复内容：
--   ① role_template micro_lite boss 的 menu_keys 精简为仅含
--      micro:dashboard:view（经营概览）+ micro:promo:list（活动管理）；
--   ② 存量小微版免费版租户 boss 克隆角色清理 sys_role_menu：
--      删除系统管理「可见」菜单（1/118）+ 小微运营区段（7000~7499）所有菜单关联，
--      仅保留 7100（小微运营目录）+ 7101（经营概览）+ 7151（活动管理）；
--      ★保留 1131（文件上传 F 按钮，OSS 上传用，不在侧边栏渲染）；
--   ③ 补全 M 类型祖先目录（递归 CTE）。
--
-- 前置：[25]M15_10（micro_lite 分档配方）+ [58]M15_25（经营概览菜单）+ [62]M19_01（推广管理菜单）+ [63]M19_02（OSS上传权限）
-- 幂等：JSON_SET 覆盖 + DELETE + INSERT IGNORE，可重复执行
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. role_template micro_lite boss 精简 menu_keys（仅保留经营概览 + 活动管理）
-- ---------------------------------------------------------------------
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY('micro:dashboard:view', 'micro:promo:list'),
    `remark` = 'micro_lite 老板：仅经营概览 + 活动管理；★无系统管理/推广发布/店铺管理/推广分销/小程序菜单'
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss';

-- 验证：
-- SELECT menu_keys FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：["micro:dashboard:view","micro:promo:list"]

-- ---------------------------------------------------------------------
-- 2. 存量小微版免费版租户 boss 克隆角色清理 sys_role_menu
--    策略：删除所有非目标菜单的关联，再重新插入目标菜单 + M 祖先目录
-- ---------------------------------------------------------------------

-- 2.1 删除 boss 克隆角色中系统管理「可见」菜单（1 系统管理 M + 118 文件管理 C）
--     ★保留 1131（文件上传 F 类型按钮，perms=system:oss:upload）：活动管理发布/编辑需上传图片，
--       1131 是 F 类型不在侧边栏渲染，删了会导致 boss 上传活动封面/logo/海报报 403。
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` = 'micro_lite'
  AND r.`role_key` = 'boss'
  AND r.`del_flag` = '0'
  AND rm.`menu_id` IN (1, 118);

-- 2.2 删除 boss 克隆角色中所有 7000~7499 区段的菜单关联（后续重新插入目标菜单）
DELETE rm
FROM `sys_role_menu` rm
JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` = 'micro_lite'
  AND r.`role_key` = 'boss'
  AND r.`del_flag` = '0'
  AND rm.`menu_id` BETWEEN 7000 AND 7499;

-- 2.3 重新插入目标菜单：7101（经营概览，perms=micro:dashboard:view）+ 7151（活动管理，perms=micro:promo:list）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'boss' AND r.`del_flag` = '0'
CROSS JOIN (SELECT 7101 AS `menu_id` UNION ALL SELECT 7151) m
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` = 'micro_lite';

-- 2.4 补全 M 类型祖先目录（7100 小微运营 + 7150 推广发布）
--     递归向上查找所有祖先 M 类型目录
--     ★起点必须排除 F 类型按钮（如 1131 文件上传）：F 按钮不在侧边栏渲染、无需 M 祖先，
--       若以其为起点会沿 1131→118→1 把「系统管理」(1)/「文件管理」(118) 重新补回，
--       使 2.1 的删除前功尽弃（这正是「系统管理又出来了」的根因之一）。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
WITH RECURSIVE ancestor_tree AS (
    -- 起点：已授权的 M/C 类型菜单的父目录（★排除 F 按钮，避免 1131→118→1 污染）
    SELECT DISTINCT m.`parent_id` AS ancestor_id
    FROM `sys_role_menu` rm
    JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
    JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
    JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
    WHERE t.`tenant_type` = 'micro'
      AND t.`version_type` = 'micro_lite'
      AND r.`role_key` = 'boss'
      AND r.`del_flag` = '0'
      AND m.`menu_type` IN ('M', 'C')
      AND m.`parent_id` > 0
    UNION
    -- 递归：继续向上查找
    SELECT m2.`parent_id`
    FROM ancestor_tree at
    JOIN `sys_menu` m2 ON m2.`menu_id` = at.ancestor_id
    WHERE m2.`parent_id` > 0
)
SELECT rm.`role_id`, at2.`ancestor_id`
FROM ancestor_tree at2
CROSS JOIN (
    SELECT DISTINCT r2.`role_id`
    FROM `sys_role` r2
    JOIN `sys_tenant` t2 ON t2.`id` = r2.`tenant_id`
    WHERE t2.`tenant_type` = 'micro'
      AND t2.`version_type` = 'micro_lite'
      AND r2.`role_key` = 'boss'
      AND r2.`del_flag` = '0'
) rm
WHERE at2.`ancestor_id` > 0
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm2
       WHERE rm2.`role_id` = rm.`role_id` AND rm2.`menu_id` = at2.`ancestor_id`
  );

-- ---------------------------------------------------------------------
-- 3. 验证查询（执行后人工核对）
-- ---------------------------------------------------------------------

-- 3.1 role_template micro_lite boss 仅含经营概览 + 活动管理
-- SELECT menu_keys FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：["micro:dashboard:view","micro:promo:list"]

-- 3.2 存量租户 boss 菜单精简后仅含目标菜单 + 祖先目录
-- SELECT t.id, t.company_name, m.menu_id, m.menu_name, m.menu_type
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--    AND m.menu_type IN ('M', 'C')
--  ORDER BY t.id, m.menu_id;
-- 预期：每个租户 3~4 行（7100 小微运营 + 7101 经营概览 + 7150 推广发布 + 7151 活动管理）

-- 3.3 boss 侧边栏（仅 M/C 渲染）无系统管理/推广发布子菜单/店铺管理/推广分销/小程序菜单
-- SELECT m.menu_id, m.menu_name
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--    AND m.menu_type IN ('M', 'C')
--    AND m.menu_id NOT IN (7100, 7101, 7150, 7151);
-- 预期：0 行（★F 按钮 1131 不在侧边栏渲染，已被 menu_type 过滤，不算多余）

-- 3.4 boss 无系统管理「可见」菜单（1 系统管理 / 118 文件管理）
-- SELECT m.menu_id, m.menu_name
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
--    AND m.menu_id IN (1, 118);
-- 预期：0 行（无系统管理/文件管理；1131 文件上传为 F 按钮保留，不渲染侧边栏）
