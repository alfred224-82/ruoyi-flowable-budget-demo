-- =====================================================================
-- M20_11 业务异常已处理确认表（异常处理中心「已处理却还存在」修复，v1.68 新增）
-- 版本：v1.0  日期：2026-10-02
-- 依据：Bug 反馈：老板在异常处理中心对「固定支出异常（本月未配置固定支出项）」
--       点击「已处理」后刷新，异常仍然存在。
--       根因：FIXED_COST_ANOMALY 为派生异常（无物理来源记录，id=0），
--             getExceptions() 每次按「本月无启用固定支出」实时重新推导，
--             而 resolveException() 对该类型仅打日志、不落任何状态 → 「已处理」必然失效。
-- 方案：新增租户级确认表 micro_exception_ack（租户+异常类型+业务键(月份) 唯一），
--       老板点「已处理」时写入当月确认标记；异常中心列表与经营驾驶舱计数
--       （含小程序「我的待处理」徽章）在存在当月确认标记时跳过推导；
--       确认标记仅当月有效，新月若仍未配置则重新推导（避免确认标记永久隐藏异常）。
-- 前置依赖：[38]M15_21（monthly_fixed_cost 表，异常推导口径来源）
-- 登记方式：★经 [80]M20_10（v1.1）末尾嵌套 source 挂入执行链（阶段三十二连带执行），
--           用户执行 00_执行顺序.sql 即自动执行本脚本，无需单独手动 source；
--           逻辑序号 [81]（v1.68 同批）。
-- 规范：
--   1. micro_exception_ack 为【租户级表】(含 tenant_id)，多租户插件自动隔离，不入 IGNORE_TENANT_TABLES
--   2. CREATE TABLE IF NOT EXISTS 幂等，可重复执行
--   3. UNIQUE(tenant_id, exception_type, exception_key) 防重复确认
--   4. 审计列 create_by/update_by 为 VARCHAR(64)，对齐 BaseEntity String 约定（M20_06 同款教训）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 业务异常已处理确认表 micro_exception_ack
-- ----------------------------
CREATE TABLE IF NOT EXISTS `micro_exception_ack` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `exception_type` VARCHAR(50) NOT NULL COMMENT '异常类型：FIXED_COST_ANOMALY(固定支出异常) 等',
  `exception_key` VARCHAR(50) NOT NULL COMMENT '异常业务键（固定支出异常=月份 yyyy-MM；确认当月有效，新月重新推导）',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_type_key` (`tenant_id`, `exception_type`, `exception_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务异常已处理确认表(租户级;异常中心「已处理」持久化;按tenant_id隔离)';

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1) 表已创建且含 tenant_id + 唯一约束
-- SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='micro_exception_ack' AND COLUMN_NAME='tenant_id';   -- 预期 1 行
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='micro_exception_ack' AND INDEX_NAME='uk_tenant_type_key';   -- 预期存在
--
-- 2) 页面点击「已处理」后可查到当月确认记录，且异常中心列表不再出现该异常
-- SELECT id, tenant_id, exception_type, exception_key, create_by, create_time
--   FROM micro_exception_ack WHERE exception_type='FIXED_COST_ANOMALY';
--   -- 预期 1 行（exception_key=当前月份 yyyy-MM）
