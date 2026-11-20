-- =====================================================================
-- M19_01  micro_lite(免费版) boss 推广管理菜单授权（v1.0，2026-09-19）
-- =====================================================================
-- 依据：《五角色菜单骨架_V3.7_最终完整版.md》§八 免费版（micro_lite）老板菜单范围
--
-- 免费版包含：
--   1. 推广管理
--      - 拉新奖励配置（唯一配置入口）
--      - 推广数据看板（拉新排行榜 + 推广奖励发放记录）
--   2. 活动管理
--      - 活动列表
--
-- 修复：role_template micro_lite boss 缺少推广管理相关权限，导致免费版老板
--       无法访问拉新奖励配置、推广数据看板等页面。
--
-- 修复内容：
--   ① role_template micro_lite boss 追加推广管理权限（JSON_CONTAINS 守卫）；
--   ② 存量小微版免费版租户 boss 克隆角色补写 sys_role_menu（INSERT IGNORE 幂等）；
--   ③ 补全 M 类型祖先目录（与 copyMenuTemplateForMicro 同逻辑）。
--
-- 前置：[21]M15_06（菜单 7000~7499）+ [25]M15_10（micro_lite 分档配方）
--       + [48]M18_08（micro_pro 已含 micro:distribution:rewardConfig）
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. role_template micro_lite boss 追加推广管理权限
--    新增权限：
--    - micro:distribution:rewardConfig  拉新奖励配置页面
--    - micro:distribution:*             推广数据看板（统计/排行榜/奖励记录）
-- ---------------------------------------------------------------------

-- 1.1 追加 micro:distribution:rewardConfig
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:distribution:rewardConfig')
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:distribution:rewardConfig"');

-- 1.2 追加 micro:distribution:*（推广看板、奖励记录、统计）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:distribution:*')
WHERE `version_type` = 'micro_lite'
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:distribution:*"');

-- 验证：
-- SELECT JSON_CONTAINS(menu_keys, '"micro:distribution:rewardConfig"') AS has_reward_config,
--        JSON_CONTAINS(menu_keys, '"micro:distribution:*"') AS has_distribution
--   FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：1, 1

-- ---------------------------------------------------------------------
-- 2. 存量小微版免费版租户 boss 克隆角色补写 sys_role_menu
--    递归 CTE 补全 M 类型祖先目录（与 copyMenuTemplateForMicro 同逻辑）
-- ---------------------------------------------------------------------

-- 2.1 补写 micro:distribution:rewardConfig 对应菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT
    r.`role_id`,
    m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r
  ON r.`tenant_id` = t.`id`
 AND r.`role_key` = 'boss'
JOIN `role_template` rt
  ON rt.`version_type` = t.`version_type`
 AND rt.`role_code` = r.`role_key`
 AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(
    rt.`menu_keys`,
    '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')
) AS j
JOIN `sys_menu` m
  ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` = 'micro_lite'
  AND r.`del_flag` = '0'
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
       WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- 2.2 补全 M 类型祖先目录（菜单树完整性）
--     递归向上查找所有祖先 M 类型目录
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
WITH RECURSIVE ancestor_tree AS (
    -- 起点：已授权的 C 类型菜单
    SELECT DISTINCT m.`parent_id` AS ancestor_id
    FROM `sys_role_menu` rm
    JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
    JOIN `sys_role` r ON r.`role_id` = rm.`role_id`
    JOIN `sys_tenant` t ON t.`id` = r.`tenant_id`
    WHERE t.`tenant_type` = 'micro'
      AND t.`version_type` = 'micro_lite'
      AND r.`role_key` = 'boss'
      AND r.`del_flag` = '0'
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

-- 3.1 micro_lite boss 配方含推广管理权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:distribution:rewardConfig"') AS has_reward_config,
--        JSON_CONTAINS(menu_keys, '"micro:distribution:*"') AS has_distribution,
--        JSON_CONTAINS(menu_keys, '"micro:promo:list"') AS has_promo_list
-- FROM role_template WHERE version_type='micro_lite' AND role_code='boss';
-- 预期：1, 1, 1

-- 3.2 存量租户 boss 菜单授权完整
-- SELECT t.id AS tenant_id, t.company_name, COUNT(rm.menu_id) AS menu_count
-- FROM sys_tenant t
-- JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
-- JOIN sys_role_menu rm ON rm.role_id = r.role_id
-- WHERE t.tenant_type = 'micro' AND t.version_type = 'micro_lite'
-- GROUP BY t.id, t.company_name;
-- 预期：每个租户 menu_count >= 10（含推广管理相关菜单）
