-- =====================================================================
-- M18_02 标签与周期规则建表脚本（T18.9，v1.29 新增）
-- 版本：v1.0  日期：2026-09-17
-- 依据：《资金管控系统开发任务文档（小微版）》v1.29 M18
--       T18.9 标签与周期规则管理
-- 前置依赖：M15_06（micro 菜单）、M15_21（monthly_fixed_cost 表）
-- 规范：
--   1. fixed_cost_tag / period_rule 均为【租户级表】(含 tenant_id)
--   2. 先删后建幂等
--   3. menu_id 7620~7629（避让 7600~7609 合规管理）
-- =====================================================================

SET NAMES utf8mb4;

-- ----------------------------
-- 1. 固定支出标签表 fixed_cost_tag
--    租户级标签管理：房租/水电/工资等，CRUD + 排序
-- ----------------------------
DROP TABLE IF EXISTS `fixed_cost_tag`;
CREATE TABLE `fixed_cost_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称（如：房租/水电/工资/物料/营销等）',
  `tag_code` VARCHAR(50) DEFAULT NULL COMMENT '标签编码（可选，用于程序引用）',
  `color` VARCHAR(20) DEFAULT NULL COMMENT '标签颜色（前端展示用，如 #ff6b6b）',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号（升序）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED(启用)/DISABLED(停用)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_tag_name` (`tenant_id`, `tag_name`),
  KEY `idx_tenant_sort` (`tenant_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定支出标签表(租户级;房租/水电/工资等;CRUD+排序;按tenant_id隔离)';

-- ----------------------------
-- 2. 周期规则表 period_rule
--    租户级周期规则：数字 + 时间单位 → 保存（如"1 个月""15 天"）
--    时间单位由 Admin 全局字典定义，本表仅存具体数值
-- ----------------------------
DROP TABLE IF EXISTS `period_rule`;
CREATE TABLE `period_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `rule_name` VARCHAR(100) NOT NULL COMMENT '规则名称（如：月付房租/季度水电/年薪）',
  `period_value` DECIMAL(10,2) NOT NULL COMMENT '周期数值（如 1/3/12/15）',
  `period_unit` VARCHAR(20) NOT NULL COMMENT '时间单位：DAY(天)/WEEK(周)/MONTH(月)/QUARTER(季)/YEAR(年)',
  `total_days` INT DEFAULT NULL COMMENT '换算天数（= period_value × 单位换算天数，冗余存储便于计算）',
  `tag_id` BIGINT DEFAULT NULL COMMENT '关联标签ID（fixed_cost_tag.id，可选）',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态：ENABLED(启用)/DISABLED(停用)',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_rule_name` (`tenant_id`, `rule_name`),
  KEY `idx_tenant_status` (`tenant_id`, `status`),
  KEY `idx_tenant_tag` (`tenant_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期规则表(租户级;数字+时间单位;如1个月/15天;按tenant_id隔离)';

-- ----------------------------
-- 3. 菜单 SQL（租户管理员 PC，menu_id 7620~7629）
--    ★先删后插幂等
-- ----------------------------
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 7620 AND 7629;
DELETE FROM `sys_menu`      WHERE `menu_id` BETWEEN 7620 AND 7629;

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(7620, '固定支出配置', 7500, 20, 'costConfig', NULL, NULL, 1, 0, 'M', '0', '0', '', 'pc', 'money', 'admin', NOW(), '★租户管理员：固定支出标签+周期规则管理'),
(7621, '标签管理', 7620, 1, 'tagConfig', 'micro/tagConfig/index', NULL, 1, 0, 'C', '0', '0', 'micro:tagConfig:list', 'pc', 'tag', 'admin', NOW(), '固定支出标签 CRUD + 排序'),
(7622, '标签新增', 7621, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:tagConfig:add', 'pc', '#', 'admin', NOW(), NULL),
(7623, '标签修改', 7621, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:tagConfig:edit', 'pc', '#', 'admin', NOW(), NULL),
(7624, '标签删除', 7621, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:tagConfig:remove', 'pc', '#', 'admin', NOW(), NULL),
(7625, '周期规则管理', 7620, 2, 'periodRule', 'micro/periodRule/index', NULL, 1, 0, 'C', '0', '0', 'micro:periodRule:list', 'pc', 'time-range', 'admin', NOW(), '周期规则 CRUD（数字+时间单位）'),
(7626, '规则新增', 7625, 1, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:periodRule:add', 'pc', '#', 'admin', NOW(), NULL),
(7627, '规则修改', 7625, 2, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:periodRule:edit', 'pc', '#', 'admin', NOW(), NULL),
(7628, '规则删除', 7625, 3, '', NULL, NULL, 1, 0, 'F', '0', '0', 'micro:periodRule:remove', 'pc', '#', 'admin', NOW(), NULL);

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 7620 AND 7629;

-- =====================================================================
-- 验收查询
-- =====================================================================
-- 1) 两张表均含 tenant_id + 唯一约束
-- SELECT TABLE_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE()
--   AND COLUMN_NAME='tenant_id' AND TABLE_NAME IN ('fixed_cost_tag','period_rule');  -- 预期 2 行
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='fixed_cost_tag' AND INDEX_NAME='uk_tenant_tag_name';  -- 预期存在
-- SELECT INDEX_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE()
--   AND TABLE_NAME='period_rule' AND INDEX_NAME='uk_tenant_rule_name';  -- 预期存在
--
-- 2) 菜单落库（7620~7628）
-- SELECT menu_id, menu_name, parent_id, perms FROM sys_menu WHERE menu_id BETWEEN 7620 AND 7629 ORDER BY menu_id;
--   -- 预期 9 行
