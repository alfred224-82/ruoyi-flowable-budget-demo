-- =====================================================================
-- M16_03 首单识别建表 + FEAT_PROMOTION_DISTRIBUTION 功能开关脚本（T16.4 / T16.8 / T16.12，v1.16 新增）
-- 版本：v1.0  日期：2026-09-10
-- 依据：《推广分销与激励方案》v1.0 第七章（活动与首单识别规则）/ 第二节（版本适用范围）
--       《资金管控系统开发任务文档》v1.16 T16.4 / T16.8 / T16.12
-- 前置依赖：M1_05（feature_flag 表 + 33 项开关；本脚本追加第 34 项）、M15_11（promotion_activity 活动表，activity_id 关联）、
--           M16_01（promotion_attribution 归因；首单判定与归属解耦）、M16_02（拉新奖励发放）
-- ★执行顺序：本脚本须在 M1_05 之后执行（M1_05 先删后插 33 项开关，本脚本仅追加第 34 项，不动前 33 项）。
-- ★版本适用硬约束（方案第二节 + T16.12）：FEAT_PROMOTION_DISTRIBUTION 为推广分销总开关，
--   basic/advanced/pro 三列均 = 0（基础版/高级版/专业版不生效），仅「小微版」启用；
--   四个校验点（生成推广码/扫码归因/拉新奖励发放/老板后台配置）统一走本开关，禁止散落版本判断（规范第 18 条）。
-- 规范：
--   1. first_order_record 为【租户级表】(含 tenant_id)，多租户插件自动隔离；feature_flag 为【系统级共享表】(无 tenant_id)
--   2. ★首单以手机号为唯一识别维度、按活动独立判断：UNIQUE(tenant_id,activity_id,customer_phone)
--      （同一手机号同一活动仅一条=已享受过；不同活动独立计数，防重复薅羊毛）
--   3. ★首单判定（管优惠）与推广归属（管业绩）两条线独立互不干扰：换码/换推广人不影响「已享受活动」事实
--   4. feature_flag 仅 INSERT 第 34 项，先删后插幂等（DELETE WHERE code=... 再 INSERT，不影响 M1_05 的 33 项）
--   5. 先删后建 / 先删后插幂等
-- =====================================================================

-- ----------------------------
-- 1. 首单识别记录表 first_order_record（按手机号 + 按活动独立判断是否首次）
-- ----------------------------
DROP TABLE IF EXISTS `first_order_record`;
CREATE TABLE `first_order_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `activity_id` BIGINT NOT NULL COMMENT '活动ID(指向 promotion_activity.id;每个活动独立判断是否首次)',
  `customer_phone` VARCHAR(20) NOT NULL COMMENT '★顾客手机号(首单唯一识别维度;同一手机号=同一顾客)',
  `customer_user_id` BIGINT DEFAULT NULL COMMENT '顾客用户ID(冗余,便于关联归因)',
  `first_order_id` BIGINT DEFAULT NULL COMMENT '首次消费订单ID(booking_order.id)',
  `consume_time` DATETIME DEFAULT NULL COMMENT '首次消费时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_activity_phone` (`tenant_id`, `activity_id`, `customer_phone`),
  KEY `idx_tenant_phone` (`tenant_id`, `customer_phone`),
  KEY `idx_tenant_activity` (`tenant_id`, `activity_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='首单识别记录表(手机号唯一识别;按活动独立判断;防重复薅羊毛;按tenant_id隔离)';

-- ----------------------------
-- 2. 功能开关 FEAT_PROMOTION_DISTRIBUTION（feature_flag 第 34 项，组 E=小微版专属）
--    ★basic/advanced/pro 三列均 = 0（三版本不生效）；仅小微版启用——
--      小微版启用路径由 Java 层按 tenant_type=micro / version_type∈{micro_lite,micro_pro} 判定，
--      feature_flag 本行作为「推广分销总开关」供 @RequireFeature("FEAT_PROMOTION_DISTRIBUTION") 统一校验；
--      ★不给 feature_flag 增列（该表为企业版/小微版共享，M16 仅归入小微版/sql，增列会破坏未部署本脚本的版本）。
--    ★先删后插幂等：仅删本 code，不动 M1_05 的 33 项。
-- ----------------------------
DELETE FROM `feature_flag` WHERE `code` = 'FEAT_PROMOTION_DISTRIBUTION';
INSERT INTO `feature_flag`
  (`code`, `name`, `flag_group`, `biz_code`, `basic_enabled`, `advanced_enabled`, `pro_enabled`, `status`, `create_by`, `create_time`, `remark`) VALUES
('FEAT_PROMOTION_DISTRIBUTION', '推广分销与激励(推广码/归因/拉新奖励/首单识别)', 'E', NULL, 0, 0, 0, '0', 'admin', NOW(),
 'T16.12；★仅小微版启用(tenant_type=micro/version_type∈micro_lite,micro_pro)；basic/advanced/pro 均不生效；四校验点统一走本开关');

-- ----------------------------
-- 3. 后端接口/校验约定（Java 交付，登记供部署核对，不入本 SQL）
--    T16.8 首单判定：顾客扫码进店→准备下单/参加活动→查 first_order_record(tenant_id,activity_id,customer_phone)：
--          无记录→首客→享受首次活动优惠(如8折/立减/送礼品)+写 first_order_record；有记录→老客→仅常规优惠
--          ★与推广归属解耦：判定只看手机号+活动，不看扫了谁的码；换码不影响首客资格
--    T16.12 版本开关与功能校验：
--          ① M16 全部接口标注 @RequireFeature("FEAT_PROMOTION_DISTRIBUTION")；
--          ② 四校验点(生成推广码/扫码归因/拉新奖励发放/老板后台配置)统一走开关；
--          ③ 非小微版租户调任一推广接口返回「当前版本不支持此功能」；
--          ④ 老板后台推广菜单(7180~7188)按开关渲染,非小微版不可见；
--          ⑤ @TenantJob 推广相关任务(如拉新结算/券过期)标注 versions={"micro"}
-- ----------------------------

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 first_order_record 含 tenant_id + UNIQUE(tenant_id,activity_id,customer_phone)
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='first_order_record' AND COLUMN_NAME='tenant_id';  -- 预期 1 行
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='first_order_record' AND INDEX_NAME='uk_tenant_activity_phone';  -- 预期 uk_tenant_activity_phone
--
-- 4.2 ★FEAT_PROMOTION_DISTRIBUTION 落表且三版本均 disabled
-- SELECT code, flag_group, basic_enabled, advanced_enabled, pro_enabled, status FROM feature_flag
--  WHERE code='FEAT_PROMOTION_DISTRIBUTION';  -- 预期 1 行：E / 0 / 0 / 0 / 0(启用)
--
-- 4.3 ★开关总数 34 项（M1_05 的 33 项 + 本脚本 1 项），组 E=1
-- SELECT COUNT(*) FROM feature_flag;                          -- 预期 34
-- SELECT flag_group, COUNT(*) FROM feature_flag GROUP BY flag_group;  -- 预期 A=11/B=9/C=6/D=7/E=1
--
-- 4.4 ★未破坏 M1_05 既有 33 项（组 A~D 数量不变）
-- SELECT SUM(basic_enabled) basic_cnt, SUM(advanced_enabled) adv_cnt, SUM(pro_enabled) pro_cnt FROM feature_flag;
-- 预期：basic=0 / advanced=23 / pro=33（FEAT_PROMOTION_DISTRIBUTION 三列均 0，不改变既有合计）
--
-- 4.5 ★首单判定闭环（运行时,T16.8 部署后）：
--   ① 手机号 X 首次参加活动 A 消费 → first_order_record 写入(tenant,A,X) → 享首单优惠
--   ② 手机号 X 再次参加活动 A → 命中 uk_tenant_activity_phone → 仅常规优惠（不重复给首单）
--   ③ 手机号 X 参加活动 B（不同活动）→ 独立判断,可享 B 的首单优惠（按活动独立计数）
--   ④ ★换码/换推广人不影响：X 扫不同推广人码,首客资格只由(tenant,activity,phone)决定,与归属解耦
--   ⑤ ★跨租户隔离：A 店首单记录不出现在 B 店（tenant_id 过滤生效）
--   ⑥ ★非小微版租户调首单/推广接口 → FEAT_PROMOTION_DISTRIBUTION 拦截,返回「当前版本不支持此功能」
