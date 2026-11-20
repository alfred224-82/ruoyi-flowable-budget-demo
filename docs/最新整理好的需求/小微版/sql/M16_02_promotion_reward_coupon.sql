-- =====================================================================
-- M16_02 拉新奖励配置/发放 + 优惠券建表脚本（T16.2 / T16.3 / T16.7，v1.16 新增）
-- 版本：v1.0  日期：2026-09-10
-- 依据：《推广分销与激励方案》v1.0 第六章（业绩与奖励规则）/ 第八章（消费回传通知）
--       《资金管控系统开发任务文档》v1.16 T16.2 / T16.3 / T16.7
-- 前置依赖：M16_01（promotion_attribution 归因关系）、M15_05（staff_settlement / cash_flow 现金奖励记账口径）、
--           M16_03（FEAT_PROMOTION_DISTRIBUTION 开关；奖励发放前校验版本）
-- ★版本适用硬约束：仅「小微版」启用；拉新奖励发放为四个校验点之一，统一走 FEAT_PROMOTION_DISTRIBUTION，禁止散落版本判断。
-- ★V1 零支付延续（规范第 13 条）：员工现金奖励不调任何支付/转账接口，走「标记已线下发放」记账口径
--   （issue_status=PENDING→PAID_OFFLINE），可联动 M15 staff_settlement / cash_flow；顾客奖励为站内优惠券（非现金）。
-- 规范：
--   1. 四表均为【租户级表】(含 tenant_id)，多租户插件自动隔离，不入 IGNORE_TENANT_TABLES
--   2. ★奖励规则固定不可改：promotion_reward_config.trigger_rule 恒为 'NEW_CUSTOMER'(拉新)，唯一可变项为奖励内容
--      （reward_type/reward_value/coupon_id）；员工配置 reward_type=cash、顾客配置 reward_type=coupon
--   3. ★拉新奖励仅一次：promotion_reward_record 以 UNIQUE(tenant_id,customer_user_id) 保证同一被拉新顾客只触发一次；
--      后续二次消费/活动消费不再给推广人奖励（活动优惠只给顾客本人，走 coupon_record.source=activity）
--   4. coupon_record.source 区分 promotion(拉新发放) / activity(活动领取)；expire_time = receive_time + coupon.valid_days
--   5. 先删后建 / 先删后插幂等
-- =====================================================================

-- ----------------------------
-- 1. 拉新奖励配置表 promotion_reward_config（老板后台配置；奖励内容唯一可变，规则固定）
-- ----------------------------
DROP TABLE IF EXISTS `promotion_reward_config`;
CREATE TABLE `promotion_reward_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `target_role` VARCHAR(20) NOT NULL COMMENT '奖励对象：staff(员工/业务员,发现金)/customer(顾客,发优惠券)',
  `reward_type` VARCHAR(20) NOT NULL COMMENT '奖励形式：cash(现金,员工)/coupon(优惠券,顾客)',
  `reward_value` DECIMAL(12,2) DEFAULT NULL COMMENT '奖励内容(唯一可变项)：cash=金额(元)；coupon=折扣/面额,由 coupon_id 定义',
  `coupon_id` BIGINT DEFAULT NULL COMMENT '顾客奖励关联的优惠券ID(reward_type=coupon 时必填,指向 coupon.id)',
  `trigger_rule` VARCHAR(30) NOT NULL DEFAULT 'NEW_CUSTOMER' COMMENT '★触发规则(固定不可改)：NEW_CUSTOMER(拉新=被推广顾客首次消费)',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_target_role` (`tenant_id`, `target_role`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拉新奖励配置表(奖励内容唯一可变;trigger_rule固定=拉新;按tenant_id隔离)';

-- ----------------------------
-- 2. 拉新奖励发放记录表 promotion_reward_record（首次消费触发，仅一次）
-- ----------------------------
DROP TABLE IF EXISTS `promotion_reward_record`;
CREATE TABLE `promotion_reward_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `promoter_user_id` BIGINT NOT NULL COMMENT '推广人用户ID(受奖人)',
  `promoter_role_code` VARCHAR(30) NOT NULL COMMENT '推广人角色：salesman(员工,现金)/customer(顾客,优惠券)/boss',
  `customer_user_id` BIGINT NOT NULL COMMENT '被拉新顾客用户ID',
  `first_order_id` BIGINT DEFAULT NULL COMMENT '触发拉新的首次消费订单ID(booking_order.id)',
  `reward_type` VARCHAR(20) NOT NULL COMMENT '奖励形式：cash/coupon(快照自配置)',
  `reward_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '现金奖励金额(reward_type=cash;顾客优惠券记 coupon_record)',
  `coupon_record_id` BIGINT DEFAULT NULL COMMENT '优惠券发放记录ID(reward_type=coupon,指向 coupon_record.id)',
  `issue_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '★发放状态(V1零支付)：PENDING(待发放)/PAID_OFFLINE(已线下发放)/ISSUED(优惠券已到账)',
  `issued_time` DATETIME DEFAULT NULL COMMENT '发放时间(现金=老板标记已线下发放时间;优惠券=系统自动到账时间)',
  `notified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已回传通知推广人小程序：0否 1是(T16.9 走 M13 IMessageProvider)',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_customer` (`tenant_id`, `customer_user_id`),
  KEY `idx_tenant_promoter` (`tenant_id`, `promoter_user_id`),
  KEY `idx_tenant_issue_status` (`tenant_id`, `issue_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拉新奖励发放记录表(首消触发仅一次;现金走线下发放/优惠券到账;按tenant_id隔离)';

-- ----------------------------
-- 3. 优惠券定义表 coupon（站内券；顾客拉新奖励 / 活动优惠的券模板）
-- ----------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `name` VARCHAR(120) NOT NULL COMMENT '优惠券名称(如 8折券/送礼品券)',
  `type` VARCHAR(20) NOT NULL DEFAULT 'discount' COMMENT '券类型：discount(折扣)/gift(赠品)/cash(代金)',
  `value` DECIMAL(12,2) DEFAULT NULL COMMENT '券值：discount=折扣(如8.0折)/cash=面额(元)；gift 由 remark 说明',
  `valid_days` INT NOT NULL DEFAULT 30 COMMENT '领取后有效天数(expire_time=receive_time+valid_days)',
  `total` INT DEFAULT NULL COMMENT '发行总量(NULL=不限量)',
  `issued_count` INT NOT NULL DEFAULT 0 COMMENT '已发放数量',
  `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0启用 1停用',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_type` (`tenant_id`, `type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券定义表(站内券模板;顾客拉新奖励/活动优惠;按tenant_id隔离)';

-- ----------------------------
-- 4. 顾客券记录表 coupon_record（发放到顾客账户；区分拉新发放与活动领取）
-- ----------------------------
DROP TABLE IF EXISTS `coupon_record`;
CREATE TABLE `coupon_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `user_id` BIGINT NOT NULL COMMENT '持券顾客用户ID',
  `coupon_id` BIGINT NOT NULL COMMENT '优惠券ID(指向 coupon.id)',
  `source` VARCHAR(20) NOT NULL DEFAULT 'promotion' COMMENT '来源：promotion(拉新奖励系统发放)/activity(活动领取)',
  `status` VARCHAR(20) NOT NULL DEFAULT 'UNUSED' COMMENT '状态：UNUSED(未使用)/USED(已使用)/EXPIRED(已过期)',
  `receive_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '到账/领取时间',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间(=receive_time+coupon.valid_days)',
  `used_time` DATETIME DEFAULT NULL COMMENT '使用时间',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_user` (`tenant_id`, `user_id`),
  KEY `idx_tenant_coupon` (`tenant_id`, `coupon_id`),
  KEY `idx_tenant_status` (`tenant_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='顾客券记录表(拉新发放/活动领取;未使用/已使用/已过期;按tenant_id隔离)';

-- ----------------------------
-- 5. 后端接口/结算约定（Java 交付，登记供部署核对，不入本 SQL）
--    ★拉新奖励结算(T16.7)全部标注 @RequireFeature("FEAT_PROMOTION_DISTRIBUTION")；发放前校验版本开关
--    触发：被推广顾客「首次消费」(first_order_record 命中,见 M16_03) → 查 promotion_attribution 取当时推广人 →
--          按 promoter_role_code 取 promotion_reward_config：
--            ├ salesman/boss(员工) → reward_type=cash → 写 promotion_reward_record(issue_status=PENDING)
--            │     → 老板「标记已线下发放」→ PAID_OFFLINE + 记 cash_flow/staff_settlement(★不调支付接口)
--            └ customer(顾客)      → reward_type=coupon → 写 coupon_record(source=promotion) + promotion_reward_record(ISSUED)
--          → 回传通知推广人小程序(T16.9,notified=1,走 M13 IMessageProvider 微信订阅消息)
--    奖励内容配置(T16.10④)：PUT /micro/distribution/rewardConfig（micro:distribution:editReward,菜单 7185）
--          只改 reward_value/coupon_id/reward_type,trigger_rule 恒 NEW_CUSTOMER 不可改
--    ★后续二次消费/活动消费不再触发 promotion_reward_record（uk_tenant_customer 保证仅一次）
-- ----------------------------

-- ----------------------------
-- 6. 验收查询（执行后人工核对）
-- ----------------------------
-- 6.1 四张表均含 tenant_id
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND COLUMN_NAME='tenant_id'
--   AND TABLE_NAME IN ('promotion_reward_config','promotion_reward_record','coupon','coupon_record');  -- 预期 4 行
--
-- 6.2 ★trigger_rule 固定为拉新（默认值 NEW_CUSTOMER，规则不可改）
-- SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_reward_config' AND COLUMN_NAME='trigger_rule';  -- 预期 NEW_CUSTOMER
--
-- 6.3 ★拉新奖励仅一次：promotion_reward_record UNIQUE(tenant_id,customer_user_id)
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_reward_record' AND INDEX_NAME='uk_tenant_customer';  -- 预期 uk_tenant_customer
--
-- 6.4 ★V1 零支付：现金奖励发放状态无在线支付态（仅 PENDING/PAID_OFFLINE/ISSUED）
-- SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='promotion_reward_record' AND COLUMN_NAME='issue_status';  -- 预期含 PAID_OFFLINE,不含 wxpay/transfer
--
-- 6.5 coupon_record.source 区分拉新发放与活动领取
-- SELECT COLUMN_COMMENT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='coupon_record' AND COLUMN_NAME='source';  -- 预期含 promotion/activity
--
-- 6.6 ★奖励结算闭环（运行时,T16.7 部署后）：
--   ① 员工推广顾客首消 → promotion_reward_record(reward_type=cash,issue_status=PENDING)
--      → 老板标记已线下发放 → PAID_OFFLINE + cash_flow 记账（无任何支付接口调用）→ 员工小程序收到「获得推广奖励 ¥XX」
--   ② 顾客推广顾客首消 → coupon_record(source=promotion) + promotion_reward_record(ISSUED)
--      → 顾客小程序收到「获得优惠券 XX」
--   ③ 同一顾客二次消费 → 不再写 promotion_reward_record（uk_tenant_customer 拦截）；活动优惠只给顾客本人(source=activity)
--   ④ ★跨租户隔离：A 店奖励配置/发放/券不出现在 B 店（tenant_id 过滤生效）
