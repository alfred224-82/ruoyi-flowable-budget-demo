-- =====================================================================
-- M22_01  活动费用与参与增强——数据模型扩展 + 菜单 + 权限（v1.0）
-- =====================================================================
-- 需求概述：
--   Boss 创建活动时的费用模型改为：房间单价×时长 + 助教单价×时长 = 总价
--   Boss 可自定义价格覆盖自动计算值；总价 / 设定人数 = 每人应付金额
--   开团前 30 分钟通过系统消息通知每人应付金额（到店支付）
--   Boss 可锁定活动（人数不可增减）
--   顾客/助教的活动列表扩展为展示所有参与的活动
--
-- 前置：
--   [26]M15_11（promotion_activity 表）
--   [31]M15_16（project_signup 表）
--   [68]M19_07（promotion_activity 活动设置增强列）
--
-- 幂等：条件 ALTER（INFORMATION_SCHEMA 守卫 + PREPARE）+ INSERT IGNORE + JSON_CONTAINS 守卫
-- ★执行后请退出重新登录，刷新 Sa-Token 权限快照
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. promotion_activity 补活动费用与参与增强列
-- =====================================================================

-- 1.1 custom_price（Boss 自定义总价）
SET @tbl = 'promotion_activity';
SET @col = 'custom_price';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` DECIMAL(12,2) DEFAULT NULL COMMENT ''Boss自定义总价（为空则用auto_calculated_price）'' AFTER `selected_staff_ids`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 auto_calculated_price（系统自动计算总价）
SET @col = 'auto_calculated_price';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` DECIMAL(12,2) DEFAULT NULL COMMENT ''系统自动计算总价（房间+助教×时长）'' AFTER `custom_price`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 participant_count（Boss 设定的参与人数）
SET @col = 'participant_count';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` INT DEFAULT NULL COMMENT ''Boss设定的参与人数（用于计算人均费用）'' AFTER `auto_calculated_price`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.4 is_locked（是否锁定）
SET @col = 'is_locked';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''是否锁定（0=未锁定,1=已锁定；锁定后不可报名/取消）'' AFTER `participant_count`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. project_signup 补支付状态列
-- =====================================================================

-- 2.1 pay_status（支付状态）
SET @tbl2 = 'project_signup';
SET @col2 = 'pay_status';
SET @sql2 = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl2 AND COLUMN_NAME = @col2) = 0,
    CONCAT('ALTER TABLE `', @tbl2, '` ADD COLUMN `', @col2,
           '` VARCHAR(20) NOT NULL DEFAULT ''PENDING_PAY'' COMMENT ''支付状态：PENDING_PAY(待到店付)/PAID(已支付)/REMIND_SENT(已提醒)'''),
    'SELECT 1'
);
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- =====================================================================
-- 3. sys_menu 锁定按钮权限（menu_id 7159，F 类型，挂 7150 活动管理目录下）
--    ★先删后插幂等
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` = 7159;
DELETE FROM `sys_menu`      WHERE `menu_id` = 7159;

INSERT INTO `sys_menu`
  (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7159, '锁定活动', 7150, 5, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:promo:lock', 'pc', '#', 'admin', NOW(), '★M22 活动锁定/解锁按钮权限');

-- =====================================================================
-- 4. role_template boss 追加 micro:promo:lock 权限（三个版本均追加）
--    ★JSON_ARRAY_APPEND + NOT JSON_CONTAINS 守卫，幂等
-- =====================================================================
SET SQL_SAFE_UPDATES = 0;
UPDATE `role_template`
SET `menu_keys` = JSON_ARRAY_APPEND(`menu_keys`, '$', 'micro:promo:lock')
WHERE `role_code` = 'boss'
  AND `version_type` IN ('micro', 'micro_pro', 'micro_lite')
  AND NOT JSON_CONTAINS(`menu_keys`, '"micro:promo:lock"');
SET SQL_SAFE_UPDATES = 1;

-- =====================================================================
-- 5. 存量租户 boss sys_role_menu 补录 7159 + 祖先目录 7150
-- =====================================================================
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 7159
FROM `sys_role` r
WHERE r.role_key = 'boss'
  AND r.del_flag = '0';

-- ★同时补 M 祖先目录（7150 活动管理），确保按钮权限可见
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, 7150
FROM `sys_role` r
WHERE r.role_key = 'boss'
  AND r.del_flag = '0';

-- =====================================================================
-- 6. 验收查询（执行后人工核对）
-- =====================================================================

-- 6.1 promotion_activity 新增列就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--   AND COLUMN_NAME IN ('custom_price','auto_calculated_price','participant_count','is_locked');
-- 预期：4 行

-- 6.2 project_signup 新增 pay_status 列就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_signup'
--   AND COLUMN_NAME = 'pay_status';
-- 预期：1 行，DEFAULT='PENDING_PAY'

-- 6.3 菜单 7159 存在
-- SELECT menu_id, menu_name, parent_id, menu_type, perms FROM sys_menu WHERE menu_id = 7159;
-- 预期：1 行，F 类型，perms=micro:promo:lock

-- 6.4 role_template boss 含锁定权限
-- SELECT version_type, role_code,
--        JSON_CONTAINS(menu_keys, '"micro:promo:lock"') AS has_lock
-- FROM role_template WHERE role_code = 'boss' AND version_type IN ('micro','micro_pro','micro_lite');
-- 预期：3 行，has_lock=1

-- 6.5 存量租户 boss 已关联锁定菜单
-- SELECT t.company_name,
--        SUM(CASE WHEN rm.menu_id = 7159 THEN 1 ELSE 0 END) AS has_lock_btn,
--        SUM(CASE WHEN rm.menu_id = 7150 THEN 1 ELSE 0 END) AS has_parent_menu
-- FROM sys_tenant t
-- JOIN sys_role r ON r.tenant_id = t.id AND r.role_key = 'boss' AND r.del_flag = '0'
-- JOIN sys_role_menu rm ON rm.role_id = r.role_id
-- WHERE t.tenant_type = 'micro'
-- GROUP BY t.id, t.company_name;
-- 预期：每行 has_lock_btn=1, has_parent_menu=1

-- =====================================================================
-- 7. xxl_job_info 注册活动支付提醒定时任务（每 5 分钟）
--    ★先删后插幂等
-- =====================================================================
DELETE FROM `xxl_job_info` WHERE `executor_handler` = 'activityPayReminderJob';
INSERT INTO `xxl_job_info` (
    `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
    `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
    `executor_handler`, `executor_param`, `executor_block_strategy`,
    `executor_timeout`, `executor_fail_retry_count`,
    `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
    `child_jobid`, `trigger_status`, `trigger_last_time`, `trigger_next_time`
) VALUES (
    1, '★M22 活动开团前支付提醒（扫描即将开始的已锁定活动，通知参与者人均费用）', NOW(), NOW(), 'ruoyi', '',
    'CRON', '0 */5 * * * ?', 'DO_NOTHING', 'FIRST',
    'activityPayReminderJob', '', 'SERIAL_EXECUTION',
    0, 1, 'BEAN', '', '@TenantJob(versions={"micro"})', NOW(), '', 1, 0, 0
);
