-- =====================================================================
-- M5_08 platform_admin 补充费用大类管理菜单（T5.13 三层角色分工修复）
-- 版本：v1.0  日期：2026-08-30
-- 背景：
--   M5_03 注册了费用大类管理菜单（3310），但仅授权了财务(9202)和老板(9201)，
--   遗漏了 platform_admin(9205)。platform_admin 需要查看 admin 定义的大类主数据，
--   才能为租户勾选启用的大类（M5_07 的 3323 租户大类配置页依赖此菜单）。
-- 说明：
--   1. platform_admin 只需只读权限（查看大类列表），不需要增删改
--   2. 补充 PLATFORM_ADMIN_BASIC/ADVANCED/PROFESSIONAL 三版本模板
--   3. 补充共享角色 9205 的菜单授权
--   4. 补充已有租户克隆 platform_admin 角色的菜单（INSERT IGNORE 幂等）
--   5. 先删后插 / INSERT IGNORE，脚本可重复执行
-- =====================================================================

-- ----------------------------
-- 1. 补充三版本 PLATFORM_ADMIN 模板（只读：目录 + 页面 + 查询按钮）
-- ----------------------------
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
-- 预算管理目录（3310 挂载于此）
('PLATFORM_ADMIN_BASIC', 2000),
-- 费用大类管理（只读查看）
('PLATFORM_ADMIN_BASIC', 3310),
('PLATFORM_ADMIN_BASIC', 3311);
-- 注：3310=费用大类管理(C页面)、3311=大类查询(F按钮)
-- platform_admin 不需要 3312(新增)/3313(编辑)/3314(删除)/3315(拆分比例)

-- ADVANCED/PROFESSIONAL 继承 BASIC（已通过 M2_02 的级联 INSERT SELECT 保证）
-- 此处显式补登，确保存量环境直接执行也能生效
INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_ADVANCED', 2000),
('PLATFORM_ADMIN_ADVANCED', 3310),
('PLATFORM_ADMIN_ADVANCED', 3311);

INSERT IGNORE INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('PLATFORM_ADMIN_PROFESSIONAL', 2000),
('PLATFORM_ADMIN_PROFESSIONAL', 3310),
('PLATFORM_ADMIN_PROFESSIONAL', 3311);

-- ----------------------------
-- 2. 补充共享角色 9205 (platform_admin) 的菜单授权
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES
(9205, 2000),
(9205, 3310),
(9205, 3311);

-- ----------------------------
-- 3. 补充已有租户克隆 platform_admin 角色的菜单授权（幂等）
--    克隆角色特征：role_key='platform_admin' AND tenant_id IS NOT NULL
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN (
    SELECT 2000 AS `menu_id`
    UNION ALL SELECT 3310
    UNION ALL SELECT 3311
) m
WHERE r.`role_key` = 'platform_admin'
  AND r.`tenant_id` IS NOT NULL
  AND r.`del_flag` = '0';

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1. 模板完整性（预期 PLATFORM_ADMIN_BASIC/ADVANCED/PROFESSIONAL 各含 3310）
-- SELECT edition, COUNT(*) FROM sys_edition_menu_template
-- WHERE menu_id IN (2000, 3310, 3311)
-- GROUP BY edition;

-- 2. 共享角色 9205 授权（预期 3 行：2000/3310/3311）
-- SELECT rm.role_id, r.role_key, rm.menu_id, m.menu_name
-- FROM sys_role_menu rm
-- JOIN sys_role r ON r.role_id = rm.role_id
-- JOIN sys_menu m ON m.menu_id = rm.menu_id
-- WHERE rm.role_id = 9205 AND rm.menu_id IN (2000, 3310, 3311);

-- 3. 租户克隆角色授权（预期每租户 3 行）
-- SELECT r.tenant_id, r.role_id, rm.menu_id, m.menu_name
-- FROM sys_role r
-- JOIN sys_role_menu rm ON rm.role_id = r.role_id
-- JOIN sys_menu m ON m.menu_id = rm.menu_id
-- WHERE r.role_key = 'platform_admin' AND r.tenant_id IS NOT NULL
--   AND rm.menu_id IN (2000, 3310, 3311)
-- ORDER BY r.tenant_id, rm.menu_id;
