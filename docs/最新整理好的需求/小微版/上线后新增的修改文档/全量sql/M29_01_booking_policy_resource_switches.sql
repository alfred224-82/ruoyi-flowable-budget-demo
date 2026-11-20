-- =====================================================================
-- M29_01  自助预约「三类资源控制」开关列扩展（REQ-01，v1.3 需求清单）
-- =====================================================================
-- 需求概述：
--   Boss → 系统设置 → 自助预约 页面新增「三类资源控制」区块：
--   项目 / 场地 / 员工，每类两个开关（对客户是否开放 + 是否必填）。
--   · 桌游(board_game)：三类自由搭配。
--   · 旅游(travel)/按摩(massage)/足浴(foot_bath)：项目锁死必选
--     （后端 savePolicy/queryPolicy 强制 project_booking_enabled=1、
--      project_booking_required=1，前端置灰只读展示）。
--   · 客户端（App 顾客端）按 policy 隐藏未开放资源入口 / 必填校验，
--     三类均不提供「新建/自定义」入口。
--
-- 前置：
--   M18_03（booking_policy 表 + 菜单 7630~7631）
--
-- 幂等：条件 ALTER（INFORMATION_SCHEMA 守卫 + PREPARE/EXECUTE）
-- ★booking_policy 为租户级表，每租户一条策略（upsert）；本脚本仅加列，不动数据。
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. booking_policy 补「三类资源控制」6 列
--    默认：全部对客户开放(enabled=1)、全部非必填(required=0)
-- =====================================================================
SET @tbl = 'booking_policy';

-- 1.1 project_booking_enabled（项目：是否对客户开放）
SET @col = 'project_booking_enabled';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT NOT NULL DEFAULT 1 COMMENT ''项目资源是否对客户开放：1=开放 0=隐藏'' AFTER `max_daily_bookings`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 project_booking_required（项目：是否必填）
SET @col = 'project_booking_required';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT NOT NULL DEFAULT 0 COMMENT ''项目资源是否必填：1=必填 0=可选（锁死行业强制1）'' AFTER `project_booking_enabled`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 room_booking_enabled（场地：是否对客户开放）
SET @col = 'room_booking_enabled';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT NOT NULL DEFAULT 1 COMMENT ''场地资源是否对客户开放：1=开放 0=隐藏'' AFTER `project_booking_required`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 room_booking_required（场地：是否必填）
SET @col = 'room_booking_required';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT NOT NULL DEFAULT 0 COMMENT ''场地资源是否必填：1=必填 0=可选'' AFTER `room_booking_enabled`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.5 staff_booking_enabled（员工：是否对客户开放）
SET @col = 'staff_booking_enabled';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT NOT NULL DEFAULT 1 COMMENT ''员工资源是否对客户开放：1=开放 0=隐藏'' AFTER `room_booking_required`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.6 staff_booking_required（员工：是否必填）
SET @col = 'staff_booking_required';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT NOT NULL DEFAULT 0 COMMENT ''员工资源是否必填：1=必填 0=可选'' AFTER `staff_booking_enabled`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. 存量数据兜底：已有策略行确保 6 列有非空默认值（enabled=1/required=0）
--    ★DEFAULT 已保证新列非空，此处仅防御历史 NULL（幂等）
-- =====================================================================
UPDATE `booking_policy`
SET `project_booking_enabled` = 1 WHERE `project_booking_enabled` IS NULL;
UPDATE `booking_policy`
SET `project_booking_required` = 0 WHERE `project_booking_required` IS NULL;
UPDATE `booking_policy`
SET `room_booking_enabled` = 1 WHERE `room_booking_enabled` IS NULL;
UPDATE `booking_policy`
SET `room_booking_required` = 0 WHERE `room_booking_required` IS NULL;
UPDATE `booking_policy`
SET `staff_booking_enabled` = 1 WHERE `staff_booking_enabled` IS NULL;
UPDATE `booking_policy`
SET `staff_booking_required` = 0 WHERE `staff_booking_required` IS NULL;

-- =====================================================================
-- 3. 验收查询（执行后人工核对）
-- =====================================================================
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_policy'
--   AND COLUMN_NAME IN ('project_booking_enabled','project_booking_required',
--                       'room_booking_enabled','room_booking_required',
--                       'staff_booking_enabled','staff_booking_required');
-- 预期：6 行；enabled 列 DEFAULT=1，required 列 DEFAULT=0
-- =====================================================================
