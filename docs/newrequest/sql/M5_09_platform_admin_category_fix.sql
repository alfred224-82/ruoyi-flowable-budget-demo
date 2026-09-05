-- =====================================================================
-- M5_09 platform_admin 费用大类菜单权限修复（T5.13 三层角色分工）
-- 版本：v1.0  日期：2026-08-30
-- 背景：
--   M5_08 给 platform_admin 补充了 3310（费用大类管理）+ 3311（大类查询），
--   但 3310 页面组件 fee/category/index 调用 /fee/category/list 需要 fee:category:list，
--   而 3311 按钮权限为 fee:category:query，权限不匹配导致 403 报错：
--     "请求地址'/fee/category/list',权限码校验失败'无此权限：fee:category:list'"
--   同时 3323 菜单名称显示为乱码（?????????），需改为'费用大类管理'
--   platform_admin 的正确工作流是使用 3323（费用大类管理）页面，
--   该页面调用 /fee/tenantCategory/list（fee:tenantCategory:list），已正确授权。
-- 修复方案：
--   1. 将 3323 菜单名称从'租户大类配置'改为'费用大类管理'（修复乱码显示）
--   2. 从 PLATFORM_ADMIN_* 模板中移除 3310/3311（费用大类管理，admin 专属）
--   3. 从共享角色 9205 中移除 3310/3311
--   4. 从租户克隆 platform_admin 角色中移除 3310/3311
--   5. 将 3323/3324（费用大类管理）补入 PLATFORM_ADMIN_* 模板
--   6. 将 3323/3324 补入租户克隆 platform_admin 角色
--   7. 保留 2000（预算管理目录）作为父目录
-- 规范：先删后插 / INSERT IGNORE，脚本可重复执行
-- =====================================================================

-- ----------------------------
-- 1. 修复 3323 菜单名称（乱码 → 费用大类管理）
-- ----------------------------
UPDATE `sys_menu`
SET `menu_name` = '费用大类管理'
WHERE `menu_id` = 3323;

-- ----------------------------
-- 2. 从 PLATFORM_ADMIN_* 模板中移除 3310/3311
-- ----------------------------
DELETE FROM `sys_edition_menu_template`
WHERE `edition` IN ('PLATFORM_ADMIN_BASIC', 'PLATFORM_ADMIN_ADVANCED', 'PLATFORM_ADMIN_PROFESSIONAL')
  AND `menu_id` IN (3310, 3311);

-- ----------------------------
-- 3. 将 3323/3324 补入 PLATFORM_ADMIN_* 模板
-- ----------------------------
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 基础版：费用大类管理页 + 保存按钮
('PLATFORM_ADMIN_BASIC', 3323),
('PLATFORM_ADMIN_BASIC', 3324);

-- 高级版继承基础版（此处显式补登，确保存量环境直接执行也能生效）
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_ADVANCED', 3323),
('PLATFORM_ADMIN_ADVANCED', 3324);

-- 专业版继承高级版
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_PROFESSIONAL', 3323),
('PLATFORM_ADMIN_PROFESSIONAL', 3324);

-- ----------------------------
-- 4. 从共享角色 9205 (platform_admin) 中移除 3310/3311
-- ----------------------------
DELETE FROM `sys_role_menu`
WHERE `role_id` = 9205 AND `menu_id` IN (3310, 3311);

-- ----------------------------
-- 5. 给共享角色 9205 补上 3323/3324（M5_07 已加过，INSERT IGNORE 幂等）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9205, 3323),
(9205, 3324);

-- ----------------------------
-- 6. 从租户克隆 platform_admin 角色中移除 3310/3311
-- ----------------------------
DELETE FROM `sys_role_menu`
WHERE `role_id` IN (
    SELECT r.`role_id` FROM `sys_role` r
    WHERE r.`role_key` = 'platform_admin'
      AND r.`tenant_id` IS NOT NULL
      AND r.`del_flag` = '0'
)
AND `menu_id` IN (3310, 3311);

-- ----------------------------
-- 7. 给租户克隆 platform_admin 角色补上 3323/3324（幂等）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN (
    SELECT 3323 AS `menu_id`
    UNION ALL SELECT 3324
) m
WHERE r.`role_key` = 'platform_admin'
  AND r.`tenant_id` IS NOT NULL
  AND r.`del_flag` = '0';

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1. PLATFORM_ADMIN 模板（预期：含 3323/3324，不含 3310/3311）
-- SELECT edition, menu_id FROM sys_edition_menu_template
-- WHERE edition LIKE 'PLATFORM_ADMIN_%' AND menu_id IN (3310, 3311, 3323, 3324)
-- ORDER BY edition, menu_id;

-- 2. 共享角色 9205（预期：含 3323/3324，不含 3310/3311）
-- SELECT rm.role_id, rm.menu_id, m.menu_name, m.perms
-- FROM sys_role_menu rm
-- JOIN sys_menu m ON m.menu_id = rm.menu_id
-- WHERE rm.role_id = 9205 AND rm.menu_id IN (3310, 3311, 3323, 3324);

-- 3. 租户克隆角色（预期：每租户 platform_admin 含 3323/3324，不含 3310/3311）
-- SELECT r.tenant_id, r.role_id, rm.menu_id, m.menu_name, m.perms
-- FROM sys_role r
-- JOIN sys_role_menu rm ON rm.role_id = r.role_id
-- JOIN sys_menu m ON m.menu_id = rm.menu_id
-- WHERE r.role_key = 'platform_admin' AND r.tenant_id IS NOT NULL
--   AND rm.menu_id IN (3310, 3311, 3323, 3324)
-- ORDER BY r.tenant_id, rm.menu_id;
