-- =====================================================================
-- M15_04 小微版服务与定价表建表脚本（T15.4，v1.13 新增）
-- 版本：v1.0  日期：2026-09-07
-- 依据：《小微版完整方案》v10.0 第七章（服务与定价体系）/ 第十六章
--       《资金管控系统开发任务文档》v1.13 T15.4 / T15.11
--       《多业态SaaS平台需求规格说明书》v1.0 第 17 章（服务与定价体系）
-- 前置依赖：M15_02（industry_template，服务分类示例来源）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）
--   2. 三表均含 tenant_id + idx_tenant_id（租户级表，租户间数据隔离）
--   3. 通用模型：服务分类 → 服务项目(价格/单位/计价方式)，支持所有行业，不区分 appid/渠道
--   4. pricing_type 计价方式枚举：duration(按时长)/weight(按重量)/times(按次)/person(按人)/portion(按份)
--   5. 改价自动写 price_change_log（老板小程序「改价确认」/ 平台管理员编辑服务项目）
--   6. 服务项目数据由租户运行时创建，本脚本不预置业务数据（行业示例见 industry_template.preset_config）
-- =====================================================================

-- ----------------------------
-- 1. 服务分类表 service_category（房间/助教/蔬菜/肉菜...）
-- ----------------------------
DROP TABLE IF EXISTS `service_category`;
CREATE TABLE `service_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `name` VARCHAR(100) NOT NULL COMMENT '分类名称(房间/助教/蔬菜/肉菜/课程...)',
  `industry_type` VARCHAR(50) DEFAULT NULL COMMENT '所属行业(industry_template.industry_code)',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_tenant_industry` (`tenant_id`, `industry_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务分类表(小微版)';

-- ----------------------------
-- 2. 服务项目表 service_item（名称+价格+单位+计价方式）
-- ----------------------------
DROP TABLE IF EXISTS `service_item`;
CREATE TABLE `service_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `category_id` BIGINT NOT NULL COMMENT '服务分类ID(FK→service_category)',
  `name` VARCHAR(100) NOT NULL COMMENT '服务项目名称(小房间/初级助教/有机蔬菜...)',
  `price` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位(小时/斤/节/人/次/份)',
  `pricing_type` VARCHAR(20) NOT NULL DEFAULT 'times' COMMENT '计价方式：duration(按时长)/weight(按重量)/times(按次)/person(按人)/portion(按份)',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '项目描述',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0上架 1下架）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag` CHAR(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_category` (`category_id`),
  KEY `idx_tenant_category` (`tenant_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务项目表(小微版;价格/单位/计价方式)';

-- ----------------------------
-- 3. 价格调整记录表 price_change_log（改价历史留痕）
-- ----------------------------
DROP TABLE IF EXISTS `price_change_log`;
CREATE TABLE `price_change_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',
  `item_id` BIGINT NOT NULL COMMENT '服务项目ID(FK→service_item)',
  `old_price` DECIMAL(12,2) DEFAULT NULL COMMENT '调整前单价',
  `new_price` DECIMAL(12,2) NOT NULL COMMENT '调整后单价',
  `operator` VARCHAR(64) DEFAULT NULL COMMENT '操作人(老板/平台管理员)',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人用户ID',
  `change_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '改价时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_item` (`item_id`),
  KEY `idx_tenant_item_time` (`tenant_id`, `item_id`, `change_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='价格调整记录表(改价历史留痕)';

-- ----------------------------
-- 4. 验收查询（执行后人工核对）
-- ----------------------------
-- 4.1 三表均含 tenant_id + 索引（租户间数据隔离）
-- SELECT TABLE_NAME FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND COLUMN_NAME = 'tenant_id'
--    AND TABLE_NAME IN ('service_category','service_item','price_change_log');  -- 预期 3 行
--
-- 4.2 pricing_type 枚举完整（duration/weight/times/person/portion）
-- SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
--  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'service_item' AND COLUMN_NAME = 'pricing_type';
--
-- 4.3 改价历史可查（运行时）
-- SELECT item_id, old_price, new_price, operator, change_time FROM price_change_log
--  WHERE tenant_id = <租户ID> ORDER BY change_time DESC;
--
-- 4.4 桌游店示例（运行时由租户创建，参考《小微版完整方案》7.1）：
--     房间：小房间(2-4人) 50元/小时(duration)、大房间(6-10人) 80元/小时(duration)
--     助教：初级助教 30元/小时(duration)、高级助教 50元/小时(duration)
