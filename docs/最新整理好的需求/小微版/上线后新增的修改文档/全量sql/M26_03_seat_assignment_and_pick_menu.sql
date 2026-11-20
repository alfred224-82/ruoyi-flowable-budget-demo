-- =====================================================================
-- M26_03  活动座位占用 + 操作日志 + 顾客选座菜单（T26.7 / T26.8，v1.0）
-- 版本：v1.0  日期：2026-10-12
-- 依据：《小微版 V3.9 开发文档》v1.0 M26 里程碑 T26.7（顾客小程序按位置选座）
--        + T26.8（活动座位占用表 + 操作日志表，联动 M28）
-- 前置依赖：
--   M15_01（role_template 标准角色集）
--   M15_06（sys_menu.client_type + 小微版菜单 7000~7499，其中 7300=小程序·顾客）
--   M15_07/M15_10（micro / micro_lite / micro_pro 三档角色模板）
--   M16_01（promotion_activity 主表，activity_seat_assignment.activity_id FK 逻辑关联）
--   ★T26.1 room_seat_layout / room_seat 表由 M26_01 建立（本文档范围外），
--     若尚未落地，本脚本创建的 activity_seat_assignment 表仍可独立使用（seat_id 允许 NULL 或占位）
-- ---------------------------------------------------------------------
-- 本脚本内容：
--   1. 新建 activity_seat_assignment 表（活动座位占用，租户级；UNIQUE(activity_id, seat_id) 保证一座位一人）
--   2. 新建 activity_seat_operation_log 表（活动座位操作日志，联动 M28 T28.4）
--   3. sys_menu 新增 7290（顾客小程序选座 F 按钮，perms=micro:booking:pickSeat，client_type=mini）
--   4. role_template 追加权限键 micro:booking:pickSeat：
--      - customer(micro / micro_lite / micro_pro) 均追加（选座为通用能力）
--   5. 存量 customer 克隆角色 sys_role_menu 补录 7290 + 7300 祖先目录
--   6. 超管 role_id=1 补录 7290（便于平台侧调试）
-- 规范：
--   1. 建表 CREATE TABLE IF NOT EXISTS 语义（本脚本自建表允许 DROP+CREATE 幂等）
--   2. ALTER 使用 INFORMATION_SCHEMA 守卫 + SQL_SAFE_UPDATES 包裹（本脚本无 ALTER）
--   3. 菜单先删后插幂等；role_template JSON_CONTAINS 守卫；sys_role_menu INSERT IGNORE
--   4. 审计列（create_by / create_time / update_by / update_time）继承 BaseEntity 规范
--   5. tenant_id 显式声明，MetaObjectHandler 自动填充；本表非 IGNORE_TENANT_TABLES 白名单
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 新建 activity_seat_assignment 表（活动座位占用，T26.8 / T28.1）
--    ★UNIQUE(activity_id, seat_id) 保证一座位一人；seat_id 允许 NULL 表示预留未指派
--    ★assignment_type 值域：PRESET=预置人员 / BOSS_ASSIGN=Boss 指派 / CUSTOMER_PICK=顾客自选
--    ★locked 字段活动锁定后=1，禁止改动（联动 M28 T28.1）
-- ----------------------------
DROP TABLE IF EXISTS `activity_seat_assignment`;
CREATE TABLE `activity_seat_assignment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID（FK→promotion_activity.id）',
  `seat_id` BIGINT DEFAULT NULL COMMENT '座位ID（FK→room_seat.id；NULL=预留未指派）',
  `seat_no` VARCHAR(32) DEFAULT NULL COMMENT '座位号冗余（便于查询与前端展示，与 room_seat.seat_no 一致）',
  `user_id` BIGINT DEFAULT NULL COMMENT '占座人 user_id（FK→sys_user.user_id；NULL=预留但未指派）',
  `assignment_type` VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER_PICK' COMMENT 'PRESET=预置人员 / BOSS_ASSIGN=Boss 指派 / CUSTOMER_PICK=顾客自选',
  `assign_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '占座时间',
  `locked` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '活动锁定后=1，禁止改动（M28 T28.1）',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_activity_seat` (`activity_id`, `seat_id`),
  INDEX `idx_tenant_activity` (`tenant_id`, `activity_id`),
  INDEX `idx_user` (`user_id`),
  INDEX `idx_seat` (`seat_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动座位占用表（T26.8 / T28.1；一座位一人 UNIQUE(activity_id,seat_id)）';

-- ----------------------------
-- 2. 新建 activity_seat_operation_log 表（活动座位操作日志，T26.8 / T28.4）
--    ★operation_type 值域：ASSIGN / MOVE / SWAP / LOCK / UNLOCK / COPY_LAYOUT / PRESET / RELEASE
--    ★before_snapshot / after_snapshot 为 JSON 字符串，可回放操作前后状态
--    ★BE 使用 @Aspect 切面自动记录（M28 T28.4）
-- ----------------------------
DROP TABLE IF EXISTS `activity_seat_operation_log`;
CREATE TABLE `activity_seat_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID（FK→promotion_activity.id）',
  `operation_type` VARCHAR(20) NOT NULL COMMENT 'ASSIGN/MOVE/SWAP/LOCK/UNLOCK/COPY_LAYOUT/PRESET/RELEASE',
  `operator_user_id` BIGINT DEFAULT NULL COMMENT '操作人 user_id（FK→sys_user.user_id）',
  `operator_role` VARCHAR(30) DEFAULT NULL COMMENT '操作人角色：boss/salesman/customer',
  `before_snapshot` TEXT DEFAULT NULL COMMENT '操作前快照 JSON（含 seat_id / user_id / assignment_type）',
  `after_snapshot` TEXT DEFAULT NULL COMMENT '操作后快照 JSON',
  `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注（如解锁原因）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_tenant_activity` (`tenant_id`, `activity_id`),
  INDEX `idx_operator` (`operator_user_id`),
  INDEX `idx_operate_time` (`operate_time`),
  INDEX `idx_operation_type` (`operation_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动座位操作日志表（T26.8 / T28.4；含 before/after JSON 快照，可回放）';

-- ----------------------------
-- 3. sys_menu 顾客选座菜单（7290，F 按钮，client_type=mini，挂 7300 小程序·顾客）
--    ★先删后插幂等
--    ★menu_id 段位 7290~7299 归 M26 顾客选座（避开 M24 7250~7254 与 M25 7260~7275）
-- ----------------------------
DELETE FROM `sys_menu` WHERE `menu_id` = 7290;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7290, '选座', 7300, 20, '', NULL, 1, 0, 'F', '0', '0', 'micro:booking:pickSeat', 'mini', '#', 'admin', NOW(), 'T26.7 顾客小程序按位置选座（读取 room_seat_layout + room_seat + activity_seat_assignment）');

-- ----------------------------
-- 4. role_template 追加权限键 micro:booking:pickSeat
--    ★幂等：NOT JSON_CONTAINS 守卫
--    ★customer(micro / micro_lite / micro_pro) 均追加（选座为通用能力，不受版本限制）
-- ----------------------------
-- 4.1 micro_pro customer
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:booking:pickSeat')
WHERE `version_type` = 'micro_pro' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:booking:pickSeat"');

-- 4.2 micro_lite customer
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:booking:pickSeat')
WHERE `version_type` = 'micro_lite' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:booking:pickSeat"');

-- 4.3 micro (旧档，兼容存量) customer
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:booking:pickSeat')
WHERE `version_type` = 'micro' AND `role_code` = 'customer' AND `status` = '0'
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:booking:pickSeat"');

-- ----------------------------
-- 5. 存量 customer 克隆角色 sys_role_menu 补录 7290 + 7300 祖先目录
--    ★INSERT IGNORE 幂等；覆盖所有小微版租户下的 customer 克隆角色
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.`role_id`, m.`menu_id`
FROM `sys_role` r
CROSS JOIN `sys_menu` m
JOIN `sys_tenant` t ON r.`tenant_id` = t.`id` AND t.`tenant_type` = 'micro'
WHERE r.`role_key` = 'customer' AND r.`del_flag` = '0'
  AND m.`menu_id` IN (7290, 7300);

-- ----------------------------
-- 6. 超管 role_id=1 补录 7290（便于平台侧调试）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu` WHERE `menu_id` = 7290;

-- ----------------------------
-- 7. 验收查询（执行后人工核对）
-- ----------------------------
-- 7.1 activity_seat_assignment / activity_seat_operation_log 两表已创建
-- SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('activity_seat_assignment','activity_seat_operation_log');
-- 预期：2 行
--
-- 7.2 activity_seat_assignment UNIQUE 约束存在
-- SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE FROM INFORMATION_SCHEMA.STATISTICS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'activity_seat_assignment' AND INDEX_NAME = 'uk_activity_seat';
-- 预期：2 行（activity_id + seat_id），NON_UNIQUE=0
--
-- 7.3 sys_menu 7290 存在且挂 7300 下，client_type=mini
-- SELECT menu_id, menu_name, parent_id, menu_type, perms, client_type FROM sys_menu WHERE menu_id = 7290;
-- 预期：1 行（选座 / 7300 / F / micro:booking:pickSeat / mini）
--
-- 7.4 role_template 三档 customer 均含 micro:booking:pickSeat
-- SELECT version_type, JSON_CONTAINS(menu_keys, '"micro:booking:pickSeat"') AS has_pick
--   FROM role_template WHERE role_code = 'customer' AND status = '0';
-- 预期：micro / micro_lite / micro_pro 三行，has_pick=1
--
-- 7.5 存量小微版租户 customer 克隆角色已授权 7290
-- SELECT COUNT(*) AS granted_count FROM sys_role_menu rm
--   JOIN sys_role r ON rm.role_id = r.role_id AND r.role_key = 'customer' AND r.del_flag = '0'
--   JOIN sys_tenant t ON r.tenant_id = t.id AND t.tenant_type = 'micro'
--  WHERE rm.menu_id = 7290;
-- 预期：>= 小微版租户数量
--
-- ★执行后请重启后端 + 退出重新登录，刷新 Sa-Token 权限快照
