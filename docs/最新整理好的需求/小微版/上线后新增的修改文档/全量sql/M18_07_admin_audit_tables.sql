-- =====================================================================
-- M18_07 Admin 运维审计补齐建表脚本（T18.15，v1.29 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18
--       T18.15 Admin运维审计补齐（BE+PC）
-- 前置依赖：voice_compliance_hit 表（M18_01）
-- 规范：
--   1. sys_alert_rule 为【平台级表】（无 tenant_id），仅 Admin 管理
--   2. 先删后建幂等
--   3. menu_id 占用 7670~7679 区段
-- =====================================================================

SET NAMES utf8mb4;

-- ===== 1. 告警规则表 sys_alert_rule =====
DROP TABLE IF EXISTS `sys_alert_rule`;
CREATE TABLE `sys_alert_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(30) NOT NULL COMMENT '规则类型：LICENSE_EXPIRE(到期预警)/COMPLIANCE_HIT(合规命中)/TENANT_DISABLED(租户禁用)/SERVICE_DOWN(服务宕机)',
  `threshold_value` INT NOT NULL DEFAULT 1 COMMENT '触发阈值（如：到期天数/命中次数）',
  `severity` VARCHAR(10) NOT NULL DEFAULT 'WARNING' COMMENT '严重程度：INFO/WARNING/CRITICAL',
  `notify_channel` VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '通知渠道：SYSTEM(站内信)/EMAIL/SMS',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：1=启用 0=停用',
  `last_trigger_time` DATETIME DEFAULT NULL COMMENT '最近触发时间',
  `trigger_count` INT NOT NULL DEFAULT 0 COMMENT '累计触发次数',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表（T18.15，Admin管理，平台级无tenant_id）';

-- ===== 2. 预置 4 条告警规则 =====
INSERT IGNORE INTO `sys_alert_rule` (`rule_name`, `rule_type`, `threshold_value`, `severity`, `notify_channel`, `create_by`, `remark`)
VALUES
('License 到期预警', 'LICENSE_EXPIRE', 30, 'WARNING', 'SYSTEM', 'admin', 'License 30天内到期时触发告警'),
('合规命中告警', 'COMPLIANCE_HIT', 5, 'CRITICAL', 'SYSTEM', 'admin', '单租户近7天合规命中>=5次时触发'),
('租户禁用告警', 'TENANT_DISABLED', 1, 'WARNING', 'SYSTEM', 'admin', '租户被禁用时触发'),
('服务健康检查', 'SERVICE_DOWN', 1, 'CRITICAL', 'SYSTEM', 'admin', '核心服务不可用时触发');

-- ===== 3. 菜单 SQL（Admin 运维审计，menu_id 7670~7679）=====
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7670 AND 7679;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7670 AND 7679;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
VALUES
(7670, '语音处理日志', 100, 60, 'voiceAuditLog', 'system/voiceAuditLog/index', NULL, 1, 0, 'C', '0', '0', 'system:voiceAuditLog:list', 'server', 'log', 'admin', NOW(), '★T18.15 语音处理审计日志（Admin专属）'),
(7671, '告警规则配置', 100, 61, 'alertRule', 'system/alertRule/index', NULL, 1, 0, 'C', '0', '0', 'system:alertRule:list', 'server', 'alarm', 'admin', NOW(), '★T18.15 告警规则CRUD（Admin专属）');

-- 角色菜单关联（admin 角色）
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT r.role_id, m.menu_id
FROM `sys_role` r
CROSS JOIN `sys_menu` m
WHERE m.menu_id BETWEEN 7670 AND 7679
  AND r.role_key = 'admin';

-- ===== 验证 =====
-- SELECT * FROM sys_alert_rule;
-- SELECT * FROM sys_menu WHERE menu_id BETWEEN 7670 AND 7679;
