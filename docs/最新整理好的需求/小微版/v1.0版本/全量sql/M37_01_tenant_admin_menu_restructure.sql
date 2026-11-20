-- =====================================================================
-- M37_01 租户管理员菜单层级重构（v4.0）
-- 版本：v4.0  日期：2026-10-24
-- 概述：按新设计重构 platform_admin 租户管理员菜单的目录层级、名称、图标。
--       先删后插幂等（DELETE + INSERT），操作区段 7500~7559 / 7650~7659。
-- 变更摘要：
--   1. 7500 根目录 icon people → office，名称保持「租户管理菜单」
--   2. 新建 6 个 M 类型中间目录：业务运营(7540)/人员与权限(7541)/
--      成本分摊管理(7542)/小程序配置(7544)/AI语音合规(7545)/系统运维(7543)
--   3. 删除旧中间目录：原子资源管理(7510)/固定支出配置(7620)
--   4. 删除员工业绩榜(7650)——属 boss 菜单，租户管理员不需要
--   5. 复用旧菜单：语音与合规配置(7610)→语音配置（改名+迁移到7545，保留三Tab页面）
--      策略开关(7630) 保持直属7500（与合规无关，独立菜单），保留子按钮7631
--   6. 重命名：角色映射→角色权限配置 / 房间/空间管理→房间资源管理 /
--      标签管理→支出标签管理 / 周期规则管理→分摊周期规则 / 小程序主题→小程序主题设置
--   7. 层级变更：版本升级/系统设置 从直属7500改挂系统运维(7543)下
--   8. 7291 房间布局配置从 7510 迁移到 7540(业务运营)
--   9. 新建员工提成管理(7551)叶子菜单
--  10. role_template platform_admin 追加 micro:staffCommission:list
-- 前置依赖：M15_17(7500目录) + M15_23(7510原子资源) + M18_02(7620固定支出)
--           + M18_03(7630策略开关) + M18_05(7650业绩榜) + M18_13(7610合规)
--           + M18_14(7695系统设置) + M26_04(7291房间布局)
-- ★menu_id 区段：7500~7559（根目录+新目录+新菜单）/ 7650~7659（清理员工业绩榜）
-- =====================================================================

SET NAMES utf8mb4;

-- =====================================================================
-- Step 1: 清理旧菜单及角色关联（先删后插幂等）
-- =====================================================================
-- 1.1 清理 7510（原子资源管理目录）+ 7517~7519（预留空位）
--     ★7511~7516 保留（项目管理+房间/空间管理+按钮），Step 5 会 re-parent 到 7540
DELETE FROM `sys_role_menu` WHERE `menu_id` IN (7510, 7517, 7518, 7519);
DELETE FROM `sys_menu`      WHERE `menu_id` IN (7510, 7517, 7518, 7519);

-- 1.2 清理 7530~7559（本次新分配的目录/菜单区段，确保幂等）
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7530 AND 7559;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7530 AND 7559;

-- 1.3 清理 7620~7629（旧固定支出配置目录 + 标签/周期规则子菜单 + 按钮）
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7620 AND 7629;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7620 AND 7629;

-- 1.4 清理 7650~7659（旧员工业绩榜——属 boss 菜单，租户管理员不需要）
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7650 AND 7659;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7650 AND 7659;

-- =====================================================================
-- Step 2: 更新 7500 根目录（icon: people → office）
-- =====================================================================
UPDATE `sys_menu`
SET `icon` = 'office',
    `remark` = '★platform_admin 租户管理员菜单根目录（v4.0 层级重构：业务运营/人员与权限/成本分摊/员工提成/小程序配置/AI语音合规/系统运维）',
    `update_by` = 'admin',
    `update_time` = NOW()
WHERE `menu_id` = 7500;

-- =====================================================================
-- Step 3: 新建 6 个 M 类型中间目录
-- =====================================================================
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 业务运营（icon: office）
(7540, '业务运营', 7500, 1, 'businessOps', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'office', 'admin', NOW(), '★租户管理员→业务运营目录（项目管理/房间资源管理/房间布局配置）'),
-- 人员与权限（icon: user）
(7541, '人员与权限', 7500, 2, 'personnelAuth', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'user', 'admin', NOW(), '★租户管理员→人员与权限目录（人员管理/角色权限配置）'),
-- 成本分摊管理（icon: coin）
(7542, '成本分摊管理', 7500, 3, 'costAllocation', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'coin', 'admin', NOW(), '★租户管理员→成本分摊管理目录（支出标签管理/分摊周期规则）'),
-- 小程序配置（icon: iphone）
(7544, '小程序配置', 7500, 5, 'miniappConfig', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'iphone', 'admin', NOW(), '★租户管理员→小程序配置目录（小程序主题设置）'),
-- AI 语音合规（icon: mic）
(7545, 'AI语音合规', 7500, 6, 'aiCompliance', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'mic', 'admin', NOW(), '★租户管理员→AI语音合规目录（语音配置）'),
-- 系统运维（icon: setting）
(7543, '系统运维', 7500, 7, 'sysOps', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'setting', 'admin', NOW(), '★租户管理员→系统运维目录（版本升级/系统设置）');

-- =====================================================================
-- Step 4: 新建 C 类型叶子菜单
-- =====================================================================
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 成本分摊管理下
(7547, '支出标签管理', 7542, 1, 'tagConfig', 'micro/tagConfig/index', NULL, 1, 0, 'C', '0', '0', 'micro:tagConfig:list', 'pc', 'price-tag', 'admin', NOW(), '★支出分类标签 CRUD + 排序（原固定支出配置→标签管理）'),
(7550, '分摊周期规则', 7542, 2, 'periodRule', 'micro/periodRule/index', NULL, 1, 0, 'C', '0', '0', 'micro:periodRule:list', 'pc', 'timer', 'admin', NOW(), '★按月/季度分摊规则 CRUD（原固定支出配置→周期规则管理）'),
-- 直属7500（员工提成管理，新菜单）
(7551, '员工提成管理', 7500, 4, 'staffCommission', 'micro/staffCommission/index', NULL, 1, 0, 'C', '0', '0', 'micro:staffCommission:list', 'pc', 'wallet', 'admin', NOW(), '★租户管理员：员工提成比例配置（新菜单，直属7500）'),
-- 小程序配置下
(7552, '小程序主题设置', 7544, 1, 'theme', 'micro/platform/theme/index', NULL, 1, 0, 'C', '0', '0', 'micro:platform:theme', 'pc', 'brush', 'admin', NOW(), '★小程序全局主题选择（原小程序主题，重命名+移入小程序配置目录）');

-- =====================================================================
-- Step 5: 更新既有菜单（改名/改图标/改父级）
-- =====================================================================
-- 5.1 项目管理 7511：parent 7510→7540（业务运营），order 1
UPDATE `sys_menu`
SET `parent_id` = 7540, `order_num` = 1,
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7511;

-- 5.2 房间/空间管理 7514 → 房间资源管理：parent 7510→7540（业务运营），icon build→house，order 2
UPDATE `sys_menu`
SET `menu_name` = '房间资源管理',
    `parent_id` = 7540, `order_num` = 2,
    `icon` = 'house',
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7514;

-- 5.2b 房间布局配置 7291：parent 7510→7540（业务运营），icon 更新为 layout，order 3
--     ★7291 由 M26_04 创建（原挂 7510 原子资源管理下），含子按钮 7292~7294
UPDATE `sys_menu`
SET `parent_id` = 7540, `order_num` = 3,
    `icon` = 'layout',
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7291;

-- 5.3 角色映射 7504 → 角色权限配置：parent 7510→7541（人员与权限），name/icon edit→post，order 2
UPDATE `sys_menu`
SET `menu_name` = '角色权限配置',
    `parent_id` = 7541, `order_num` = 2,
    `icon` = 'post',
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7504;

-- 5.4 人员管理 7507：parent 7510→7541（人员与权限），order 1
UPDATE `sys_menu`
SET `parent_id` = 7541, `order_num` = 1,
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7507;

-- 5.5 版本升级 7501：parent 7500→7543（系统运维），icon upload→refresh，order 1
UPDATE `sys_menu`
SET `parent_id` = 7543, `order_num` = 1,
    `icon` = 'refresh',
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7501;

-- 5.6 系统设置 7695：parent 7500→7543（系统运维），补 icon=setting，order 2
UPDATE `sys_menu`
SET `parent_id` = 7543, `order_num` = 2,
    `icon` = 'setting',
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7695;

-- 5.7 ★语音与合规配置 7610 → 语音配置：parent 7500→7545（AI语音合规），
--     icon shield→document，order 1（复用原三Tab页面，不新建）
UPDATE `sys_menu`
SET `menu_name` = '语音配置',
    `parent_id` = 7545, `order_num` = 1,
    `icon` = 'document',
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7610;

-- 5.8 ★策略开关 7630 保持直属 7500（与合规无关，独立菜单），仅更新 order_num
--     保留子按钮 7631 策略保存
UPDATE `sys_menu`
SET `order_num` = 5,
    `update_by` = 'admin', `update_time` = NOW()
WHERE `menu_id` = 7630;

-- =====================================================================
-- Step 6: 授予超管 role_id=1（新建目录+菜单 7540~7559）
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7540 AND 7559;

-- =====================================================================
-- Step 7: 存量 platform_admin 克隆角色补写 sys_role_menu（幂等 INSERT IGNORE）
-- =====================================================================
-- 7.1 通过 role_template.menu_keys → sys_menu.perms 匹配（C/F 类型菜单）
--     ★覆盖 7540~7559（新建菜单）+ 7511~7516（re-parent 的项目/房间菜单）+ 7291~7294（房间布局）
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
  AND m.`menu_id` IN (7291,7292,7293,7294, 7511,7512,7513,7514,7515,7516, 7540,7541,7542,7543,7544,7545,7547,7548,7550,7551,7552)
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`);

-- 7.2 ★M 类型目录（7540~7545）的 perms 为空，不会被 menu_keys→perms 匹配，
--     但 selectMenuTreeByUserId 依赖 sys_role_menu 找到中间目录节点才能组装完整树。
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_tenant` t
JOIN `sys_role` r ON r.`tenant_id` = t.`id` AND r.`role_key` = 'platform_admin'
CROSS JOIN `sys_menu` m
WHERE t.`tenant_type` = 'micro'
  AND m.`menu_id` IN (7540, 7541, 7542, 7543, 7544, 7545)
  AND m.`menu_type` = 'M'
  AND NOT EXISTS (SELECT 1 FROM `sys_role_menu` rm WHERE rm.`role_id` = r.`role_id` AND rm.`menu_id` = m.`menu_id`);

-- 7.3 ★7610/7630 改名+迁移后 perms 不变，已有 sys_role_menu 记录无需补录
--     7631(策略保存 F) parent 仍为 7630，自动跟随
-- 7.4 ★7511~7516/7291~7294 的 perms 已在 role_template 中（M15_23/M26_04 授权），
--     Step 7.1 已通过 perms 匹配补录，此处无需额外处理

-- =====================================================================
-- Step 8: 更新 role_template（platform_admin 追加新 perms，JSON_CONTAINS 守卫）
-- =====================================================================
-- 8.1 追加新权限到 micro/micro_lite/micro_pro 三档 platform_admin
--     ★7511 的 micro:platform:project + 7514 的 micro:platform:room（M15_23 已有）
--     ★7291 的 micro:room:layout（M26_04 已有）
--     ★7610 的 perms（rule/policy/sensitiveWord）已在 role_template 中，改名后不变
--     ★7630 的 perms（bookingPolicy:list/edit）已在 role_template 中，保持不变
--     ★仅需补 micro:staffCommission:list（7551 员工提成管理）
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:staffCommission:list')
WHERE `version_type` IN ('micro', 'micro_lite', 'micro_pro')
  AND `role_code` = 'platform_admin'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:staffCommission:list"');

-- =====================================================================
-- 验收查询
-- =====================================================================
-- A. 新目录结构完整（预期 6 个 M 类型目录）
-- SELECT menu_id, menu_name, parent_id, icon, menu_type
--   FROM sys_menu WHERE menu_id IN (7540,7541,7542,7543,7544,7545);
--   -- 预期 6 行，全部 menu_type='M'，parent_id=7500
--
-- B. 新叶子菜单已创建（预期 4 个 C 类型：7547/7550/7551/7552）
-- SELECT menu_id, menu_name, parent_id, icon, perms
--   FROM sys_menu WHERE menu_id IN (7547,7550,7551,7552);
--   -- 预期 4 行
--
-- C. 旧菜单已清理（7510/7620/7650 预期 0 行）
-- SELECT COUNT(*) AS old_remaining FROM sys_menu
--   WHERE menu_id IN (7510,7620,7650);
--   -- 预期 0
--
-- D. 7500 根目录 icon 已更新
-- SELECT menu_id, menu_name, icon FROM sys_menu WHERE menu_id = 7500;
--   -- 预期 icon='office'
--
-- E. 7610 已改名+迁移到 7545 下；7630 保持直属 7500
-- SELECT menu_id, menu_name, parent_id, icon, perms
--   FROM sys_menu WHERE menu_id IN (7610,7630,7631) ORDER BY menu_id;
--   -- 预期：7610 | 语音配置 | 7545 | document | (原 perms 不变)
--   --       7630 | 策略开关 | 7500 | date | micro:bookingPolicy:list
--   --       7631 | 策略保存 | 7630 | # | micro:bookingPolicy:edit
--
-- F. 既有菜单已正确迁移
-- SELECT menu_id, menu_name, parent_id, icon
--   FROM sys_menu WHERE menu_id IN (7501,7504,7507,7511,7514,7695)
--   ORDER BY menu_id;
--   -- 预期：7501 parent=7543 icon=refresh / 7504 parent=7541 name=角色权限配置 icon=post
--   --       7507 parent=7541 / 7511 parent=7540 / 7514 parent=7540 name=房间资源管理 icon=house
--   --       7695 parent=7543
--
-- G. 完整树形结构验证
-- SELECT m.menu_id, m.menu_name, m.parent_id, m.menu_type, m.icon, m.order_num
--   FROM sys_menu m WHERE m.parent_id = 7500 OR m.parent_id IN (
--     SELECT menu_id FROM sys_menu WHERE parent_id = 7500
--   ) ORDER BY m.parent_id, m.order_num;
--   -- 预期：7500 下 7 个子节点（7540~7545 + 7551），各子节点下各有对应叶子
--
-- H. 房间布局配置 7291 已迁移到业务运营(7540)下
-- SELECT menu_id, menu_name, parent_id, icon FROM sys_menu WHERE menu_id = 7291;
--   -- 预期：7291 | 房间布局配置 | 7540 | layout
--
-- I. role_template 含新权限
-- SELECT version_type,
--        JSON_CONTAINS(menu_keys,'"micro:staffCommission:list"') AS has_commission
--   FROM role_template WHERE role_code='platform_admin' AND status='0';
--   -- 预期：各档 1
-- =====================================================================
