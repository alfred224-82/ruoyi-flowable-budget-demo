-- =====================================================================
-- M29_02  活动「应收/实收」拆分列扩展 + 存量补跑（REQ-02，v1.3 需求清单）
-- =====================================================================
-- 需求概述：
--   Boss 首页收益拆「应收 / 实收」双值：
--   · 应收(receivable_amount)：活动生成且【首次有人报名】时一次锁定，
--     = COALESCE(custom_price, auto_calculated_price)（活动总价），
--     存续期内不再随后续报名变动（一次锁定口径，已与用户对齐）。
--   · 实收(received_amount)：活动结束（autoEndActivities）后回写；
--     received_confirmed_time 记录回写时间。
--     ★口径说明：活动报名走 project_signup（不产生 cash_flow，且 cash_flow
--       无 activity 关联类型），故实收按「收款确认口径」计算：
--       received = 人均应付(receivable/participant_count) × 已支付(PAID)报名人数，
--       上限不超过 receivable。详见 PromotionActivityServiceImpl.writeBackReceivedAmount。
--   · 有人参加的活动应收必有值；未结束时实收显示 0。
--
-- 前置：
--   M15_11（promotion_activity 表）
--   M22_06（promotion_activity custom_price/auto_calculated_price/participant_count 列）
--
-- 幂等：条件 ALTER（INFORMATION_SCHEMA 守卫 + PREPARE）+ 条件 UPDATE 存量补跑
-- ★本脚本仅加列 + 补历史应收，不改动既有业务数据。
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. promotion_activity 补「应收/实收」3 列
-- =====================================================================
SET @tbl = 'promotion_activity';

-- 1.1 receivable_amount（应收金额，首次报名锁定）
SET @col = 'receivable_amount';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` DECIMAL(12,2) DEFAULT NULL COMMENT ''应收金额（首次有人报名时锁定=活动总价；NULL=尚无人报名）'' AFTER `is_locked`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.2 received_amount（实收金额，活动结束回写）
SET @col = 'received_amount';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` DECIMAL(12,2) DEFAULT 0.00 COMMENT ''实收金额（活动结束后按收款确认口径回写；未结束为0）'' AFTER `receivable_amount`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 1.3 received_confirmed_time（实收确认/回写时间）
SET @col = 'received_confirmed_time';
SET @sql = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = @tbl AND COLUMN_NAME = @col) = 0,
    CONCAT('ALTER TABLE `', @tbl, '` ADD COLUMN `', @col,
           '` DATETIME DEFAULT NULL COMMENT ''实收回写确认时间（活动结束时置 NOW()）'' AFTER `received_amount`'),
    'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- 2. 存量补跑：历史「已有人报名」活动回填应收金额（一次锁定口径）
--    ★仅补 receivable_amount IS NULL 且 participant_count>0 的活动；幂等
-- =====================================================================
UPDATE `promotion_activity`
SET `receivable_amount` = COALESCE(`custom_price`, `auto_calculated_price`)
WHERE `receivable_amount` IS NULL
  AND COALESCE(`participant_count`, 0) > 0
  AND COALESCE(`custom_price`, `auto_calculated_price`) IS NOT NULL;

-- =====================================================================
-- 3. 验收查询（执行后人工核对）
-- =====================================================================
-- 3.1 三列就绪
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'promotion_activity'
--   AND COLUMN_NAME IN ('receivable_amount','received_amount','received_confirmed_time');
-- 预期：3 行

-- 3.2 存量补跑：已有人报名的活动应收全部非空
-- SELECT COUNT(*) AS should_be_zero FROM promotion_activity
-- WHERE COALESCE(participant_count,0) > 0 AND receivable_amount IS NULL
--   AND COALESCE(custom_price, auto_calculated_price) IS NOT NULL;
-- 预期：0

-- 3.3 已结束活动实收回写情况（跑完 M29_02 后由 autoEndActivities 逐步填充）
-- SELECT status, COUNT(*) total,
--        SUM(CASE WHEN received_confirmed_time IS NOT NULL THEN 1 ELSE 0 END) confirmed
-- FROM promotion_activity GROUP BY status;
-- =====================================================================
