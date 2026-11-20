-- =====================================================================
-- M10_03 文案映射 i18n 建表脚本（T10.5，v1.29 新增）
-- 版本：v1.0  日期：2026-09-16
-- 依据：《资金管控系统开发任务文档（系统管理）》v1.29 T10.5
--       《资金管控系统设计文档》v1.3 §1.8.1（三层架构·业态包层·文案映射）
-- 规范：
--   1. 先删后建，脚本可重复执行（幂等）
--   2. biz_copy 为【系统级共享表】，不含 tenant_id，
--      须纳入 MybatisPlusConfig.IGNORE_TENANT_TABLES（与 biz_bundle 同策略）
--   3. 与 biz_bundle.copy（JSON 单语言）互补：biz_bundle.copy 作为默认包（zh-CN 兜底），
--      biz_copy 提供多语言精细化覆盖（zh-CN/en-US/zh-TW 等）
--   4. 三维合并优先级：租户 module_overrides.copy > biz_copy(biz_code,lang) > biz_bundle.copy > 前端硬编码兜底
-- =====================================================================

-- ----------------------------
-- 1. 业态文案映射表 biz_copy（系统级共享，一行一 key×lang；新增文案仅需插数据，零 DDL）
-- ----------------------------
DROP TABLE IF EXISTS `biz_copy`;
CREATE TABLE `biz_copy` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `biz_code` VARCHAR(50) NOT NULL COMMENT '业态包编码（与 biz_bundle.biz_code 对齐；NULL/空=全业态通用）',
  `i18n_key` VARCHAR(100) NOT NULL COMMENT '文案 key（前端 t(key) 入参，如 project/expense/income/signup/team）',
  `lang` VARCHAR(20) NOT NULL DEFAULT 'zh-CN' COMMENT '语言编码：zh-CN/en-US/zh-TW/ja-JP 等（BCP-47）',
  `copy_value` VARCHAR(500) NOT NULL COMMENT '文案值（当前语言的翻译文本）',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '所属模块（可选，用于分组展示：project/expense/budget/micro 等）',
  `status` CHAR(1) DEFAULT '0' COMMENT '状态（0启用 1停用）',
  `create_by` VARCHAR(64) COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_key_lang` (`biz_code`, `i18n_key`, `lang`),
  KEY `idx_biz_lang` (`biz_code`, `lang`),
  KEY `idx_i18n_key` (`i18n_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业态文案映射表(系统级共享,i18n,IGNORE_TENANT_TABLES)';

-- =====================================================================
-- 2. 内置初始数据（三业态包 × zh-CN，与 biz_bundle.copy 对齐；先删后插幂等）
-- =====================================================================
DELETE FROM `biz_copy` WHERE `biz_code` IN ('fund_control', 'boardgame', 'fresh_food');

-- 2.1 资金管控业态包（zh-CN）
INSERT INTO `biz_copy` (`biz_code`, `i18n_key`, `lang`, `copy_value`, `module`, `status`, `remark`) VALUES
('fund_control', 'project',   'zh-CN', '项目',       'common', '0', '资金管控业态：项目'),
('fund_control', 'expense',   'zh-CN', '报销',       'common', '0', '资金管控业态：报销'),
('fund_control', 'income',    'zh-CN', '收入确认',   'common', '0', '资金管控业态：收入确认'),
('fund_control', 'dashboard', 'zh-CN', '老板看板',   'common', '0', '资金管控业态：老板看板'),
('fund_control', 'budget',    'zh-CN', '预算',       'budget', '0', '资金管控业态：预算'),
('fund_control', 'subject',   'zh-CN', '科目',       'budget', '0', '资金管控业态：科目');

-- 2.2 桌游轰趴业态包（zh-CN）
INSERT INTO `biz_copy` (`biz_code`, `i18n_key`, `lang`, `copy_value`, `module`, `status`, `remark`) VALUES
('boardgame', 'project', 'zh-CN', '桌局/活动', 'common', '0', '桌游业态：项目→桌局/活动'),
('boardgame', 'expense', 'zh-CN', '支出',      'common', '0', '桌游业态：报销→支出'),
('boardgame', 'income',  'zh-CN', '收款',      'common', '0', '桌游业态：收入→收款'),
('boardgame', 'signup',  'zh-CN', '报名',      'common', '0', '桌游业态：signup→报名'),
('boardgame', 'team',    'zh-CN', '拼桌',      'common', '0', '桌游业态：team→拼桌');

-- 2.3 生鲜团购业态包（zh-CN）
INSERT INTO `biz_copy` (`biz_code`, `i18n_key`, `lang`, `copy_value`, `module`, `status`, `remark`) VALUES
('fresh_food', 'project', 'zh-CN', '团购/批次',   'common', '0', '生鲜业态：项目→团购/批次'),
('fresh_food', 'expense', 'zh-CN', '进货支出',    'common', '0', '生鲜业态：报销→进货支出'),
('fresh_food', 'income',  'zh-CN', '销售收入',    'common', '0', '生鲜业态：收入→销售收入'),
('fresh_food', 'signup',  'zh-CN', '下单',        'common', '0', '生鲜业态：signup→下单'),
('fresh_food', 'team',    'zh-CN', '团购',        'common', '0', '生鲜业态：team→团购');

-- 2.4 英文示例（en-US，仅 fund_control 三条示范，其余按需补）
INSERT INTO `biz_copy` (`biz_code`, `i18n_key`, `lang`, `copy_value`, `module`, `status`, `remark`) VALUES
('fund_control', 'project', 'en-US', 'Project',      'common', '0', '资金管控业态英文：Project'),
('fund_control', 'expense', 'en-US', 'Reimbursement','common', '0', '资金管控业态英文：Reimbursement'),
('fund_control', 'income',  'en-US', 'Income',       'common', '0', '资金管控业态英文：Income');

-- =====================================================================
-- 3. 菜单脚本（挂「资金平台管理菜单」1300 下，menu_id 1350~1352）
--    ★sys_menu 实际列名为 query_param（非 query），无 route_name 列；
--      client_type 由 M1_04 v1.1 幂等补列，本脚本直接使用；
--      与 M10_01 保持一致的列口径，避免 Unknown column 'query' 报错
-- =====================================================================
DELETE FROM `sys_role_menu` WHERE `menu_id` BETWEEN 1350 AND 1352;
DELETE FROM `sys_menu` WHERE `menu_id` BETWEEN 1350 AND 1352;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `client_type`, `icon`, `create_by`, `create_time`, `remark`) VALUES
(1350, '文案映射管理', 1300, 5, 'bizCopy', 'system/bizCopy/index', NULL, 1, 0, 'C', '0', '0', 'system:bizCopy:list',  'pc', 'language', 'admin', NOW(), 'T10.5 业态文案 i18n 管理'),
(1351, '文案查询',     1350, 1, '',        NULL,                   NULL, 1, 0, 'F', '0', '0', 'system:bizCopy:query', 'pc', '#',        'admin', NOW(), ''),
(1352, '文案编辑',     1350, 2, '',        NULL,                   NULL, 1, 0, 'F', '0', '0', 'system:bizCopy:edit',  'pc', '#',        'admin', NOW(), '');

-- ----------------------------
-- 3.1 授予超管 role_id=1（与 M10_01 同策略，否则平台管理员看不到新菜单）
-- ----------------------------
INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, menu_id FROM `sys_menu` WHERE menu_id BETWEEN 1350 AND 1352;

-- =====================================================================
-- 4. 验收查询
-- =====================================================================
-- 4.1 三业态包 zh-CN 文案齐备
-- SELECT biz_code, COUNT(*) AS cnt FROM biz_copy WHERE lang='zh-CN' GROUP BY biz_code;
-- 预期：fund_control>=6, boardgame>=5, fresh_food>=5

-- 4.2 唯一键生效
-- SELECT biz_code, i18n_key, lang, COUNT(*) FROM biz_copy GROUP BY biz_code, i18n_key, lang HAVING COUNT(*) > 1;
-- 预期：空集

-- 4.3 菜单挂载
-- SELECT menu_id, menu_name, perms FROM sys_menu WHERE menu_id BETWEEN 1350 AND 1352;
-- 预期：3 条

-- 4.4 已授权超管 role_id=1
-- SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1 AND menu_id BETWEEN 1350 AND 1352;
-- 预期：3
