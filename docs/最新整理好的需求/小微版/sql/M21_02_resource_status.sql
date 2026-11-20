-- =====================================================================
-- M21_02  资源状态流转 + Boss PC 状态管理页（T21.5 / T21.6，v1.68 新增）
-- 版本：v1.0  日期：2026-10-07
-- 依据：《资金管控系统开发任务文档（小微版）》v1.68 M21 里程碑 T21.5~T21.6
-- 前置依赖：M15_04（service_item 建表）、M15_06（sys_menu 7000~7499 区段）、
--           M15_07（role_template boss 配方）、M21_01（自助预约菜单 7200~7202）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. service_item 补 resource_status 列（idle/reserved/in_use 三态，默认 idle）
--   2. 新建 resource_status_log 表（资源状态变更日志，租户级）
--   3. sys_menu 新增 Boss PC 资源状态管理菜单 7203~7206（client_type=pc，挂 7100 小微运营）
--   4. role_template micro/micro_pro/micro_lite boss menu_keys 追加 4 个权限键
--   5. 存量 boss 角色 sys_role_menu 补录
-- 规范：
--   1. ALTER TABLE 使用 INFORMATION_SCHEMA 幂等（列已存在则跳过）
--   2. 先删后插幂等（菜单/角色关联）
--   3. menu_id 7203~7206 区段紧接 M21_01 的 7200~7202
--   4. ★资源状态三态：idle(空闲)→reserved(预约中)→in_use(使用中)→idle(结束/取消释放)
-- =====================================================================

-- ----------------------------
-- 1. service_item 补 resource_status 列（幂等：列已存在则跳过）
-- ----------------------------
DROP PROCEDURE IF EXISTS `_m21_02_add_resource_status`;
DELIMITER $$
CREATE PROCEDURE `_m21_02_add_resource_status`()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'service_item'
          AND COLUMN_NAME  = 'resource_status'
    ) THEN
        ALTER TABLE `service_item`
            ADD COLUMN `resource_status` VARCHAR(10) NOT NULL DEFAULT 'idle'
            COMMENT '★T21.5 资源状态：idle=空闲 / reserved=预约中 / in_use=使用中（仅 space 分类和助教有实际意义）'
            AFTER `status`;
    END IF;
END$$
DELIMITER ;
CALL `_m21_02_add_resource_status`();
DROP PROCEDURE IF EXISTS `_m21_02_add_resource_status`;

-- 存量数据兜底：确保已有记录的 resource_status 不为空
SET SQL_SAFE_UPDATES = 0;
UPDATE `service_item` SET `resource_status` = 'idle' WHERE `resource_status` IS NULL OR `resource_status` = '';
-- 执行完恢复打开
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------
-- 2. 新建 resource_status_log 表（资源状态变更日志，租户级）
-- ----------------------------
DROP TABLE IF EXISTS `resource_status_log`;
CREATE TABLE `resource_status_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `resource_type` VARCHAR(20) NOT NULL COMMENT '资源类型：staff=助教 / room=房间',
  `resource_id` BIGINT NOT NULL COMMENT '资源ID（助教=sys_user.user_id / 房间=service_item.id）',
  `resource_name` VARCHAR(100) DEFAULT NULL COMMENT '资源名称（冗余快照，便于日志查看）',
  `from_status` VARCHAR(10) DEFAULT NULL COMMENT '变更前状态：idle/reserved/in_use',
  `to_status` VARCHAR(10) NOT NULL COMMENT '变更后状态：idle/reserved/in_use',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人 user_id（系统自动操作为 NULL）',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `reason` VARCHAR(255) DEFAULT NULL COMMENT '变更原因（手动释放需填写/预约提交/活动取消等）',
  `booking_id` BIGINT DEFAULT NULL COMMENT '关联 booking_order.id（预约触发时填写，手动释放为 NULL）',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_resource` (`tenant_id`, `resource_type`, `resource_id`),
  INDEX `idx_tenant_time` (`tenant_id`, `operate_time`),
  INDEX `idx_booking` (`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源状态变更日志（T21.5 资源状态流转留痕；按 tenant_id 隔离）';

-- ----------------------------
-- 3. sys_menu Boss PC 资源状态管理菜单（7203~7206，client_type=pc，挂 7100 小微运营）
--    ★先删后插幂等
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` IN (7203, 7204, 7205, 7206);

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
-- 7203: 助教状态管理（C 类型页面）
(7203, '助教状态管理', 7100, 70, 'resourceStatus/staff', 'micro/resourceStatus/staff/index', 1, 0, 'C', '0', '0', 'micro:resourceStatus:staff:view', 'pc', 'peoples', 'admin', NOW(), 'T21.6 Boss PC 助教状态列表（空闲🟢/预约中🟡/使用中🔴）+ 手动释放'),
-- 7204: 房间状态管理（C 类型页面）
(7204, '房间状态管理', 7100, 71, 'resourceStatus/room', 'micro/resourceStatus/room/index', 1, 0, 'C', '0', '0', 'micro:resourceStatus:room:view', 'pc', 'international', 'admin', NOW(), 'T21.6 Boss PC 房间状态列表（空闲🟢/预约中🟡/使用中🔴）+ 手动释放'),
-- 7205: 手动释放资源（F 按钮）
(7205, '手动释放资源', 7100, 1, '', NULL, 1, 0, 'F', '0', '0', 'micro:resourceStatus:release', 'pc', '#', 'admin', NOW(), 'T21.6 将预约中资源手动改为空闲（需填写原因）'),
-- 7206: 查看状态日志（F 按钮）
(7206, '查看状态日志', 7100, 2, '', NULL, 1, 0, 'F', '0', '0', 'micro:resourceStatus:log:list', 'pc', '#', 'admin', NOW(), 'T21.5 查看资源状态变更日志');

-- ----------------------------
-- 4. role_template boss 追加权限（micro/micro_pro/micro_lite 三档均追加）
--    ★幂等：NOT JSON_CONTAINS 守卫
-- ----------------------------
-- 4.1 micro_pro boss
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:staff:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:staff:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:room:view')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:room:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:release')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:release"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:log:list')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:log:list"');

-- 4.2 micro (lite) boss
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:staff:view')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:staff:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:room:view')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:room:view"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:release')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:release"');

UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:resourceStatus:log:list')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'boss' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:resourceStatus:log:list"');

-- ----------------------------
-- 5. 存量 boss 角色 sys_role_menu 补录（7203~7206 + 7100 祖先目录）
-- ----------------------------
-- 5.1 所有小微版 boss 克隆角色补录新菜单
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'boss' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7203, 7204, 7205, 7206, 7100);

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 service_item.resource_status 列已存在
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_item' AND COLUMN_NAME = 'resource_status';
-- 预期：1 行，varchar(10)，default 'idle'
--
-- 6.2 resource_status_log 表已创建
-- SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'resource_status_log';
-- 预期：1
--
-- 6.3 菜单 7203~7206 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu WHERE menu_id IN (7203, 7204, 7205, 7206);
-- 预期：4 行，C/C/F/F，client_type=pc
--
-- 6.4 role_template micro_pro boss 含新权限
-- SELECT JSON_CONTAINS(menu_keys, '"micro:resourceStatus:staff:view"') AS has_staff,
--        JSON_CONTAINS(menu_keys, '"micro:resourceStatus:room:view"') AS has_room,
--        JSON_CONTAINS(menu_keys, '"micro:resourceStatus:release"') AS has_release,
--        JSON_CONTAINS(menu_keys, '"micro:resourceStatus:log:list"') AS has_log
--   FROM role_template WHERE version_type = 'micro_pro' AND role_code = 'boss';
-- 预期：1/1/1/1
--
-- 6.5 存量 boss 已授权新菜单
-- SELECT r.role_id, r.tenant_id, COUNT(DISTINCT rm.menu_id) AS new_menu_count
--   FROM sys_role r JOIN sys_role_menu rm ON rm.role_id = r.role_id
--  WHERE r.role_key = 'boss' AND rm.menu_id IN (7203, 7204, 7205, 7206)
--  GROUP BY r.role_id, r.tenant_id;
-- 预期：每个小微版 boss 角色 4 个新菜单
