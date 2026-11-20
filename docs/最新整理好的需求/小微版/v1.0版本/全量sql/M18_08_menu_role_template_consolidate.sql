-- =====================================================================
-- M18_08 M18 里程碑菜单汇总 + role_template 更新脚本（T18.20，v1.29）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18 全量任务
-- 前置依赖：M18_01~M18_07（各子任务已分别建表+建菜单）
-- 规范：
--   1. 本脚本为【汇总补丁】：将 M18 新增菜单权限补写入 role_template
--   2. role_template 为系统级共享表（无 tenant_id），IGNORE_TENANT_TABLES
--   3. 先 JSON_REMOVE 移除旧值（如有），再 JSON_ARRAY_APPEND 追加新值
--   4. 幂等：使用 JSON_CONTAINS 检查后再追加
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、M18 新增菜单清单（menu_id 7620~7679，各子脚本已建）
-- =====================================================================
-- | 区段       | 任务      | 菜单名称           | 权限标识                           | 归属角色        |
-- |------------|-----------|--------------------|------------------------------------|-----------------|
-- | 7620~7629  | T18.9     | 固定支出配置       | micro:tagConfig:list/add/edit/remove | platform_admin  |
-- |            |           | 周期规则管理       | micro:periodRule:list/add/edit/remove| platform_admin  |
-- | 7630~7639  | T18.10    | 预约策略配置       | micro:bookingPolicy:list/edit    | platform_admin  |
-- | 7640~7649  | T18.12    | 拉新奖励配置       | micro:distribution:rewardConfig    | micro_pro(boss) |
-- | 7650~7659  | T18.13    | 员工业绩榜         | micro:staffRanking:list            | micro_pro(boss) |
-- | 7660~7669  | T18.14    | License模板管理    | system:licenseTemplate:list        | admin           |
-- |            |           | License使用报表    | system:licenseReport:list          | admin           |
-- | 7670~7679  | T18.15    | 语音处理日志       | system:voiceAuditLog:list          | admin           |
-- |            |           | 告警规则配置       | system:alertRule:list              | admin           |

-- =====================================================================
-- 二、role_template 更新：boss 角色补入 M18 新增菜单权限
--     适用 version_type = 'micro_pro' / 'micro_lite'
-- =====================================================================

-- ★boss：补入拉新奖励配置 + 员工业绩榜权限
-- 幂等检查：若已包含则跳过
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
        `menu_keys`,
        '$', 'micro:distribution:rewardConfig'
    ),
    '$', 'micro:staffRanking:list'
)
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:distribution:rewardConfig"');

-- =====================================================================
-- 三、role_template 更新：platform_admin 角色补入 M18 新增菜单权限
--     适用 version_type = 'micro_pro' / 'micro_lite'
-- =====================================================================

-- ★platform_admin：补入固定支出配置 + 周期规则 + 预约策略（含 edit 按钮）权限
-- 注：micro:bookingPolicy:edit 为按钮级 F 权限（menu_id=7631），对应 MicroBookingPolicyController.savePolicy 的 @SaCheckPermission
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
        JSON_ARRAY_APPEND(
            JSON_ARRAY_APPEND(
                `menu_keys`,
                '$', 'micro:tagConfig:list'
            ),
            '$', 'micro:periodRule:list'
        ),
        '$', 'micro:bookingPolicy:list'
    ),
    '$', 'micro:bookingPolicy:edit'
)
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:tagConfig:list"');

-- ★补丁：对已含 tagConfig:list 但缺 bookingPolicy:edit 的存量 role_template 单独追加（幂等）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:bookingPolicy:edit')
WHERE `version_type` IN ('micro_pro', 'micro_lite')
  AND `role_code` = 'platform_admin'
  AND JSON_CONTAINS(`menu_keys`, '"micro:bookingPolicy:list"')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:bookingPolicy:edit"');

-- =====================================================================
-- 四、sys_role_menu 补录：存量租户的 platform_admin 克隆角色
--     确保已存在的小微版租户能立即看到新菜单
-- =====================================================================

-- 4.1 固定支出配置菜单（7620~7629）→ platform_admin 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin'
  AND m.menu_id BETWEEN 7620 AND 7629;

-- 4.2 预约策略配置菜单（7630~7639）→ platform_admin 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin'
  AND m.menu_id BETWEEN 7630 AND 7639;

-- 4.3 拉新奖励配置菜单（7640~7649）→ micro_pro 角色（boss）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'micro_pro'
  AND m.menu_id BETWEEN 7640 AND 7649;

-- 4.4 员工业绩榜菜单（7650~7659）→ micro_pro 角色（boss）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'micro_pro'
  AND m.menu_id BETWEEN 7650 AND 7659;

-- 4.5 License 管理菜单（7660~7669）→ admin 角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'admin'
  AND m.menu_id BETWEEN 7660 AND 7669;

-- 4.6 运维审计菜单（7670~7679）→ admin 角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'admin'
  AND m.menu_id BETWEEN 7670 AND 7679;

-- =====================================================================
-- 五、验证查询（执行后人工核对）
-- =====================================================================

-- 5.1 boss 配方含 M18 新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:distribution:rewardConfig"') AS has_reward,
--        JSON_CONTAINS(menu_keys, '"micro:staffRanking:list"') AS has_ranking
-- FROM role_template WHERE version_type IN ('micro_pro','micro_lite') AND role_code = 'boss';
-- 预期：1, 1

-- 5.2 platform_admin 配方含 M18 新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:tagConfig:list"')     AS has_tag,
--        JSON_CONTAINS(menu_keys, '"micro:periodRule:list"')    AS has_period,
--        JSON_CONTAINS(menu_keys, '"micro:bookingPolicy:list"') AS has_booking_list,
--        JSON_CONTAINS(menu_keys, '"micro:bookingPolicy:edit"') AS has_booking_edit
-- FROM role_template WHERE version_type IN ('micro_pro','micro_lite') AND role_code = 'platform_admin';
-- 预期：1, 1, 1, 1

-- 5.3 M18 全部菜单已落库
-- SELECT COUNT(*) AS total_menus FROM sys_menu WHERE menu_id BETWEEN 7620 AND 7679;
-- 预期：≥12（实际菜单行数，不含按钮级 F 类型）

-- 5.4 sys_role_menu 关联完整
-- SELECT m.menu_id, m.menu_name, COUNT(rm.role_id) AS role_count
-- FROM sys_menu m LEFT JOIN sys_role_menu rm ON m.menu_id = rm.menu_id
-- WHERE m.menu_id BETWEEN 7620 AND 7679
-- GROUP BY m.menu_id, m.menu_name ORDER BY m.menu_id;
-- 预期：每个菜单至少有 1 个角色关联
