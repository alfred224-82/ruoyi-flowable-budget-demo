-- ============================================================
-- 全面预算管理与费用管控系统 V5.4 - 基础版建表脚本
-- 版本：BASIC（小微企业轻量化）
-- 生成日期：2026-07-23
-- 说明：
--   基础版包含：预算编制、基础报销管控、科目调剂、操作日志、
--   AI报表、站内消息、License授权等核心功能表
--   不包含：资金池、超预算多级审批、合规自定义、计提引擎、
--   三轨管控、补助配置、外币、AI话术自定义、行业配置等
-- 表数量：23张
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、预算编制模块
-- ============================================================

-- -----------------------------------------------------------
-- 1. 预算编制主表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_preparation`;
CREATE TABLE `budget_preparation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_no` varchar(50) NOT NULL COMMENT '预算单号（唯一，格式：BG-YYYYMM-XXX）',
  `org_id` bigint NOT NULL COMMENT '组织ID（关联sys_dept表的dept_id）',
  `org_name` varchar(100) DEFAULT NULL COMMENT '组织名称（冗余字段，便于查询）',
  `budget_year` int NOT NULL COMMENT '预算年度',
  `budget_month` int NOT NULL COMMENT '预算月份（1-12）',
  `budget_period` varchar(7) GENERATED ALWAYS AS (concat(`budget_year`,_utf8mb4'-',lpad(`budget_month`,2,_utf8mb4'0'))) STORED COMMENT '预算期间（格式：YYYY-MM，自动生成）',
  `status` varchar(20) NOT NULL DEFAULT 'Draft' COMMENT '生命周期状态（Draft-草稿，Pending_Review-待审核，Pending_Revision-待修订，Approved-已批准，Rejected-已驳回）',
  `reject_level` varchar(10) DEFAULT 'None' COMMENT '驳回来源（HQ-总部，Branch-分公司，None-无）',
  `reject_reason` text COMMENT '驳回理由',
  `deadline_time` datetime DEFAULT NULL COMMENT '截止时间',
  `current_handler` varchar(50) DEFAULT NULL COMMENT '当前处理人（用户账号）',
  `total_budget` decimal(18,2) DEFAULT '0.00' COMMENT '预算总额（自动汇总明细表）',
  `total_actual` decimal(18,2) DEFAULT '0.00' COMMENT '实际总额',
  `variance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '差异率（百分比）',
  `process_instance_id` varchar(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `approval_stage` varchar(10) DEFAULT '' COMMENT '审批阶段（None-无，Dept-部门领导，Branch-分公司领导，HQ-总公司领导）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sheet_no` (`sheet_no`),
  KEY `idx_org_period` (`org_id`,`budget_year`,`budget_month`),
  KEY `idx_status` (`status`),
  KEY `idx_budget_period` (`budget_period`),
  KEY `idx_process_instance_id` (`process_instance_id`),
  KEY `idx_approval_stage` (`approval_stage`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算编制主表';

-- -----------------------------------------------------------
-- 2. 预算编制明细表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_preparation_detail`;
CREATE TABLE `budget_preparation_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '预算单ID（外键，关联budget_preparation.id）',
  `subject_code` varchar(20) NOT NULL COMMENT '科目编码（关联budget_subject.subject_code）',
  `subject_name` varchar(100) DEFAULT NULL COMMENT '科目名称（冗余字段）',
  `subject_type` varchar(20) DEFAULT NULL COMMENT '科目类型（INCOME-收入类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类，COST-成本类）',
  `budget_amount` decimal(18,2) DEFAULT '0.00' COMMENT '预算金额',
  `actual_amount` decimal(18,2) DEFAULT '0.00' COMMENT '实际金额',
  `variance_amount` decimal(18,2) DEFAULT '0.00' COMMENT '差异金额（自动计算：预算-实际）',
  `variance_rate` decimal(5,2) DEFAULT '0.00' COMMENT '差异率（百分比）',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID（关联sys_dept.dept_id）',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称（冗余字段）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_subject_code` (`subject_code`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算编制明细表';

-- -----------------------------------------------------------
-- 3. 预算科目表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_subject`;
CREATE TABLE `budget_subject` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `subject_code` varchar(20) NOT NULL COMMENT '科目编码（唯一，格式：一级1001，二级1001001）',
  `subject_name` varchar(100) NOT NULL COMMENT '科目名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父级科目ID（顶级为0）',
  `parent_code` varchar(20) DEFAULT '' COMMENT '父级科目编码（冗余字段，便于查询）',
  `level` int NOT NULL DEFAULT '1' COMMENT '科目层级（1-一级科目，2-二级科目，3-三级科目）',
  `ancestors` varchar(500) DEFAULT '' COMMENT '祖级列表（如：0/1001/1001001）',
  `is_leaf` tinyint(1) DEFAULT '0' COMMENT '是否叶子节点（0-否有子科目，1-是无子科目）',
  `sort_order` int DEFAULT '0' COMMENT '排序号（同级科目内的显示顺序）',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `description` varchar(500) DEFAULT NULL COMMENT '科目说明',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `subject_type` varchar(20) DEFAULT NULL COMMENT '科目类型（INCOME-收入类，COST-成本类，EXPENSE-费用类，ASSET-资产类，LIABILITY-负债类，EQUITY-权益类）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_code` (`subject_code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_ancestors` (`ancestors`(100)),
  KEY `idx_subject_type` (`subject_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算科目表';

-- -----------------------------------------------------------
-- 4. 预算邮件发送日志表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_email_log`;
CREATE TABLE `budget_email_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '预算单ID（关联budget_preparation表）',
  `sheet_no` varchar(50) DEFAULT NULL COMMENT '预算单号（冗余字段）',
  `email_type` varchar(20) NOT NULL COMMENT '邮件类型（REJECT/SEND_BACK/TIMEOUT_REMIND）',
  `recipient_user` varchar(50) NOT NULL COMMENT '收件人（用户账号）',
  `recipient_email` varchar(100) NOT NULL COMMENT '收件人邮箱',
  `email_subject` varchar(200) NOT NULL COMMENT '邮件主题',
  `send_status` varchar(10) NOT NULL DEFAULT 'PENDING' COMMENT '发送状态（PENDING/SUCCESS/FAILED）',
  `error_message` text COMMENT '错误信息（发送失败时记录）',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `send_time` datetime DEFAULT NULL COMMENT '发送时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_recipient_user` (`recipient_user`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算邮件发送日志表';

-- -----------------------------------------------------------
-- 5. 预算操作日志表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_operation_log`;
CREATE TABLE `budget_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '预算单ID（关联budget_preparation表）',
  `sheet_no` varchar(50) DEFAULT NULL COMMENT '预算单号（冗余字段）',
  `operation_type` varchar(20) NOT NULL COMMENT '操作类型（CREATE/SUBMIT/APPROVE/REJECT/SEND_BACK/UPDATE/DELETE）',
  `operator_user` varchar(50) NOT NULL COMMENT '操作人（用户账号）',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名（冗余字段）',
  `operator_dept_id` bigint DEFAULT NULL COMMENT '操作人部门ID（关联sys_dept表）',
  `operator_ip` varchar(128) DEFAULT NULL COMMENT '操作IP地址',
  `old_status` varchar(20) DEFAULT NULL COMMENT '原状态',
  `new_status` varchar(20) DEFAULT NULL COMMENT '新状态',
  `operation_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_operator_user` (`operator_user`),
  KEY `idx_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算操作日志表';

-- -----------------------------------------------------------
-- 6. 驳回历史记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_reject_history`;
CREATE TABLE `budget_reject_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `sheet_id` bigint NOT NULL COMMENT '预算单ID（外键，关联budget_preparation.id）',
  `sheet_no` varchar(50) DEFAULT NULL COMMENT '预算单号（冗余字段）',
  `reject_from_level` varchar(10) NOT NULL COMMENT '驳回方层级（HQ/Branch）',
  `reject_from_user` varchar(50) NOT NULL COMMENT '驳回人工号（用户账号）',
  `reject_from_name` varchar(50) DEFAULT NULL COMMENT '驳回人姓名（冗余字段）',
  `reject_to_level` varchar(10) NOT NULL COMMENT '被驳回方层级（Branch/Dept）',
  `reject_to_user` varchar(50) DEFAULT NULL COMMENT '被驳回人工号（用户账号，可为空）',
  `reject_to_dept_id` bigint DEFAULT NULL COMMENT '被驳回部门ID',
  `reject_to_dept_name` varchar(100) DEFAULT NULL COMMENT '被驳回部门名称（冗余字段）',
  `reject_reason` text NOT NULL COMMENT '驳回理由',
  `deadline_time` datetime NOT NULL COMMENT '截止时间',
  `reject_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '驳回时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  `handle_duration_hours` decimal(8,2) DEFAULT NULL COMMENT '处理耗时（小时，自动计算）',
  `is_timeout` tinyint(1) DEFAULT '0' COMMENT '是否超时（0-否，1-是）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sheet_id` (`sheet_id`),
  KEY `idx_reject_time` (`reject_time`),
  KEY `idx_reject_from_user` (`reject_from_user`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='驳回历史记录表';

-- -----------------------------------------------------------
-- 7. 驳回理由库表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_reject_reason`;
CREATE TABLE `budget_reject_reason` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reason_code` varchar(50) NOT NULL COMMENT '理由编码（唯一）',
  `reason_content` varchar(500) NOT NULL COMMENT '理由内容',
  `reject_level` varchar(10) NOT NULL COMMENT '驳回层级（HQ/Branch）',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reason_code` (`reason_code`),
  KEY `idx_reject_level` (`reject_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='驳回理由库表';

-- -----------------------------------------------------------
-- 8. 预算风控规则表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_risk_rule`;
CREATE TABLE `budget_risk_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码（唯一）',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_category` varchar(20) NOT NULL COMMENT '规则类别（BUDGET_OVER/LOGIC_ERROR/SUBJECT_ERROR/MISSING_DATA/HISTORICAL_COMPARE）',
  `threshold_value` decimal(10,2) DEFAULT NULL COMMENT '阈值',
  `risk_level` varchar(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '风险等级（HIGH/MEDIUM/LOW）',
  `rule_description` varchar(500) DEFAULT NULL COMMENT '规则描述',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算风控规则表';

-- -----------------------------------------------------------
-- 9. 预算科目规则校验表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `budget_validation_rule`;
CREATE TABLE `budget_validation_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码（唯一）',
  `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
  `rule_type` varchar(30) NOT NULL COMMENT '规则类型（REQUIRED/MIN_AMOUNT/MAX_AMOUNT/RATIO_LIMIT/SUM_CHECK/FORMULA）',
  `severity_level` varchar(20) NOT NULL DEFAULT 'ERROR' COMMENT '严重级别（ERROR/WARNING/INFO）',
  `subject_type` varchar(20) DEFAULT NULL COMMENT '适用科目类型（NULL表示全部类型）',
  `subject_code` varchar(20) DEFAULT NULL COMMENT '适用科目编码（NULL表示该类型全部）',
  `threshold_value` decimal(18,2) DEFAULT NULL COMMENT '阈值',
  `threshold_value2` decimal(18,2) DEFAULT NULL COMMENT '阈值2（用于范围校验）',
  `rule_expression` varchar(500) DEFAULT NULL COMMENT '规则表达式/说明',
  `error_message` varchar(500) NOT NULL COMMENT '校验不通过时的提示信息',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用（0-禁用，1-启用）',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `description` varchar(500) DEFAULT NULL COMMENT '规则说明',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_subject_type` (`subject_type`),
  KEY `idx_severity` (`severity_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算科目规则校验表';

-- ============================================================
-- 二、费用管控基础模块
-- ============================================================

-- -----------------------------------------------------------
-- 10. 报销单表（含V5.4新增字段）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_expense_bill`;
CREATE TABLE `fee_expense_bill` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_no` varchar(50) NOT NULL COMMENT '报销单号',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `cost_center_code` varchar(50) DEFAULT NULL COMMENT '责任中心编码',
  `item_category` varchar(100) NOT NULL COMMENT '物资小类编码',
  `purpose_tag` varchar(50) NOT NULL COMMENT '用途标签编码',
  `project_code` varchar(100) DEFAULT NULL COMMENT '项目编号',
  `calc_budget_subject` varchar(100) DEFAULT NULL COMMENT '计算后预算科目编码',
  `calc_accounting_class` varchar(50) DEFAULT NULL COMMENT '计算后会计大类',
  `amount` decimal(18,2) NOT NULL COMMENT '报销金额',
  `currency_code` varchar(10) DEFAULT 'CNY' COMMENT '币种编码',
  `original_amount` decimal(18,2) DEFAULT NULL COMMENT '原币金额（外币时填写）',
  `exchange_rate` decimal(10,6) DEFAULT NULL COMMENT '使用汇率',
  `subsidy_amount` decimal(18,2) DEFAULT NULL COMMENT '补助金额',
  `travel_days` int DEFAULT NULL COMMENT '出差天数（用于补助核算）',
  `expense_date` date NOT NULL COMMENT '费用发生日期',
  `expense_period` varchar(7) DEFAULT NULL COMMENT '报销年月（格式：YYYY-MM，对应预算编制期间）',
  `is_fixed_asset` tinyint DEFAULT '0' COMMENT '是否固定资产(0否 1是)',
  `useful_life` int DEFAULT NULL COMMENT '使用年限(年)，仅固定资产时填写',
  `expenditure_type` varchar(20) DEFAULT 'OPERATING' COMMENT '支出类型(CAPITAL/OPERATING)',
  `is_over_budget` tinyint DEFAULT '0' COMMENT '是否超预算(0否 1是)',
  `over_budget_approval_id` bigint DEFAULT NULL COMMENT '关联超预算审批单ID',
  `status` varchar(30) DEFAULT 'DRAFT' COMMENT '状态(DRAFT/SUBMITTED/APPROVED/REJECTED/OVER_BUDGET_PENDING)',
  `allocation_status` varchar(20) DEFAULT 'NONE' COMMENT '分摊状态(NONE/PENDING/ALLOCATED)',
  `compliance_status` varchar(20) DEFAULT NULL COMMENT '合规校验状态（PASS/FAIL/PENDING_SEAL）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `applicant_id` bigint DEFAULT NULL COMMENT '申请人ID',
  `applicant_name` varchar(50) DEFAULT NULL COMMENT '申请人姓名',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `reject_reason` text COMMENT '驳回原因',
  `reject_time` datetime DEFAULT NULL COMMENT '驳回时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_cost_center` (`cost_center_code`),
  KEY `idx_status` (`status`),
  KEY `idx_expense_date` (`expense_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='报销单表';

-- -----------------------------------------------------------
-- 11. 映射规则表（含V5.4新增industry_code字段）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_mapping_rule`;
CREATE TABLE `fee_mapping_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_code` varchar(50) NOT NULL COMMENT '规则编码',
  `item_category` varchar(100) NOT NULL COMMENT '物资小类编码',
  `purpose_tag` varchar(50) NOT NULL COMMENT '用途标签编码',
  `cost_center_group` varchar(50) DEFAULT '*' COMMENT '适用责任中心编码(*=全部)',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码（*=通用规则）',
  `budget_subject` varchar(100) NOT NULL COMMENT '目标预算科目编码',
  `accounting_class` varchar(50) NOT NULL COMMENT '归属会计大类',
  `is_fixed_asset` tinyint DEFAULT '0' COMMENT '是否固定资产(0否 1是)',
  `priority` int DEFAULT '0' COMMENT '优先级(数值越大优先级越高)',
  `status` varchar(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rule_code` (`rule_code`),
  KEY `idx_item_purpose` (`item_category`,`purpose_tag`),
  KEY `idx_cost_center` (`cost_center_group`),
  KEY `idx_industry_code` (`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='映射规则表';

-- -----------------------------------------------------------
-- 12. 用途标签表（含V5.4新增industry_code字段）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_purpose_tag`;
CREATE TABLE `fee_purpose_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tag_code` varchar(50) NOT NULL COMMENT '标签编码',
  `tag_name` varchar(200) NOT NULL COMMENT '标签名称',
  `tag_icon` varchar(100) DEFAULT NULL COMMENT '标签图标',
  `default_accounting_class` varchar(50) DEFAULT 'EXPENSE' COMMENT '默认会计大类',
  `require_project` tinyint DEFAULT '0' COMMENT '是否要求项目编号(0否 1是)',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` varchar(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码（*=全行业可见）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_code` (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用途标签表';

-- -----------------------------------------------------------
-- 13. 预算科目调剂流水表（基础版：同部门小额科目调剂）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_budget_adjustment_log`;
CREATE TABLE `fee_budget_adjustment_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cost_center_group` varchar(50) NOT NULL COMMENT '责任中心编码',
  `from_subject` varchar(100) NOT NULL COMMENT '调出科目编码',
  `to_subject` varchar(100) NOT NULL COMMENT '调入科目编码',
  `adjust_amount` decimal(18,2) NOT NULL COMMENT '调剂金额',
  `related_approval_id` bigint DEFAULT NULL COMMENT '关联审批单ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `operate_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_approval_id` (`related_approval_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算科目调剂流水表';

-- -----------------------------------------------------------
-- 14. 责任中心表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_cost_center`;
CREATE TABLE `fee_cost_center` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `center_code` varchar(50) NOT NULL COMMENT '责任中心编码',
  `center_name` varchar(200) NOT NULL COMMENT '责任中心名称',
  `default_accounting_class` varchar(50) DEFAULT 'EXPENSE' COMMENT '默认会计大类',
  `is_pending` tinyint DEFAULT '0' COMMENT '是否待分摊中心(0否 1是)',
  `status` varchar(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_center_code` (`center_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='责任中心表';

-- -----------------------------------------------------------
-- 15. 责任中心-部门关联表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_cost_center_dept`;
CREATE TABLE `fee_cost_center_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `center_code` varchar(50) NOT NULL COMMENT '责任中心编码',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `dept_name` varchar(200) DEFAULT NULL COMMENT '部门名称(冗余)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_id` (`dept_id`),
  KEY `idx_center_code` (`center_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='责任中心-部门关联表';

-- -----------------------------------------------------------
-- 16. 费用管控操作日志表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_operation_log`;
CREATE TABLE `fee_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `module` varchar(30) NOT NULL COMMENT '所属模块：EXPENSE/OVER_BUDGET/BUDGET_ADJUST',
  `operation_type` varchar(30) NOT NULL COMMENT '操作类型',
  `bill_id` bigint DEFAULT NULL COMMENT '关联报销单ID',
  `bill_no` varchar(50) DEFAULT NULL COMMENT '报销单编号',
  `related_app_id` bigint DEFAULT NULL COMMENT '关联预算调整申请ID',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) NOT NULL COMMENT '操作人姓名',
  `operator_role` varchar(30) DEFAULT NULL COMMENT '操作人角色',
  `before_status` varchar(30) DEFAULT NULL COMMENT '操作前状态',
  `after_status` varchar(30) DEFAULT NULL COMMENT '操作后状态',
  `operation_detail` text COMMENT '操作详情（JSON格式）',
  `ip_address` varchar(50) DEFAULT NULL COMMENT '操作人IP地址',
  `operation_time` datetime NOT NULL COMMENT '操作时间',
  PRIMARY KEY (`log_id`),
  KEY `idx_bill_no` (`bill_no`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operation_time` (`operation_time`),
  KEY `idx_operator_id` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='费用管控操作日志表';

-- ============================================================
-- 三、AI与消息模块
-- ============================================================

-- -----------------------------------------------------------
-- 17. AI报表生成记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `ai_report_record`;
CREATE TABLE `ai_report_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_type` varchar(50) NOT NULL COMMENT '报表类型(MONTHLY_DETAIL/APPROVAL_SUMMARY/YEARLY_FORECAST/AUDIT_ARCHIVE)',
  `report_name` varchar(200) DEFAULT NULL COMMENT '报表名称',
  `sheet_ids` varchar(500) DEFAULT NULL COMMENT '关联预算单ID(逗号分隔)',
  `report_content` longtext COMMENT '报表内容(AI生成的Markdown文本)',
  `export_format` varchar(20) DEFAULT 'HTML' COMMENT '导出格式(PDF/EXCEL/WORD/HTML)',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件存储路径',
  `status` varchar(20) NOT NULL DEFAULT 'GENERATING' COMMENT '生成状态(GENERATING/COMPLETED/FAILED)',
  `error_message` text COMMENT '错误信息',
  `cost_time_ms` bigint DEFAULT NULL COMMENT '生成耗时(毫秒)',
  `generate_time` datetime DEFAULT NULL COMMENT '生成时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_report_type` (`report_type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI报表生成记录表';

-- -----------------------------------------------------------
-- 18. 系统消息通知表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `sys_message`;
CREATE TABLE `sys_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `title` varchar(200) NOT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `message_type` varchar(20) NOT NULL COMMENT '消息类型（APPROVAL/APPROVAL_RESULT/REJECT/SYSTEM）',
  `biz_type` varchar(50) DEFAULT NULL COMMENT '业务类型（如 budget_preparation/EXPENSE/BUDGET_ADJUST）',
  `biz_id` bigint DEFAULT NULL COMMENT '业务ID（用于跳转）',
  `is_read` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否已读（0未读 1已读）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_user_read` (`user_id`,`is_read`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统消息通知表';

-- ============================================================
-- 四、License 授权与版本管理
-- ============================================================

-- -----------------------------------------------------------
-- 19. License授权表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `sys_license`;
CREATE TABLE `sys_license` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `license_id` varchar(50) NOT NULL COMMENT '授权编号（LIC-YYYY-NNNNNN）',
  `license_type` varchar(20) NOT NULL COMMENT '授权类型（TRIAL/FORMAL）',
  `customer` varchar(200) NOT NULL COMMENT '客户名称',
  `edition` varchar(20) NOT NULL COMMENT '版本等级（BASIC/ADVANCED/ENTERPRISE）',
  `trial_days` int DEFAULT 0 COMMENT '试用天数（仅TRIAL有效）',
  `industries` varchar(500) DEFAULT NULL COMMENT '授权行业列表JSON（如["INTERNET","FINANCE"]）',
  `max_tenants` int DEFAULT 1 COMMENT '最大租户数',
  `features` text NOT NULL COMMENT '功能开关列表JSON',
  `machine_fingerprint` varchar(128) NOT NULL COMMENT '机器指纹哈希',
  `issued_at` date NOT NULL COMMENT '签发日期',
  `expires_at` date DEFAULT NULL COMMENT '到期日期（NULL=永久）',
  `license_version` int DEFAULT 1 COMMENT 'License版本号（防降级回滚）',
  `signature` text NOT NULL COMMENT 'RSA-SHA256数字签名',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/EXPIRED/REVOKED/TAMPERED）',
  `activated_at` datetime DEFAULT NULL COMMENT '首次激活时间（试用起算）',
  `last_check_time` datetime DEFAULT NULL COMMENT '最后校验时间（防时钟回拨）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_license_id` (`license_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='License授权表';

-- -----------------------------------------------------------
-- 20. 版本增量初始化数据标记表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_edition_init_data`;
CREATE TABLE `fee_edition_init_data` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `edition` varchar(20) NOT NULL COMMENT '版本（BASIC/ADVANCED/ENTERPRISE）',
  `init_key` varchar(100) NOT NULL COMMENT '初始化数据标识',
  `init_status` varchar(20) DEFAULT 'PENDING' COMMENT '执行状态（PENDING/DONE）',
  `executed_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edition_key` (`edition`, `init_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本增量初始化数据标记表';

-- -----------------------------------------------------------
-- 21. 工作流配置主表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_workflow_config`;
CREATE TABLE `fee_workflow_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_key` varchar(50) NOT NULL COMMENT '工作流标识（budget_preparation/expense_approval/budget_adjustment）',
  `workflow_name` varchar(100) NOT NULL COMMENT '工作流名称',
  `description` varchar(500) DEFAULT NULL COMMENT '流程说明',
  `node_count` int NOT NULL DEFAULT '0' COMMENT '审批节点数量',
  `deploy_id` varchar(64) DEFAULT NULL COMMENT '当前已部署的Flowable部署ID',
  `proc_def_id` varchar(64) DEFAULT NULL COMMENT '当前流程定义ID',
  `status` varchar(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态（DRAFT未部署/DEPLOYED已部署/DISABLED已停用）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_key` (`workflow_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流配置主表';

-- -----------------------------------------------------------
-- 22. 工作流审批节点配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_workflow_node_config`;
CREATE TABLE `fee_workflow_node_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `workflow_id` bigint NOT NULL COMMENT '所属工作流ID（关联fee_workflow_config.id）',
  `node_seq` int NOT NULL COMMENT '节点顺序（从1开始）',
  `node_name` varchar(100) NOT NULL COMMENT '节点名称（如：部门经理审批）',
  `role_id` bigint NOT NULL COMMENT '审批角色ID（关联sys_role.role_id）',
  `role_key` varchar(100) DEFAULT NULL COMMENT '角色标识（冗余，如：deptManager）',
  `role_name` varchar(100) DEFAULT NULL COMMENT '角色名称（冗余，如：部门经理）',
  `approval_type` varchar(20) NOT NULL DEFAULT 'ANY' COMMENT '审批方式（ANY单人审批/ALL会签）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_workflow_seq` (`workflow_id`,`node_seq`),
  KEY `idx_workflow_id` (`workflow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工作流审批节点配置表';

-- -----------------------------------------------------------
-- 23. 资金池初始额度配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_fund_pool_config`;
CREATE TABLE `fee_fund_pool_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `level_type` varchar(20) NOT NULL COMMENT '层级类型（DEPT/BRANCH/HQ）',
  `level_name` varchar(50) NOT NULL COMMENT '层级名称（部门级/分公司级/总公司级）',
  `role_id` bigint DEFAULT NULL COMMENT '关联角色ID（sys_role.role_id）',
  `role_key` varchar(100) DEFAULT NULL COMMENT '角色标识（冗余）',
  `role_name` varchar(100) DEFAULT NULL COMMENT '角色名称（冗余）',
  `initial_amount` decimal(18,2) NOT NULL COMMENT '初始额度',
  `warning_threshold` decimal(5,2) NOT NULL DEFAULT '80.00' COMMENT '预警阈值百分比',
  `status` varchar(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态（ENABLED/DISABLED）',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_level_type` (`level_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资金池初始额度配置表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 基础版表清单（共23张）：
-- ============================================================
--  1. budget_preparation        预算编制主表
--  2. budget_preparation_detail 预算编制明细表
--  3. budget_subject            预算科目表
--  4. budget_email_log          预算邮件发送日志表
--  5. budget_operation_log      预算操作日志表
--  6. budget_reject_history     驳回历史记录表
--  7. budget_reject_reason      驳回理由库表
--  8. budget_risk_rule          预算风控规则表
--  9. budget_validation_rule    预算科目规则校验表
-- 10. fee_expense_bill          报销单表
-- 11. fee_mapping_rule          映射规则表
-- 12. fee_purpose_tag           用途标签表
-- 13. fee_budget_adjustment_log 预算科目调剂流水表
-- 14. fee_cost_center           责任中心表
-- 15. fee_cost_center_dept      责任中心-部门关联表
-- 16. fee_operation_log         费用管控操作日志表
-- 17. ai_report_record          AI报表生成记录表
-- 18. sys_message               系统消息通知表
-- 19. sys_license               License授权表
-- 20. fee_edition_init_data     版本增量初始化数据标记表
-- 21. fee_workflow_config        工作流配置主表
-- 22. fee_workflow_node_config   工作流审批节点配置表
-- 23. fee_fund_pool_config       资金池初始额度配置表
-- ============================================================
