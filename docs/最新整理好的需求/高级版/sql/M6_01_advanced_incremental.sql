-- =====================================================================
-- M6_01 高级版增量开发脚本（T6.1~T6.7 配套）
-- 版本：v1.0  日期：2026-08-29
-- 依据：《资金管控系统设计文档》3.2 / 《资金管控系统开发任务文档》M6
-- 规范：
--   1. 先删后建 / 条件 ALTER / 先删后插，脚本可重复执行
--   2. 逐表含 tenant_id + 租户索引
--   3. 高级版增量表建齐但默认不开放（开放由功能开关控制）
-- =====================================================================

-- =====================================================================
-- 第一部分：高级版增量表（M1_03 未覆盖部分）
-- =====================================================================

-- ----------------------------
-- 1. 打卡触发规则配置表（FEAT_TIME_CLOCK，T6.2，租户级可配置）
--    tenant_id=0 为内置默认模板，租户无自定义配置时回退使用
-- ----------------------------
DROP TABLE IF EXISTS `fee_clock_trigger_rule`;
CREATE TABLE `fee_clock_trigger_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID(0=内置默认模板)',
  `rule_code` VARCHAR(50) NOT NULL COMMENT '规则编码(TRAVEL_CATEGORY差旅招待大类/AMOUNT_THRESHOLD金额阈值/HISTORY_ABNORMAL历史异常)',
  `rule_name` VARCHAR(100) COMMENT '规则名称',
  `threshold` DECIMAL(14,2) DEFAULT NULL COMMENT '阈值(金额阈值/历史异常次数)',
  `category_scope` VARCHAR(200) DEFAULT NULL COMMENT '大类范围(逗号分隔大类编码，差旅招待类)',
  `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
  `create_by` VARCHAR(64) DEFAULT NULL COMMENT '创建者',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` VARCHAR(64) DEFAULT NULL COMMENT '更新者',
  `update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡触发规则配置表';

-- 内置默认触发规则（先删后插幂等）：差旅招待 / ≥500 / 历史异常≥2
DELETE FROM `fee_clock_trigger_rule` WHERE `tenant_id` = 0;
INSERT INTO `fee_clock_trigger_rule` (`tenant_id`, `rule_code`, `rule_name`, `threshold`, `category_scope`, `is_enabled`) VALUES
(0, 'TRAVEL_CATEGORY',  '差旅/招待大类触发', NULL,  'TRAVEL,ENTERTAIN', 1),
(0, 'AMOUNT_THRESHOLD', '单笔金额阈值触发',  500,   NULL, 1),
(0, 'HISTORY_ABNORMAL', '历史异常次数触发',  2,     NULL, 1);

-- =====================================================================
-- 第二部分：存量表字段扩展（幂等条件 ALTER）
-- =====================================================================

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;
DELIMITER $$
CREATE PROCEDURE `sp_add_column_if_missing`(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(500))
BEGIN
    DECLARE v_cnt INT DEFAULT 0;
    SELECT COUNT(*) INTO v_cnt FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column;
    IF v_cnt = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 映射规则草稿→发布（FEAT_RULE_ENGINE_CUSTOM，T6.4；存量规则默认 PUBLISHED 不中断）
CALL sp_add_column_if_missing('fee_mapping_rule', 'publish_status',
  '`publish_status` VARCHAR(20) DEFAULT ''PUBLISHED'' COMMENT ''发布状态(DRAFT草稿/PUBLISHED已发布)''');
CALL sp_add_column_if_missing('fee_mapping_rule', 'publish_time',
  '`publish_time` DATETIME DEFAULT NULL COMMENT ''发布时间''');
CALL sp_add_column_if_missing('fee_mapping_rule', 'published_by',
  '`published_by` BIGINT DEFAULT NULL COMMENT ''发布人''');

-- 报销单匹配链路追溯（T6.4：匹配方式/关键词/是否人工修改）
CALL sp_add_column_if_missing('fee_expense_bill', 'match_trace',
  '`match_trace` TEXT DEFAULT NULL COMMENT ''映射规则匹配链路JSON(FEAT_RULE_ENGINE_CUSTOM)''');

-- 报销单信用等级快照（T6.3：提交时信用等级，差异化审批留痕）
CALL sp_add_column_if_missing('fee_expense_bill', 'credit_level',
  '`credit_level` VARCHAR(20) DEFAULT NULL COMMENT ''提交时信用等级快照(FEAT_CREDIT_SCORE)''');

-- 报销单部门预算扣减标记（T6.1：按提交人部门扣部门额度）
CALL sp_add_column_if_missing('fee_expense_bill', 'dept_budget_deducted',
  '`dept_budget_deducted` TINYINT DEFAULT 0 COMMENT ''是否已扣部门额度(0否 1是)''');

-- M1_03 高级版增量表补审计列（BaseEntity 自动填充需要，同 M5_01 模式）
CALL sp_add_column_if_missing('fee_employee_credit', 'create_by',
  '`create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者''');
CALL sp_add_column_if_missing('fee_employee_credit', 'update_by',
  '`update_by` VARCHAR(64) DEFAULT NULL COMMENT ''更新者''');
CALL sp_add_column_if_missing('fee_credit_rule', 'create_by',
  '`create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者''');
CALL sp_add_column_if_missing('fee_credit_rule', 'update_by',
  '`update_by` VARCHAR(64) DEFAULT NULL COMMENT ''更新者''');
CALL sp_add_column_if_missing('fee_clock_record', 'create_by',
  '`create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者''');
CALL sp_add_column_if_missing('fee_clock_record', 'update_by',
  '`update_by` VARCHAR(64) DEFAULT NULL COMMENT ''更新者''');
CALL sp_add_column_if_missing('fee_clock_record', 'update_time',
  '`update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL sp_add_column_if_missing('fee_mapping_audit_log', 'create_by',
  '`create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者''');
CALL sp_add_column_if_missing('fee_mapping_audit_log', 'update_by',
  '`update_by` VARCHAR(64) DEFAULT NULL COMMENT ''更新者''');
CALL sp_add_column_if_missing('fee_mapping_audit_log', 'update_time',
  '`update_time` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL sp_add_column_if_missing('fee_elastic_quota_config', 'create_by',
  '`create_by` VARCHAR(64) DEFAULT NULL COMMENT ''创建者''');
CALL sp_add_column_if_missing('fee_elastic_quota_config', 'update_by',
  '`update_by` VARCHAR(64) DEFAULT NULL COMMENT ''更新者''');

DROP PROCEDURE IF EXISTS `sp_add_column_if_missing`;

-- =====================================================================
-- 第三部分：菜单补充（3004 报销单管理增强按钮，menu_id 顺延，perms 不变规则见任务文档）
-- =====================================================================

DELETE FROM `sys_menu` WHERE `menu_id` IN (3301, 3302);
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3301, '批量通过', 3004, 20, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:expense:batchApprove', '#', 'admin', NOW(), '', NULL, '高级版 T6.7 财务批量审核'),
(3302, '批量打回', 3004, 21, '', NULL, NULL, 1, 0, 'F', '0', '0', 'fee:expense:batchReject',  '#', 'admin', NOW(), '', NULL, '高级版 T6.7 财务批量审核');

-- 三版本授权模板补充：高级版+ 含批量审核按钮（防串第④层，低版本模板不含）
DELETE FROM `sys_edition_menu_template` WHERE `menu_id` IN (3301, 3302);
INSERT INTO `sys_edition_menu_template` (`edition`, `menu_id`) VALUES
('ADVANCED', 3301), ('ADVANCED', 3302),
('PROFESSIONAL', 3301), ('PROFESSIONAL', 3302);

-- =====================================================================
-- 验收查询
-- =====================================================================
-- SHOW CREATE TABLE fee_clock_trigger_rule;
-- SELECT COUNT(*) FROM fee_clock_trigger_rule WHERE tenant_id = 0;      -- 预期 3
-- SELECT menu_id, perms FROM sys_menu WHERE menu_id IN (3301, 3302);    -- 预期 2 行
-- SELECT COUNT(*) FROM sys_edition_menu_template
--  WHERE edition = 'BASIC' AND menu_id IN (3301, 3302);                 -- 预期 0
