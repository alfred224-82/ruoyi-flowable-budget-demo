-- =====================================================================
-- M15_17 platform_admin 租户管理员菜单三件套（T15.33，v1.30 新增）
-- 版本：v1.0  日期：2026-09-12
-- 依据：《资金管控系统开发任务文档（小微版）》v1.30 T15.33 详细设计
-- 概述：为 platform_admin 角色提供独立于 admin 超管的租户级管理菜单，
--       统一放在「[租户名称]管理菜单」目录下（menu_id 7500~7509 区段）。
--       与 admin 的「资金平台管理菜单」(1300~1359) 完全隔离，权限互不交叉。
-- 菜单内容：
--   1. 版本升级（micro:platform:upgrade）—— 导入 License 密钥升级 micro_lite → micro_pro
--   2. 角色映射（micro:platform:roleMapping）—— 设置角色自定义显示名
--   3. 人员管理（micro:platform:userManage）—— 新增/编辑/删除租户人员
--   4. 小程序主题（micro:platform:theme）—— 选择全局主题（默认 4 套）
-- 前置依赖：M15_06（sys_menu.client_type + 小微版菜单 7000~7499）、
--           M15_10（分档机制 version_type）、M15_16（platform_admin 角色模板）
-- ★menu_id 区段：7500~7509（避让既有 7000~7499 小微版菜单）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 先删后插幂等（7500~7509 区段）
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7500 AND 7509;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 7500 AND 7509;

-- ----------------------------
-- 2. 建菜单
-- ----------------------------
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 一级目录：[租户名称]管理菜单（platform_admin 专属，与 admin 的 1300 资金平台管理完全隔离）
(7500, '租户管理菜单', 0, 8, 'tenantAdmin', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'people', 'admin', NOW(), '★platform_admin 租户级管理菜单目录（版本升级/角色映射/人员管理/小程序主题）'),
-- 菜单一：版本升级
(7501, '版本升级', 7500, 1, 'upgrade', 'micro/platform/upgrade/index', NULL, 1, 0, 'C', '0', '0', 'micro:platform:upgrade', 'pc', 'upload', 'admin', NOW(), '展示当前版本信息，导入 License 密钥升级 micro_lite → micro_pro'),
(7502, '升级查询', 7501, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:upgrade:query', 'pc', '#', 'admin', NOW(), ''),
(7503, '执行升级', 7501, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:upgrade:do', 'pc', '#', 'admin', NOW(), '导入 License 并刷新角色模板'),
-- 菜单二：角色映射
(7504, '角色映射', 7500, 2, 'roleMapping', 'micro/platform/roleMapping/index', NULL, 1, 0, 'C', '0', '0', 'micro:platform:roleMapping', 'pc', 'edit', 'admin', NOW(), '为系统预设角色设置租户自定义显示名（如 店长/导购/会员）'),
(7505, '映射查询', 7504, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:roleMapping:query', 'pc', '#', 'admin', NOW(), ''),
(7506, '映射修改', 7504, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:platform:roleMapping:edit', 'pc', '#', 'admin', NOW(), ''),
-- 菜单三：人员管理
(7507, '人员管理', 7500, 3, 'userManage', 'micro/platform/userManage/index', NULL, 1, 0, 'C', '0', '0', 'micro:platform:userManage', 'pc', 'user', 'admin', NOW(), '新增/编辑/删除租户人员，分配角色时展示映射后的自定义角色名'),
-- 菜单四：小程序主题
(7508, '小程序主题', 7500, 4, 'theme', 'micro/platform/theme/index', NULL, 1, 0, 'C', '0', '0', 'micro:platform:theme', 'pc', 'skin', 'admin', NOW(), '默认 4 套主题供选择，选定后全局换肤，首页欢迎语系统名称=「【租户名称】管理系统」');

-- ----------------------------
-- 3. 授予超管 role_id=1
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7500 AND 7509;

-- ----------------------------
-- 4. 更新 platform_admin 角色模板（micro_lite/micro_pro 两档追加新 perms）
-- ----------------------------
-- ★先删后插幂等
DELETE FROM `role_template`
WHERE `version_type` IN ('micro_lite', 'micro_pro')
  AND `role_code` = 'platform_admin';

-- ★v1.36 修正：移除 admin 级别 perms（system:tenant:list 等），仅保留租户管理三件套 perms。
--   admin 与 platform_admin 菜单完全隔离（见记忆 admin 与小微版 platform_admin 菜单权限完全隔离）。
--   M 类型目录 7500 的 perms 为空，不会被 menu_keys→perms 匹配，需在 Step 5 显式补录。
INSERT INTO `role_template` (`version_type`, `role_code`, `menu_keys`, `block_keys`, `status`, `create_by`, `remark`) VALUES
('micro_lite', 'platform_admin',
 JSON_ARRAY(
   'micro:platform:upgrade', 'micro:platform:upgrade:query', 'micro:platform:upgrade:do',
   'micro:platform:roleMapping', 'micro:platform:roleMapping:query', 'micro:platform:roleMapping:edit',
   'micro:platform:userManage',
   'micro:platform:theme'
 ),
 JSON_ARRAY(),
 '0', 'admin', 'micro_lite platform_admin：租户管理员三件套 + 主题选择（与 admin 完全隔离）'),
('micro_pro', 'platform_admin',
 JSON_ARRAY(
   'micro:platform:upgrade', 'micro:platform:upgrade:query', 'micro:platform:upgrade:do',
   'micro:platform:roleMapping', 'micro:platform:roleMapping:query', 'micro:platform:roleMapping:edit',
   'micro:platform:userManage',
   'micro:platform:theme'
 ),
 JSON_ARRAY(),
 '0', 'admin', 'micro_pro platform_admin：同 micro_lite（平台管理功能不随业务档位变化）');

-- ----------------------------
-- 5. 存量租户 platform_admin 克隆角色补写 sys_role_menu（幂等 INSERT IGNORE）
-- ----------------------------
-- 5.1 通过 role_template.menu_keys → sys_menu.perms 匹配（C/F 类型菜单）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin'
JOIN `role_template` rt ON rt.`version_type` = t.`version_type`
  AND rt.`role_code` = 'platform_admin' AND rt.`status` = '0'
CROSS JOIN JSON_TABLE(rt.`menu_keys`, '$[*]' COLUMNS (`perm` VARCHAR(64) PATH '$')) AS j
JOIN `sys_menu` m ON m.`perms` = j.`perm`
WHERE t.`tenant_type` = 'micro'
  AND t.`version_type` IS NOT NULL AND t.`version_type` != ''
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`);

-- 5.2 ★v1.36 补录：M 类型目录 7500 的 perms 为空，不会被 menu_keys→perms 匹配，
--     但 selectMenuTreeByUserId 依赖 sys_role_menu 找到根节点才能组装完整树。
--     若不补录，子菜单 7501~7508 成为孤儿，侧边栏空白。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, 7500
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin'
WHERE t.`tenant_type` = 'micro'
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = 7500);

-- ----------------------------
-- 6. 验收查询
-- ----------------------------
-- A. 租户管理菜单目录 + 子菜单存在
-- SELECT menu_id, menu_name, parent_id, perms, menu_type FROM sys_menu WHERE menu_id BETWEEN 7500 AND 7509 ORDER BY menu_id;
-- 预期：10 行（7500 目录 + 7501~7508 四菜单 + 7502/7503/7505/7506 按钮）
--
-- B. role_template 包含新 perms
-- SELECT JSON_CONTAINS(menu_keys, '"micro:platform:upgrade"') AS has_upgrade,
--        JSON_CONTAINS(menu_keys, '"micro:platform:theme"') AS has_theme
--   FROM role_template WHERE version_type='micro_lite' AND role_code='platform_admin';
-- 预期：1/1
--
-- C. 存量租户 platform_admin 菜单数 > 0
-- SELECT t.id, t.company_name, COUNT(rm.menu_id) AS menu_count
--   FROM sys_tenant t
--   JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'platform_admin'
--   LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE t.tenant_type = 'micro'
--  GROUP BY t.id, t.company_name;
-- 预期：menu_count >= 16（原 8 + 新增 8）
