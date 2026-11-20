-- =====================================================================
-- M18_04 拉新奖励配置扩展建表脚本（T18.12，v1.29 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18
--       T18.12 老板拉新奖励配置（BE+PC）
-- 前置依赖：M16_02（promotion_reward_config 表）、M16_01（菜单 7184 种子）
-- 规范：
--   1. ALTER TABLE 增量加列（不重建表，保留既有数据）
--   2. 新增 trigger_condition / reward_limit_type / reward_limit_value 三列
--   3. 菜单 menu_id 占用 7640~7649 区段（M18 新增菜单）
--   4. 先删后建幂等
-- =====================================================================

SET NAMES utf8mb4;

-- ===== 1. ALTER TABLE：promotion_reward_config 加列 =====

-- ① trigger_condition：触发条件（NEW_CUSTOMER_BIND=新客绑定 / NEW_CUSTOMER_FIRST_ORDER=新客首单）
--   默认 NEW_CUSTOMER_FIRST_ORDER（延续 M16 语义：被推广顾客首次消费才触发奖励）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_reward_config'
    AND COLUMN_NAME = 'trigger_condition');
SET @sql := IF(@exist = 0,
    'ALTER TABLE `promotion_reward_config` ADD COLUMN `trigger_condition` VARCHAR(30) NOT NULL DEFAULT ''NEW_CUSTOMER_FIRST_ORDER'' COMMENT ''触发条件：NEW_CUSTOMER_BIND(新客绑定)/NEW_CUSTOMER_FIRST_ORDER(新客首单)'' AFTER `trigger_rule`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② reward_limit_type：奖励上限周期（NONE=不限 / DAILY=每日 / MONTHLY=每月）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_reward_config'
    AND COLUMN_NAME = 'reward_limit_type');
SET @sql := IF(@exist = 0,
    'ALTER TABLE `promotion_reward_config` ADD COLUMN `reward_limit_type` VARCHAR(10) NOT NULL DEFAULT ''NONE'' COMMENT ''奖励上限周期：NONE(不限)/DAILY(每日)/MONTHLY(每月)'' AFTER `trigger_condition`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ③ reward_limit_value：奖励上限次数（reward_limit_type != NONE 时生效）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_reward_config'
    AND COLUMN_NAME = 'reward_limit_value');
SET @sql := IF(@exist = 0,
    'ALTER TABLE `promotion_reward_config` ADD COLUMN `reward_limit_value` INT DEFAULT NULL COMMENT ''奖励上限次数（reward_limit_type!=NONE时必填，>=1）'' AFTER `reward_limit_type`',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ===== 2. 菜单 SQL（老板 PC 端拉新奖励配置页，menu_id 7640~7649）=====
-- 父菜单：7180（推广分销管理，M16 已建）
-- ★ 更新既有 7184 菜单的 component 路径，使其指向 T18.12 新页面

DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7640 AND 7649;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7640 AND 7649;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7640, '拉新奖励配置', 7180, 10, 'rewardConfig', 'micro/distribution/rewardConfig/index', NULL, 1, 0, 'C', '0', '0', 'micro:distribution:rewardConfig', 'pc', 'money', 'admin', NOW(), '★T18.12 老板拉新奖励配置：开关/奖励形式/触发条件/上限');

-- 角色菜单关联（micro_pro 角色，假设 role_id=1 为超管/micro_pro）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE m.menu_id BETWEEN 7640 AND 7649
  AND r.role_key = 'micro_pro';

-- ===== 3. 更新既有 7184 菜单 component（如有）=====
UPDATE `sys_menu` SET `component` = 'micro/distribution/rewardConfig/index'
WHERE `menu_id` = 7184 AND (`component` IS NULL OR `component` = '' OR `component` = '#');

-- ===== 验证 =====
-- SELECT * FROM sys_menu WHERE menu_id BETWEEN 7640 AND 7649;
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_DEFAULT FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_reward_config'
--   AND COLUMN_NAME IN ('trigger_condition','reward_limit_type','reward_limit_value');
