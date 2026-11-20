-- =====================================================================
-- M25_06 投诉记录表新增被投诉员工字段（v1.0）
-- 版本：v1.0  日期：2026-10-14
-- ---------------------------------------------------------------------
-- 【问题背景】
--   投诉页「关联活动」仅展示非 stopped 状态的活动（本地缓存过滤），
--   且顾客无法直接投诉员工（必须关联活动）。
-- 【修复内容】
--   1. complaint_record 表新增 staff_id / staff_name 两列（被投诉员工）；
--   2. 前端投诉页改用 API 加载顾客参与的全部活动（不限状态）；
--   3. 前端投诉页新增「投诉员工」选填下拉（本租户全部 salesman，不过滤空闲状态）。
-- 【执行后必做】
--   1. 重启后端 ruoyi-admin
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. complaint_record：新增 staff_id / staff_name 两列（幂等守卫）
-- ---------------------------------------------------------------------
DROP PROCEDURE IF EXISTS `_m25_06_alter_complaint_record`;
DELIMITER $$
CREATE PROCEDURE `_m25_06_alter_complaint_record`()
BEGIN
    -- staff_id
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'complaint_record'
          AND COLUMN_NAME  = 'staff_id'
    ) THEN
        ALTER TABLE `complaint_record`
            ADD COLUMN `staff_id` BIGINT DEFAULT NULL COMMENT '被投诉员工 user_id（可空=不指定员工）' AFTER `activity_id`;
    END IF;

    -- staff_name
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME   = 'complaint_record'
          AND COLUMN_NAME  = 'staff_name'
    ) THEN
        ALTER TABLE `complaint_record`
            ADD COLUMN `staff_name` VARCHAR(50) DEFAULT NULL COMMENT '被投诉员工姓名（冗余快照）' AFTER `staff_id`;
    END IF;
END$$
DELIMITER ;
CALL `_m25_06_alter_complaint_record`();
DROP PROCEDURE IF EXISTS `_m25_06_alter_complaint_record`;
