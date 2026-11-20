-- =====================================================================
-- M15_05 小微版预约/结算/流水表建表脚本（T15.5，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第十一章（业务闭环）/ 第十二章（V1 零支付合规）/ 第十六章
--       《资金管控系统开发任务文档》v1.13 T15.5 / T15.12 / T15.13 / T15.14 / T15.23
--       《多业态SaaS平台需求规格说明书》v1.0 第 16 章（V1 零支付合规设计）/ 第 18 章（预约/结算业务闭环）
-- 前置依赖：M15_04（service_item）、M15_01（user_tenant_role）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）
--   2. 四表均含 tenant_id + idx_tenant_id + 日期索引（租户级表）
--   3. ★V1 零支付合规：pay_status 枚举只有「待到店付/已收款」「待发放/已线下发放」，
--      【无「已支付」在线支付态】；收款线下完成仅标记，结算只记账不打款，全系统无支付 SDK
--   4. 业务闭环：顾客预约(待到店付) → 老板标记已收款(写 cash_flow 收入) → 完成服务 →
--      周期结算(待发放) → 老板线下转账后标记已线下发放(写 cash_flow 支出) → 日营收快照
--   5. cash_flow 为纯记账口径流水（income/expense），不是支付流水
-- =====================================================================

-- ----------------------------
-- 1. 预约订单表 booking_order（顾客预约 → 到店付，V1 无在线支付）
-- ----------------------------
DROP TABLE IF EXISTS `booking_order`;
CREATE TABLE `booking_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `order_no` VARCHAR(40) DEFAULT NULL COMMENT '订单编号',
  `customer_user_id` BIGINT DEFAULT NULL COMMENT '顾客用户ID(可空=游客预约，记手机号)',
  `customer_phone` VARCHAR(20) DEFAULT NULL COMMENT '顾客手机号',
  `staff_user_id` BIGINT DEFAULT NULL COMMENT '服务员工/助教用户ID',
  `service_item_id` BIGINT DEFAULT NULL COMMENT '服务项目ID(FK→service_item)',
  `service_name` VARCHAR(100) DEFAULT NULL COMMENT '服务项目名称快照',
  `booking_time` DATETIME DEFAULT NULL COMMENT '预约到店时间',
  `quantity` DECIMAL(12,2) DEFAULT 1.00 COMMENT '数量/时长(按计价方式)',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAY' COMMENT '收款状态：PENDING_PAY(待到店付)/RECEIVED(已收款)；★无在线支付态',
  `order_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_SERVICE' COMMENT '订单状态：PENDING_SERVICE(待服务)/COMPLETED(已完成)/CANCELLED(已取消)',
  `pay_time` DATETIME DEFAULT NULL COMMENT '老板标记已收款时间(线下收款)',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成服务时间',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_customer` (`customer_user_id`),
  KEY `idx_staff` (`staff_user_id`),
  KEY `idx_tenant_booking_time` (`tenant_id`, `booking_time`),
  KEY `idx_tenant_status` (`tenant_id`, `order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约订单表(到店付,V1无在线支付)';

-- ----------------------------
-- 2. 人员结算表 staff_settlement（V1 只记账不打款：待发放/已线下发放）
-- ----------------------------
DROP TABLE IF EXISTS `staff_settlement`;
CREATE TABLE `staff_settlement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `settle_no` VARCHAR(40) DEFAULT NULL COMMENT '结算单号',
  `staff_user_id` BIGINT NOT NULL COMMENT '被结算员工/助教用户ID',
  `staff_name` VARCHAR(64) DEFAULT NULL COMMENT '员工姓名快照',
  `period_start` DATE NOT NULL COMMENT '结算周期起',
  `period_end` DATE NOT NULL COMMENT '结算周期止',
  `total_orders` INT DEFAULT 0 COMMENT '周期内完成单量',
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '应发金额(自动汇总算出)',
  `pay_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '发放状态：PENDING(待发放)/PAID_OFFLINE(已线下发放)；★仅标记不调支付接口',
  `marked_by` VARCHAR(64) DEFAULT NULL COMMENT '标记人(老板线下转账后点此)',
  `marked_by_id` BIGINT DEFAULT NULL COMMENT '标记人用户ID',
  `marked_time` DATETIME DEFAULT NULL COMMENT '标记已线下发放时间',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_staff` (`staff_user_id`),
  KEY `idx_tenant_period` (`tenant_id`, `period_start`, `period_end`),
  KEY `idx_tenant_status` (`tenant_id`, `pay_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员结算表(V1只记账不打款;待发放/已线下发放)';

-- ----------------------------
-- 3. 收支流水表 cash_flow（纯记账口径，非支付流水）
-- ----------------------------
DROP TABLE IF EXISTS `cash_flow`;
CREATE TABLE `cash_flow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `flow_type` VARCHAR(20) NOT NULL COMMENT '流水类型：income(收入)/expense(支出)',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '收支分类(订单收入/工资结算/房租/水电/采购...)',
  `amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `related_order_id` BIGINT DEFAULT NULL COMMENT '关联单据ID(booking_order/staff_settlement)',
  `related_type` VARCHAR(30) DEFAULT NULL COMMENT '关联单据类型(booking/settlement/manual)',
  `occur_date` DATE NOT NULL COMMENT '发生日期',
  `create_by` VARCHAR(64) COMMENT '创建者(手动补录人)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_type_date` (`tenant_id`, `flow_type`, `occur_date`),
  KEY `idx_tenant_date` (`tenant_id`, `occur_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支流水表(纯记账口径;income/expense)';

-- ----------------------------
-- 4. 每日营收快照表 micro_daily_revenue（microDailyRevenueJob 每日生成）
-- ----------------------------
DROP TABLE IF EXISTS `micro_daily_revenue`;
CREATE TABLE `micro_daily_revenue` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `revenue_date` DATE NOT NULL COMMENT '营收日期',
  `gross_income` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日总收入',
  `total_expense` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日总支出',
  `net_profit` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '当日净利润(收入-支出)',
  `order_count` INT DEFAULT 0 COMMENT '当日订单数',
  `snapshot_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '快照生成时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  UNIQUE KEY `uk_tenant_date` (`tenant_id`, `revenue_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日营收快照表(microDailyRevenueJob生成)';

-- ----------------------------
-- 5. 验收查询（执行后人工核对）
-- ----------------------------
-- 5.1 四表均含 tenant_id + 日期索引
-- SELECT TABLE_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'tenant_id'
--    AND TABLE_NAME IN ('booking_order','staff_settlement','cash_flow','micro_daily_revenue');  -- 预期 4 行
--
-- 5.2 ★V1 零支付：pay_status 枚举无「已支付」在线态
-- SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
--   AND ((TABLE_NAME='booking_order' AND COLUMN_NAME='pay_status')
--     OR (TABLE_NAME='staff_settlement' AND COLUMN_NAME='pay_status'));
-- 预期：booking_order=PENDING_PAY/RECEIVED；staff_settlement=PENDING/PAID_OFFLINE
--
-- 5.3 micro_daily_revenue 支持按日期查快照（uk_tenant_date 幂等 upsert）
-- SELECT revenue_date, gross_income, total_expense, net_profit FROM micro_daily_revenue
--  WHERE tenant_id = <租户ID> ORDER BY revenue_date DESC LIMIT 7;
--
-- 5.4 业务闭环验证（运行时）：
--   预约(待到店付) → 标记已收款(cash_flow income) → 完成服务 →
--   结算(待发放) → 标记已线下发放(cash_flow expense) → 日营收快照
