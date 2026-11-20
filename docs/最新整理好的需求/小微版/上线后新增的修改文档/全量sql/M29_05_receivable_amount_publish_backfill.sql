-- =====================================================================
-- M29_05  活动应收金额发布时锁定（存量数据回填，v1.3 修复）
-- =====================================================================
-- 需求概述：
--   应收金额语义调整：活动发布时即锁定 receivable_amount = COALESCE(custom_price, auto_calculated_price)，
--   不再等首次报名才锁定。本脚本回填存量已发布活动的 receivable_amount。
--
-- 前置：
--   [139]M29_02（promotion_activity 含 receivable_amount / received_amount / received_confirmed_time）
--   [99]M22_06（promotion_activity 含 custom_price / auto_calculated_price）
--
-- 幂等：仅当 receivable_amount IS NULL 且状态为 PUBLISHED 时更新，可重复执行
-- =====================================================================
SET NAMES utf8mb4;

-- =====================================================================
-- 1. 回填：已发布活动 + receivable_amount 为空 → 用 COALESCE(custom_price, auto_calculated_price)
-- =====================================================================
UPDATE promotion_activity
SET receivable_amount = COALESCE(custom_price, auto_calculated_price),
    update_time = NOW(),
    update_by = 'M29_05_backfill'
WHERE del_flag = '0'
  AND status = 'PUBLISHED'
  AND receivable_amount IS NULL
  AND COALESCE(custom_price, auto_calculated_price) IS NOT NULL;

-- =====================================================================
-- 2. 验收
-- =====================================================================
-- 2.1 回填后不应存在已发布活动 receivable_amount 为空（预期 0 行）
-- SELECT COUNT(*) FROM promotion_activity
--  WHERE del_flag = '0' AND status = 'PUBLISHED' AND receivable_amount IS NULL;
--
-- 2.2 回填行数（应 > 0，视存量数据而定）
-- SELECT COUNT(*) AS backfilled FROM promotion_activity
--  WHERE del_flag = '0' AND update_by = 'M29_05_backfill';
