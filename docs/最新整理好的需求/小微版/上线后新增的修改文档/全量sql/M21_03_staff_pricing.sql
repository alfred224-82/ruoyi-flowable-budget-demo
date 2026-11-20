-- =====================================================================
-- M21_03  助教/房间价格设置（T21.7，v1.69 新增）
-- 版本：v1.0  日期：2026-10-07
-- 依据：用户需求——Boss 可随时设置/修改助教和房间价格（按小时计费），
--       顾客在自助预约选择时可以看到价格。
-- 前置依赖：M15_04（service_item 建表，房间价格已有 price 字段）、
--           M15_07（role_template boss 配方）、M21_01（自助预约菜单 7200~7202）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 新建 staff_pricing 表（助教小时价格，租户级）
--      ★房间价格复用 service_item.price（已有字段，无需新建）
--   2. sys_menu 新增 Boss PC「资源价格设置」菜单 7207~7209（client_type=pc，挂 7100 小微运营）
--   3. role_template micro/micro_pro/micro_lite boss menu_keys 追加 3 个权限键
--   4. 存量 boss 角色 sys_role_menu 补录
-- 规范：
--   1. 先删后插幂等（菜单/角色关联）
--   2. menu_id 7207~7209 区段紧接 M21_02 的 7203~7206
--   3. ★价格按小时计费，Boss 随时可改，顾客可见
-- =====================================================================

-- ----------------------------
-- 1. 新建 staff_pricing 表（助教小时价格，租户级）
--    ★房间价格已在 service_item.price（无需新建表）
-- ----------------------------
DROP TABLE IF EXISTS `staff_pricing`;
CREATE TABLE `staff_pricing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '助教用户ID（FK→sys_user.user_id）',
  `hourly_rate` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '★小时单价（元/小时）',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用（0停用 1启用）',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注（如：资深助教/特价等）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`, `user_id`),
  INDEX `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='助教小时价格设置（T21.7 Boss 随时设置/修改，顾客预约时可见；按小时计费）';

-- ----------------------------
-- 2. sys_menu Boss PC「资源价格设置」菜单（7207~7209，client_type=pc，挂 7100 小微运营）
--    ★先删后插幂等
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7207, 7208, 7209);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7207: 资源价格设置（C 类型页面，统一设置助教+房间价格）
(7207, '资源价格设置', 7100, 72, 'resourcePrice', 'micro/resourcePrice/index', 1, 0, 'C', '0', '0', 'micro:resourcePrice:view', 'pc', 'money', 'admin', NOW(), 'T21.7 Boss PC 设置助教/房间小时单价（顾客预约时可见）'),
-- 7208: 设置助教价格（F 按钮）
(7208, '设置助教价格', 7100, 3, '', NULL, 1, 0, 'F', '0', '0', 'micro:resourcePrice:staff:set', 'pc', '#', 'admin', NOW(), 'T21.7 设置/修改助教小时单价'),
-- 7209: 设置房间价格（F 按钮）
(7209, '设置房间价格', 7100, 4, '', NULL, 1, 0, 'F', '0', '0', 'micro:resourcePrice:room:set', 'pc', '#', 'admin', NOW(), 'T21.7 设置/修改房间小时单价');

-- ----------------------------
-- 3. role_template boss 追加权限（micro/micro_pro/micro_lite 三档均追加）
--    ★幂等：NOT JSON_CONTAINS 守卫
-- ----------------------------
-- 3.1 micro_pro boss
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourcePrice:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourcePrice:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourcePrice:staff:set')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourcePrice:staff:set"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourcePrice:room:set')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourcePrice:room:set"');

-- 3.2 micro_lite boss
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourcePrice:view')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourcePrice:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourcePrice:staff:set')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourcePrice:staff:set"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourcePrice:room:set')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourcePrice:room:set"');

-- ----------------------------
-- 4. 存量 boss 角色 sys_role_menu 补录（7207~7209 + 7100 祖先目录）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7207, 7208, 7209, 7100);

-- ----------------------------
-- 5. 验收查询（执行后人工核对）
-- ----------------------------
-- 5.1 staff_pricing 表已创建
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'staff_pricing';
-- 预期：1
--
-- 5.2 菜单 7207~7209 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu WHERE menu_id IN (7207, 7208, 7209);
-- 预期：3 行，C/F/F，client_type=pc
--
-- 5.3 role_template micro_pro boss 含新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:resourcePrice:view"') AS has_view,
--        JSON_CONTAINS(menu_keys, '"micro:resourcePrice:staff:set"') AS has_staff,
--        JSON_CONTAINS(menu_keys, '"micro:resourcePrice:room:set"') AS has_room
--   FROM role_template WHERE version_type = 'micro_pro' AND role_code = 'boss';
-- 预期：1/1/1
--
-- 5.4 存量 boss 已授权新菜单
-- SELECT r.role_id, r.tenant_id, COUNT(DISTINCT rm.menu_id) AS new_menu_count
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.role_key = 'boss' AND rm.menu_id IN (7207, 7208, 7209)
--  GROUP BY r.role_id, r.tenant_id;
-- 预期：每个小微版 boss 角色 3 个新菜单
