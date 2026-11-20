-- =====================================================================
-- M18_15 M18 最终汇总补丁——role_template + sys_role_menu 全覆盖（T18.20）
-- 版本：v1.0  日期：2026-09-21
-- 依据：《待开发任务清单》第七批 T18.20
--       《资金管控系统开发任务文档（小微版）》v1.36 M18
-- 前置依赖：M18_01~M18_14 全部执行完毕
-- 说明：
--   1. 本脚本为 M18 里程碑【最终汇总补丁】，在 M18_01~M18_14 全部执行后运行
--   2. 修复 M18_08 遗留缺陷：role_key='micro_pro' 不存在，应为 'boss'
--   3. 确保 role_template 包含 M18 全部新增权限（admin/platform_admin/boss 三角色）
--   4. 确保 sys_role_menu 覆盖 M18 全部菜单区段（7600~7679 + 7690 + 7695~7697 + 1370~1379；7680~7683 v1.57 已清理）
--   5. 全部幂等（JSON_CONTAINS 守卫 + INSERT IGNORE），可重复执行
--   ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- 一、修复 M18_08 缺陷：7640~7659 应授予 boss 克隆角色（非 micro_pro）
-- =====================================================================
-- M18_08 原脚本使用 role_key='micro_pro'，但 sys_role 中不存在该 role_key
-- 正确逻辑：7640（拉新奖励配置）+ 7650（员工业绩榜）→ boss 克隆角色

-- 1.1 拉新奖励配置 7640 → boss 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
WHERE r.role_key = 'boss' AND r.del_flag = '0'
  AND m.menu_id BETWEEN 7640 AND 7649;

-- 1.2 员工业绩榜 7650 → boss 克隆角色
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
WHERE r.role_key = 'boss' AND r.del_flag = '0'
  AND m.menu_id BETWEEN 7650 AND 7659;

-- =====================================================================
-- 二、Admin 角色 role_template 补全 M18 权限
-- =====================================================================
-- admin(role_code='admin', version_type IS NULL) 需包含：
--   system:compliance:sensitiveWord/imageConfig/alert（M18_12 已幂等追加）
-- 注：admin 的 sys_role_menu 由 M18_06/M18_07/M18_12 各自授予 role_id=1

-- 补全 admin 对 M18 全部 Admin 菜单的 sys_role_menu（1370~1379 + 7660~7679）
-- M18_06 已授 7660~7669、M18_07 已授 7670~7679、M18_12 已授 1370~1379
-- 此处做幂等兜底确保完整
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, m.menu_id
FROM `sys_menu` m
WHERE m.menu_id IN (
    -- Admin 平台合规目录（M18_12）
    1370, 1371, 1372, 1373,
    -- License 模板管理（M18_06）
    7660, 7661,
    -- 运维审计（M18_07）
    7670, 7671
)
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm WHERE rm.role_id = 1 AND rm.menu_id = m.menu_id
);

-- =====================================================================
-- 三、platform_admin 克隆角色 sys_role_menu 全量补录
-- =====================================================================
-- M18_08 仅覆盖 7620~7639；M18_13（v1.57 扁平化）/M18_14 在 M18_08 之后执行，
-- 其新增权限未同步到存量 platform_admin 克隆角色
-- 本步骤统一补录 M18 全部 platform_admin 菜单（7680~7683 已由 M18_13 清理）

-- 3.1 语音合规配置 7610（M18_13，v1.57 扁平化为单 C 菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
WHERE r.role_key = 'platform_admin' AND r.del_flag = '0'
  AND m.menu_id = 7610;

-- 3.2 系统设置 7695~7696（M18_14）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
WHERE r.role_key = 'platform_admin' AND r.del_flag = '0'
  AND m.menu_id BETWEEN 7695 AND 7696;

-- =====================================================================
-- 四、boss 克隆角色 sys_role_menu 全量补录
-- =====================================================================
-- 4.1 合规命中记录 7690（M18_12）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
WHERE r.role_key = 'boss' AND r.del_flag = '0'
  AND m.menu_id = 7690;

-- =====================================================================
-- 五、role_template 最终完整性校验（幂等追加）
-- =====================================================================
-- 确保各角色 role_template 包含 M18 全部新增权限
-- 各子脚本（M18_08/M18_12/M18_13/M18_14）已分别追加，此处做幂等兜底

-- 5.1 admin 平台合规权限（兜底）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(
    JSON_ARRAY_APPEND(
      JSON_ARRAY_APPEND(
        `menu_keys`,
        '$', 'system:compliance:sensitiveWord'),
      '$', 'system:compliance:imageConfig'),
    '$', 'system:compliance:alert'
)
WHERE `version_type` IS NULL
  AND `role_code` = 'admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"system:compliance:sensitiveWord"');

-- 5.2 boss 合规命中记录权限（兜底）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:boss:complianceHit:list')
WHERE `version_type` IN ('micro', 'micro_pro')
  AND `role_code` = 'boss'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:boss:complianceHit:list"');

-- =====================================================================
-- 六、验证查询
-- =====================================================================
-- 6.1 ★M18 全部菜单落库统计（7600~7679 + 7690 + 7695~7697 + 1370~1379；7680~7683 v1.57 已清理）
-- SELECT COUNT(*) AS micro_menus FROM sys_menu WHERE menu_id BETWEEN 7600 AND 7699;
--   -- 预期 ≥26（含各子脚本建的菜单；7680~7683 已清理 -4）
-- SELECT COUNT(*) AS admin_menus FROM sys_menu WHERE menu_id BETWEEN 1370 AND 1379;
--   -- 预期 4

-- 6.2 ★role_template 完整性
-- -- admin 含平台合规
-- SELECT JSON_CONTAINS(menu_keys, '"system:compliance:sensitiveWord"') AS admin_sw
--   FROM role_template WHERE version_type IS NULL AND role_code='admin';  -- 预期 1
-- -- platform_admin 含语音合规 + 系统设置
-- SELECT JSON_CONTAINS(menu_keys, '"micro:compliance:config:rule"') AS pa_vc,
--        JSON_CONTAINS(menu_keys, '"micro:systemSetting:list"')     AS pa_ss
--   FROM role_template WHERE version_type='micro_pro' AND role_code='platform_admin';  -- 预期 1/1
-- -- boss 含合规命中 + 拉新奖励 + 员工业绩榜 + 语音记录
-- SELECT JSON_CONTAINS(menu_keys, '"micro:boss:complianceHit:list"')     AS boss_hit,
--        JSON_CONTAINS(menu_keys, '"micro:distribution:rewardConfig"')   AS boss_reward,
--        JSON_CONTAINS(menu_keys, '"micro:staffRanking:list"')           AS boss_rank,
--        JSON_CONTAINS(menu_keys, '"micro:compliance:records"')          AS boss_voice
--   FROM role_template WHERE version_type='micro_pro' AND role_code='boss';  -- 预期 1/1/1/1

-- 6.3 ★sys_role_menu 完整性（存量租户）
-- -- platform_admin 克隆角色菜单数
-- SELECT t.id, t.company_name, COUNT(rm.menu_id) AS pa_menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='platform_admin' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--  WHERE t.tenant_type='micro'
--  GROUP BY t.id, t.company_name;
--   -- 预期 pa_menu_count >= 22（7500~7508 + 7610 + 7620~7628 + 7630~7631 + 7695~7696 + M目录）
-- -- boss 克隆角色含 M18 菜单
-- SELECT t.id, t.company_name,
--        SUM(CASE WHEN rm.menu_id BETWEEN 7640 AND 7659 THEN 1 ELSE 0 END) AS boss_m18_reward_rank,
--        SUM(CASE WHEN rm.menu_id = 7690 THEN 1 ELSE 0 END) AS boss_m18_hit
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id=t.id AND r.role_key='boss' AND r.del_flag='0'
--   JOIN sys_role_menu rm ON rm.role_id=r.role_id
--  WHERE t.tenant_type='micro'
--  GROUP BY t.id, t.company_name;
--   -- 预期 boss_m18_reward_rank >= 2, boss_m18_hit >= 1

-- 6.4 ★M18_08 缺陷修复确认（7640~7659 已授予 boss 而非 micro_pro）
-- SELECT COUNT(*) AS boss_7640 FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id=r.role_id
--   JOIN sys_tenant t ON r.tenant_id=t.id AND t.tenant_type='micro'
--  WHERE r.role_key='boss' AND rm.menu_id BETWEEN 7640 AND 7649;
--   -- 预期 > 0（每个小微版租户 boss 克隆角色各 1 条）
-- =====================================================================
