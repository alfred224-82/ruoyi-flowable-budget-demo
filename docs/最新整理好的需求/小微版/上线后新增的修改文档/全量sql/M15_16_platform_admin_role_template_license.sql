-- =====================================================================
-- M15_16 platform_admin 角色模板补全 + License 菜单修复脚本
-- 版本：v1.0  日期：2026-09-12
-- 依据：用户反馈小微版免费版(micro_lite)租户管理员(platform_admin)登录后无菜单
-- 根因：role_template 表中完全没有 platform_admin 条目（M15_07/M15_10 只建了 boss/salesman/customer）
--       → M15_15 Step 2 JOIN role_template 匹配不到 platform_admin → sys_role_menu 为空
-- 修复逻辑（全部幂等）：
--   Step 1: 将 License管理(3130) re-parent 到「资金平台管理」(1300)
--   Step 2: 为 micro_lite / micro_pro 补充 platform_admin role_template 配方
--   Step 3: 为存量租户 platform_admin 克隆角色补写 sys_role_menu
--   Step 4: 同步修复全局共享预设角色 9205 (platform_admin) 的 sys_role_menu
-- 前置依赖：M15_10（role_template 分档配方）、M15_12（资金平台管理菜单 1300）、
--           M15_15（存量租户修复）、sys_menu_fee_increment.sql（License 菜单 3130~3132）
-- =====================================================================

-- =====================================================================
-- Step 1: License管理(3130) re-parent 到「资金平台管理」(1300)
-- =====================================================================
-- 原因：License管理当前挂在「系统管理」(parent_id=1) 下，
--       platform_admin 无权访问系统管理，导致看不到 License 菜单。
--       License 属于平台管理功能（上传 License 升级版本），应归入 1300。
-- UPDATE by menu_id 幂等安全：仅当 parent_id≠1300 时才更新
SET SQL_SAFE_UPDATES = 0;
UPDATE `sys_menu`
SET `parent_id` = 1300, `order_num` = 7
WHERE `menu_id` = 3130
  AND `parent_id` <> 1300;
SET SQL_SAFE_UPDATES = 1;

-- =====================================================================
-- Step 2: 为 micro_lite / micro_pro 补充 platform_admin role_template 配方
-- =====================================================================
-- ★先删后插幂等（按 version_type+role_code 精确删除）
DELETE FROM `role_template`
WHERE `version_type` IN ('micro_lite', 'micro_pro')
  AND `role_code` = 'platform_admin';

-- 2.1 micro_lite 档 platform_admin（免费版：平台管理 + License 上传升级）
INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
('micro_lite', 'platform_admin',
 JSON_ARRAY(
   'system:tenant:list',
   'system:role:query',
   'system:industry:list',
   'system:menu:query',
   'system:roleTemplate:list',
   'system:edition:list',
   'system:license:query',
   'system:license:manage'
 ),
 JSON_ARRAY(),
 '0', 'admin',
 'micro_lite 租户组织管理员：租户管理/角色查看/行业模板/菜单查看/角色模板/版本档位/License管理(上传升级)'),

-- 2.2 micro_pro 档 platform_admin（收费版：同 micro_lite，平台管理功能不随业务档位变化）
('micro_pro', 'platform_admin',
 JSON_ARRAY(
   'system:tenant:list',
   'system:role:query',
   'system:industry:list',
   'system:menu:query',
   'system:roleTemplate:list',
   'system:edition:list',
   'system:license:query',
   'system:license:manage'
 ),
 JSON_ARRAY(),
 '0', 'admin',
 'micro_pro 租户组织管理员：租户管理/角色查看/行业模板/菜单查看/角色模板/版本档位/License管理(上传升级)');

-- =====================================================================
-- Step 3: 为存量租户 platform_admin 克隆角色补写 sys_role_menu（幂等 INSERT IGNORE）
-- =====================================================================
-- 逻辑：sys_tenant(micro) → sys_role(platform_admin 克隆) → 新 role_template → sys_menu(perms)
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT
    r.`role_id`,
    m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r
  ON r.`tenant_id` = t.`id`
 AND r.`role_key` = 'platform_admin'
JOIN `role_template` rt
  ON rt.`version_type` = t.`version_type`
 AND rt.`role_code` = 'platform_admin'
 AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(
    rt.`menu_keys`,
    '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')
) AS j
JOIN `sys_menu` m
  ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL
  AND t.`version_type` != ''
  AND NOT EXISTS (
      SELECT 1 FROM `sys_role_menu` rm
       WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`
  );

-- =====================================================================
-- Step 4: 同步修复全局共享预设角色 9205 (platform_admin) 的 sys_role_menu
-- =====================================================================
-- 确保 9205 也拥有 License 管理菜单权限（超管在预设角色上直接授权）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 9205, m.`menu_id`
FROM `sys_menu` m
WHERE m.`perms` IN (
    'system:tenant:list',
    'system:role:query',
    'system:industry:list',
    'system:menu:query',
    'system:roleTemplate:list',
    'system:edition:list',
    'system:license:query',
    'system:license:manage'
)
AND NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` rm
     WHERE rm.`role_id` = 9205 AND rm.`menu_id` = m.`menu_id`
);

-- =====================================================================
-- 验收查询（执行后人工核对）
-- =====================================================================
-- A. License管理已移到资金平台管理下
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id = 3130;
-- 预期：parent_id = 1300
--
-- B. role_template 包含 platform_admin 条目
-- SELECT version_type, role_code, JSON_LENGTH(menu_keys) AS menus
--   FROM role_template WHERE role_code = 'platform_admin' ORDER BY version_type;
-- 预期：micro_lite 和 micro_pro 各 1 行，menus = 8
--
-- C. 存量租户 platform_admin 菜单数 > 0
-- SELECT t.id, t.company_name, t.version_type, r.role_key, COUNT(rm.menu_id) AS menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin'
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name, t.version_type, r.role_key;
-- 预期：menu_count >= 8（含 License 管理）
--
-- D. ★License 菜单权限验证
-- SELECT r.role_key, t.version_type, m.menu_id, m.menu_name, m.perms
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin'
--   JOIN sys_role_menu rm ON rm.role_id = r.role_id
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE t.tenant_type = 'micro'
--    AND m.perms IN ('system:license:query', 'system:license:manage')
--  ORDER BY t.id, m.menu_id;
-- 预期：每个小微版租户 2 行（query + manage）
--
-- E. 预设角色 9205 也有 License 权限
-- SELECT m.menu_id, m.menu_name, m.perms
--   FROM sys_role_menu rm
--   JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE rm.role_id = 9205
--    AND m.perms LIKE 'system:license:%'
--  ORDER BY m.menu_id;
-- 预期：2 行（3131=授权查询, 3132=授权管理）
