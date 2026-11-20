-- =====================================================================
-- M18_03 预约策略配置建表脚本（T18.10，v1.29 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18
--       T18.10 预约策略配置（BE+PC）
-- 前置依赖：M15_06（micro 菜单）、M18_02（标签/周期规则菜单 7620~7629）
-- 规范：
--   1. booking_policy 为【租户级表】(含 tenant_id)，多租户插件自动隔离
--   2. 每租户一条策略（UNIQUE tenant_id），upsert 模式
--   3. 继承 BaseEntity 审计列
--   4. 先删后建幂等
--   5. menu_id 占用 7630~7639 区段
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 预约策略配置表 booking_policy
--    租户级预约策略：顾客自助预约开关 / 提前天数 / 取消政策 / 爽约阈值
-- ----------------------------
DROP TABLE IF EXISTS `booking_policy`;
CREATE TABLE `booking_policy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID（UNIQUE，每租户一条策略）',
  `customer_self_booking` TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许顾客自助预约：1=允许 0=不允许',
  `advance_booking_days` INT NOT NULL DEFAULT 7 COMMENT '提前预约天数（0=当天可约，默认7天）',
  `cancel_policy` VARCHAR(20) NOT NULL DEFAULT 'FREE' COMMENT '取消政策：FREE(免费取消)/FEE(收费取消)/FORBID(禁止取消)',
  `cancel_free_hours` INT NOT NULL DEFAULT 2 COMMENT '免费取消截止小时数（距预约时间前N小时可免费取消）',
  `no_show_threshold` INT NOT NULL DEFAULT 3 COMMENT '爽约次数阈值（达到后自动限制预约权限）',
  `auto_confirm` TINYINT NOT NULL DEFAULT 0 COMMENT '是否自动确认预约：1=自动 0=需老板确认',
  `max_daily_bookings` INT DEFAULT NULL COMMENT '每日最大预约数（NULL=不限）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约策略配置表(租户级;每租户一条;upsert模式)';

-- ----------------------------
-- 2. 菜单 SQL（租户管理员 PC，menu_id 7630~7639）
--    ★先删后插幂等
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7630 AND 7639;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7630 AND 7639;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7630, '策略开关', 7500, 25, 'bookingPolicy', 'micro/bookingPolicy/index', NULL, 1, 0, 'C', '0', '0', 'micro:bookingPolicy:list', 'pc', 'date', 'admin', NOW(), '★租户管理员：预约策略配置'),
(7631, '策略保存', 7630, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:bookingPolicy:edit', 'pc', '#', 'admin', NOW(), '★预约策略保存/更新按钮（对齐 MicroBookingPolicyController.savePolicy 的 @SaCheckPermission）');

-- 给 platform_admin 角色补写 role_menu（存量租户）
-- ★INSERT IGNORE 幂等
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE r.role_key = 'platform_admin'
  AND m.menu_id BETWEEN 7630 AND 7639;
