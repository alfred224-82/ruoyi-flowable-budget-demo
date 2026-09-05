-- ============================================================
-- 全面预算管理与费用管控系统 V5.4 - 企业专业版建表脚本
-- 版本：ENTERPRISE（集团型企业全功能管控）
-- 生成日期：2026-07-23
-- 说明：
--   企业专业版 = 高级版全部功能 + 以下新增能力：
--   长短期借款台账+利息自动计提、集团跨部门自动分摊、
--   分公司/总公司两级上层资金池、私有化AI部署支持
-- 表数量：40张（高级版38张 + 企业版新增2张）
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 一、预算编制模块（同基础版）
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
-- 二、费用管控基础模块（同基础版）
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
-- 11. 映射规则表
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
-- 12. 用途标签表
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
-- 13. 预算科目调剂流水表
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
-- 三、AI与消息模块（同基础版）
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
-- 四、License 授权与版本管理（同基础版）
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

-- ============================================================
-- 五、高级版新增 — 超预算审批与资金池
-- ============================================================
-- -----------------------------------------------------------
-- 21. 超预算审批表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_over_budget_approval`;
CREATE TABLE `fee_over_budget_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_id` bigint NOT NULL COMMENT '关联报销单ID',
  `cost_center_group` varchar(50) NOT NULL COMMENT '预算责任中心编码',
  `budget_subject` varchar(100) NOT NULL COMMENT '超预算的预算科目编码',
  `original_budget` decimal(18,2) NOT NULL COMMENT '该科目原预算金额',
  `used_amount` decimal(18,2) NOT NULL COMMENT '该科目已使用金额',
  `request_amount` decimal(18,2) NOT NULL COMMENT '本次申请金额',
  `shortage_amount` decimal(18,2) NOT NULL COMMENT '缺口金额',
  `approval_action` varchar(20) DEFAULT NULL COMMENT '审批动作(APPROVE/REJECT/ADJUST)',
  `adjust_from_subject` varchar(100) DEFAULT NULL COMMENT '调出科目编码',
  `adjust_amount` decimal(18,2) DEFAULT NULL COMMENT '调剂金额',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approval_opinion` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/PROCESSED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='超预算审批表';

-- -----------------------------------------------------------
-- 22. 预算调整申请表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_budget_adjustment_app`;
CREATE TABLE `fee_budget_adjustment_app` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_no` varchar(50) NOT NULL COMMENT '申请单号（BA-YYYYMMDD-XXX）',
  `bill_id` bigint DEFAULT NULL COMMENT '关联报销单ID',
  `bill_no` varchar(50) DEFAULT NULL COMMENT '关联报销单号',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID',
  `applicant_name` varchar(50) DEFAULT NULL COMMENT '申请人姓名',
  `dept_id` bigint NOT NULL COMMENT '申请人部门ID',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '申请人部门名称',
  `item_category` varchar(100) DEFAULT NULL COMMENT '物资小类编码',
  `purpose_tag` varchar(50) DEFAULT NULL COMMENT '用途标签编码',
  `target_subject` varchar(50) NOT NULL COMMENT '目标预算科目编码',
  `target_subject_name` varchar(100) DEFAULT NULL COMMENT '目标预算科目名称',
  `cost_center_code` varchar(50) DEFAULT NULL COMMENT '责任中心编码',
  `current_balance` decimal(15,2) NOT NULL COMMENT '申请时科目可用余额',
  `request_amount` decimal(15,2) NOT NULL COMMENT '申请调整金额',
  `budget_amount` decimal(15,2) DEFAULT NULL COMMENT '预算金额',
  `reason` text NOT NULL COMMENT '调整原因说明',
  `current_level` int NOT NULL DEFAULT '1' COMMENT '当前审批层级：1部门/2分公司/3总公司',
  `dept_reserved_id` bigint DEFAULT NULL COMMENT '部门预留预算ID',
  `branch_reserved_id` bigint DEFAULT NULL COMMENT '分公司预留预算ID',
  `hq_reserved_id` bigint DEFAULT NULL COMMENT '总公司预留预算ID',
  `allocate_amount` decimal(15,2) DEFAULT NULL COMMENT '实际分摊金额',
  `allocate_from_reserved_id` bigint DEFAULT NULL COMMENT '实际分摊来源的预留预算ID',
  `reject_reason` text COMMENT '驳回原因',
  `status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '状态',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `complete_time` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` varchar(64) DEFAULT '',
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_no` (`app_no`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_status` (`status`),
  KEY `idx_applicant` (`applicant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预算调整申请表';

-- -----------------------------------------------------------
-- 23. 预留预算表（资金池）
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_reserved_budget`;
CREATE TABLE `fee_reserved_budget` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `budget_period` varchar(20) NOT NULL COMMENT '预算期间',
  `level_type` varchar(20) NOT NULL COMMENT '层级类型：DEPT/BRANCH/HQ',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID（HQ级别为空）',
  `dept_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `total_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '预留预算总额度',
  `allocated_amount` decimal(15,2) NOT NULL DEFAULT '0.00' COMMENT '已分摊金额',
  `available_amount` decimal(15,2) GENERATED ALWAYS AS ((`total_amount` - `allocated_amount`)) STORED COMMENT '剩余可用额度（生成列）',
  `warning_threshold` decimal(5,2) DEFAULT '80.00' COMMENT '预警阈值百分比',
  `creator_id` bigint DEFAULT NULL COMMENT '编制人ID',
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/FROZEN/EXPIRED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` varchar(64) DEFAULT '',
  `update_by` varchar(64) DEFAULT '',
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_period_level` (`budget_period`,`level_type`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预留预算表';

-- -----------------------------------------------------------
-- 24. 预留预算分摊记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_reserved_allocation_log`;
CREATE TABLE `fee_reserved_allocation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `reserved_id` bigint NOT NULL COMMENT '关联预留预算ID',
  `application_id` bigint NOT NULL COMMENT '关联预算调整申请ID',
  `app_no` varchar(50) DEFAULT NULL COMMENT '关联申请单号',
  `target_subject` varchar(50) NOT NULL COMMENT '目标预算科目编码',
  `target_subject_name` varchar(100) DEFAULT NULL COMMENT '目标预算科目名称',
  `allocate_amount` decimal(15,2) NOT NULL COMMENT '分摊金额',
  `before_available` decimal(15,2) NOT NULL COMMENT '分摊前科目可用余额',
  `after_available` decimal(15,2) NOT NULL COMMENT '分摊后科目可用余额',
  `before_reserved_available` decimal(15,2) NOT NULL COMMENT '分摊前预留剩余额度',
  `after_reserved_available` decimal(15,2) NOT NULL COMMENT '分摊后预留剩余额度',
  `operator_id` bigint NOT NULL COMMENT '操作人ID（审批人）',
  `operator_name` varchar(50) DEFAULT NULL COMMENT '操作人姓名',
  `operate_time` datetime NOT NULL COMMENT '操作时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reserved_id` (`reserved_id`),
  KEY `idx_application_id` (`application_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预留预算分摊记录表';

-- -----------------------------------------------------------
-- 25. 费用分摊记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_cost_allocation`;
CREATE TABLE `fee_cost_allocation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `bill_id` bigint NOT NULL COMMENT '关联报销单ID',
  `allocation_batch` varchar(20) NOT NULL COMMENT '分摊批次(YYYY-MM)',
  `source_cost_center` varchar(50) NOT NULL COMMENT '源责任中心编码',
  `target_dept_id` bigint NOT NULL COMMENT '目标部门ID',
  `target_cost_center` varchar(50) NOT NULL COMMENT '目标责任中心编码',
  `alloc_amount` decimal(18,2) NOT NULL COMMENT '分摊金额',
  `alloc_ratio` decimal(10,4) NOT NULL COMMENT '分摊比例',
  `alloc_rule_id` bigint DEFAULT NULL COMMENT '分摊规则ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_batch` (`allocation_batch`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='费用分摊记录表';

-- -----------------------------------------------------------
-- 26. 费用分摊规则表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_cost_allocation_rule`;
CREATE TABLE `fee_cost_allocation_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `rule_name` varchar(200) NOT NULL COMMENT '规则名称',
  `allocation_method` varchar(30) NOT NULL COMMENT '分摊方法(HEAD_COUNT/WORK_HOUR/RATIO)',
  `target_dept_id` bigint NOT NULL COMMENT '目标部门ID',
  `target_cost_center` varchar(50) NOT NULL COMMENT '目标责任中心编码',
  `weight` decimal(10,4) NOT NULL COMMENT '权重',
  `status` varchar(20) DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='费用分摊规则表';

-- ============================================================
-- 六、高级版新增 — 合规引擎/行业配置/三轨管控
-- ============================================================
-- -----------------------------------------------------------
-- 27. 发票合规级别配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_compliance_config`;
CREATE TABLE `fee_compliance_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `budget_subject_code` varchar(100) NOT NULL COMMENT '预算科目编码',
  `budget_subject_name` varchar(100) DEFAULT NULL COMMENT '预算科目名称',
  `compliance_level` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '合规级别（SIMPLE/NORMAL/STRICT）',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码（*=通用）',
  `require_seal` tinyint DEFAULT 0 COMMENT '严格模式是否需财务盖章确认（0否1是）',
  `status` varchar(20) DEFAULT 'ENABLED' COMMENT '状态',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_industry` (`budget_subject_code`, `industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票合规级别配置表（按科目）';

-- -----------------------------------------------------------
-- 28. 发票合规校验记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_compliance_record`;
CREATE TABLE `fee_compliance_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bill_id` bigint NOT NULL COMMENT '报销单ID',
  `bill_no` varchar(50) NOT NULL COMMENT '报销单号',
  `invoice_type` varchar(30) DEFAULT NULL COMMENT '识别票据类型（FORMAL_INVOICE/RECEIPT/OTHER）',
  `required_level` varchar(20) NOT NULL COMMENT '要求合规级别',
  `actual_level` varchar(20) NOT NULL COMMENT '实际满足级别',
  `check_result` varchar(20) NOT NULL COMMENT '校验结果（PASS/FAIL/PENDING_SEAL）',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '不合规原因',
  `seal_confirmed` tinyint DEFAULT 0 COMMENT '财务盖章确认（0未确认1已确认）',
  `seal_confirmer_id` bigint DEFAULT NULL COMMENT '盖章确认人ID',
  `seal_confirm_time` datetime DEFAULT NULL COMMENT '盖章确认时间',
  `check_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '校验时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill_id` (`bill_id`),
  KEY `idx_bill_no` (`bill_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发票合规校验记录表';

-- -----------------------------------------------------------
-- 29. 行业配置主数据表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_industry_config`;
CREATE TABLE `fee_industry_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `industry_code` varchar(50) NOT NULL COMMENT '行业编码（INTERNET/REALESTATE/MANUFACTURING/FINANCE）',
  `industry_name` varchar(100) NOT NULL COMMENT '行业名称',
  `switches` text DEFAULT NULL COMMENT '行业开关组JSON',
  `depreciation_defaults` text DEFAULT NULL COMMENT '折旧默认参数JSON',
  `budget_thresholds` text DEFAULT NULL COMMENT '预算管控阈值JSON',
  `compliance_rules` text DEFAULT NULL COMMENT '行业合规拦截规则JSON',
  `ai_prompt_pack` text DEFAULT NULL COMMENT 'AI引导话术包JSON',
  `profit_tip_pack` text DEFAULT NULL COMMENT '盈利提示文案包JSON',
  `accrual_params` text DEFAULT NULL COMMENT '计提规则参数JSON',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_code` (`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行业配置主数据表';

-- -----------------------------------------------------------
-- 30. 三轨预算管控配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_budget_track_config`;
CREATE TABLE `fee_budget_track_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `budget_subject_code` varchar(100) NOT NULL COMMENT '预算科目编码',
  `track_type` varchar(20) NOT NULL COMMENT '轨道类型（EXPENSE/CAPITAL/CASHFLOW）',
  `control_mode` varchar(20) NOT NULL DEFAULT 'SOFT' COMMENT '管控模式（SOFT软预警/HARD硬阻断）',
  `warning_threshold` decimal(5,2) DEFAULT 80.00 COMMENT '预警阈值百分比',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_track` (`budget_subject_code`, `track_type`, `industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='三轨预算管控配置表';

-- ============================================================
-- 七、高级版新增 — 计提引擎/补助/盈利提示/AI增强
-- ============================================================
-- -----------------------------------------------------------
-- 31. 资产卡片表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_asset_card`;
CREATE TABLE `fee_asset_card` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `asset_code` varchar(50) NOT NULL COMMENT '资产编号',
  `asset_name` varchar(200) NOT NULL COMMENT '资产名称',
  `asset_type` varchar(30) NOT NULL COMMENT '资产类型（FIXED/INTANGIBLE）',
  `dept_id` bigint NOT NULL COMMENT '归属部门ID',
  `budget_subject_code` varchar(100) DEFAULT NULL COMMENT '关联预算科目',
  `original_value` decimal(18,2) NOT NULL COMMENT '原值',
  `residual_rate` decimal(5,2) DEFAULT 5.00 COMMENT '残值率(%)',
  `useful_life_months` int NOT NULL COMMENT '折旧/摊销月数',
  `start_date` date NOT NULL COMMENT '启用日期（次月起计提）',
  `monthly_amount` decimal(18,2) NOT NULL COMMENT '月折旧/摊销额',
  `accumulated_amount` decimal(18,2) DEFAULT 0.00 COMMENT '累计已计提金额',
  `source_bill_id` bigint DEFAULT NULL COMMENT '来源报销单ID',
  `industry_code` varchar(50) DEFAULT NULL COMMENT '行业编码',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/DISPOSED/FULLY_DEPRECIATED）',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_code` (`asset_code`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资产卡片表';

-- -----------------------------------------------------------
-- 32. 合同台账表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_contract_ledger`;
CREATE TABLE `fee_contract_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contract_code` varchar(50) NOT NULL COMMENT '合同编号',
  `contract_name` varchar(200) NOT NULL COMMENT '合同名称',
  `contract_type` varchar(30) NOT NULL COMMENT '合同类型（RENT/SERVICE/ANNUAL_FEE）',
  `dept_id` bigint NOT NULL COMMENT '归属部门ID',
  `budget_subject_code` varchar(100) DEFAULT NULL COMMENT '关联预算科目',
  `total_amount` decimal(18,2) NOT NULL COMMENT '合同总金额',
  `amortize_months` int NOT NULL COMMENT '摊销月数',
  `monthly_amount` decimal(18,2) NOT NULL COMMENT '月摊销额',
  `start_date` date NOT NULL COMMENT '合同起始日',
  `end_date` date NOT NULL COMMENT '合同到期日',
  `accumulated_amount` decimal(18,2) DEFAULT 0.00 COMMENT '累计已摊销',
  `industry_code` varchar(50) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/COMPLETED/TERMINATED）',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contract_code` (`contract_code`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='合同台账表';

-- -----------------------------------------------------------
-- 33. 计提记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_accrual_record`;
CREATE TABLE `fee_accrual_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `accrual_period` varchar(7) NOT NULL COMMENT '计提期间（YYYY-MM）',
  `source_type` varchar(30) NOT NULL COMMENT '来源类型（ASSET_DEPRECIATION/CONTRACT_AMORTIZE/LOAN_INTEREST）',
  `source_id` bigint NOT NULL COMMENT '来源ID（资产卡片/合同/借款ID）',
  `source_code` varchar(50) NOT NULL COMMENT '来源编号',
  `dept_id` bigint NOT NULL COMMENT '归属部门',
  `budget_subject_code` varchar(100) NOT NULL COMMENT '预算科目',
  `track_type` varchar(20) DEFAULT 'EXPENSE' COMMENT '轨道类型（计提全部归费用轨）',
  `amount` decimal(18,2) NOT NULL COMMENT '计提金额',
  `voucher_status` varchar(20) DEFAULT 'DRAFT' COMMENT '凭证状态（DRAFT/CONFIRMED/VOIDED）',
  `confirmed_by` bigint DEFAULT NULL COMMENT '财务复核人',
  `confirmed_time` datetime DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_period` (`accrual_period`),
  KEY `idx_source` (`source_type`, `source_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计提记录表';

-- -----------------------------------------------------------
-- 34. 盈利提示配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_profit_tip_config`;
CREATE TABLE `fee_profit_tip_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_type` varchar(20) NOT NULL COMMENT '科目类型（INCOME/EXPENSE/ASSET/LIABILITY）',
  `tip_type` varchar(20) NOT NULL COMMENT '提示类型（INCREASE/DECREASE/NEUTRAL）',
  `tip_text` varchar(500) NOT NULL COMMENT '提示文案',
  `tip_color` varchar(20) DEFAULT '#333' COMMENT '文字颜色',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码',
  `large_amount_threshold` decimal(18,2) DEFAULT NULL COMMENT '大额支出阈值（超则弹窗）',
  `force_popup` tinyint DEFAULT 0 COMMENT '是否强制弹窗（0否1是）',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_subject_type` (`subject_type`, `industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='盈利提示配置表';

-- -----------------------------------------------------------
-- 35. 补助标准配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_subsidy_standard`;
CREATE TABLE `fee_subsidy_standard` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subsidy_type` varchar(30) NOT NULL COMMENT '补助类型（TRAVEL/MEAL/TRANSPORT）',
  `subsidy_name` varchar(100) NOT NULL COMMENT '补助名称',
  `unit_amount` decimal(10,2) NOT NULL COMMENT '单位金额（元/天）',
  `unit` varchar(20) DEFAULT 'DAY' COMMENT '计量单位（DAY/TRIP/MONTH）',
  `budget_subject_code` varchar(100) DEFAULT NULL COMMENT '归集科目（职工福利费）',
  `max_days` int DEFAULT NULL COMMENT '最大天数限制',
  `industry_code` varchar(50) DEFAULT '*',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='补助标准配置表';

-- -----------------------------------------------------------
-- 36. 月度汇率表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_exchange_rate`;
CREATE TABLE `fee_exchange_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `currency_code` varchar(10) NOT NULL COMMENT '币种编码（USD/EUR/GBP/JPY）',
  `currency_name` varchar(50) NOT NULL COMMENT '币种名称',
  `rate_period` varchar(7) NOT NULL COMMENT '汇率期间（YYYY-MM）',
  `rate_to_cny` decimal(10,6) NOT NULL COMMENT '兑人民币汇率',
  `source` varchar(20) DEFAULT 'MANUAL' COMMENT '来源（MANUAL/AUTO）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_currency_period` (`currency_code`, `rate_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度汇率表';

-- -----------------------------------------------------------
-- 37. AI引导话术配置表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_ai_prompt_config`;
CREATE TABLE `fee_ai_prompt_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scene_code` varchar(50) NOT NULL COMMENT '场景编码（EXPENSE_BASIC/ASSET/LOAN/FOREIGN）',
  `scene_name` varchar(100) NOT NULL COMMENT '场景名称',
  `prompt_template` text NOT NULL COMMENT 'AI引导话术模板',
  `question_sequence` text DEFAULT NULL COMMENT '提问顺序JSON',
  `industry_code` varchar(50) DEFAULT '*' COMMENT '行业编码',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scene` (`scene_code`, `industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI引导话术配置表';

-- -----------------------------------------------------------
-- 38. 数据脱敏规则表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_desensitize_rule`;
CREATE TABLE `fee_desensitize_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `field_name` varchar(50) NOT NULL COMMENT '脱敏字段名',
  `field_label` varchar(100) DEFAULT NULL COMMENT '字段中文名',
  `mask_type` varchar(30) NOT NULL COMMENT '脱敏方式（PHONE/ID_CARD/BANK_CARD/NAME/CUSTOM）',
  `mask_pattern` varchar(100) DEFAULT NULL COMMENT '自定义脱敏正则',
  `intercept_action` varchar(20) DEFAULT 'MASK' COMMENT '拦截动作（MASK/BLOCK）',
  `sensitive_words` text DEFAULT NULL COMMENT '敏感词黑名单JSON',
  `status` varchar(20) DEFAULT 'ENABLED',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据脱敏规则表';

-- ============================================================
-- 八、企业版新增 — 借款台账与跨部门分摊
-- ============================================================

-- -----------------------------------------------------------
-- 39. 借款台账表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_loan_ledger`;
CREATE TABLE `fee_loan_ledger` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `loan_code` varchar(50) NOT NULL COMMENT '借款编号',
  `loan_type` varchar(20) NOT NULL COMMENT '借款类型（SHORT_TERM/LONG_TERM）',
  `principal` decimal(18,2) NOT NULL COMMENT '本金',
  `annual_rate` decimal(6,4) NOT NULL COMMENT '年化利率',
  `monthly_interest` decimal(18,2) NOT NULL COMMENT '月利息（principal×annual_rate÷12）',
  `start_date` date NOT NULL COMMENT '借款起始日',
  `end_date` date NOT NULL COMMENT '借款到期日',
  `interest_cycle` varchar(20) DEFAULT 'MONTHLY' COMMENT '计提周期（MONTHLY/QUARTERLY）',
  `dept_id` bigint DEFAULT NULL COMMENT '归属部门',
  `budget_subject_code` varchar(100) DEFAULT '3003001' COMMENT '关联科目（默认利息支出）',
  `accumulated_interest` decimal(18,2) DEFAULT 0.00 COMMENT '累计已计提利息',
  `source` varchar(20) DEFAULT 'MANUAL' COMMENT '录入来源（AI/MANUAL）',
  `status` varchar(20) DEFAULT 'ACTIVE' COMMENT '状态（ACTIVE/SETTLED/OVERDUE）',
  `create_by` varchar(64) DEFAULT '',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) DEFAULT '',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_loan_code` (`loan_code`),
  KEY `idx_status` (`status`),
  KEY `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='借款台账表';

-- -----------------------------------------------------------
-- 40. 跨部门分摊记录表
-- -----------------------------------------------------------
DROP TABLE IF EXISTS `fee_cross_dept_allocation_log`;
CREATE TABLE `fee_cross_dept_allocation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `allocation_period` varchar(7) NOT NULL COMMENT '分摊期间（YYYY-MM）',
  `source_dept_id` bigint NOT NULL COMMENT '来源部门（虚拟待分摊部门）',
  `target_dept_id` bigint NOT NULL COMMENT '目标部门',
  `budget_subject_code` varchar(100) NOT NULL COMMENT '预算科目',
  `total_amount` decimal(18,2) NOT NULL COMMENT '待分摊总额',
  `alloc_ratio` decimal(5,4) NOT NULL COMMENT '分摊比例',
  `alloc_amount` decimal(18,2) NOT NULL COMMENT '分摊金额',
  `alloc_basis` varchar(30) DEFAULT 'HEADCOUNT' COMMENT '分摊依据（HEADCOUNT/WORKHOURS）',
  `voucher_no` varchar(50) DEFAULT NULL COMMENT '调整凭证号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_period` (`allocation_period`),
  KEY `idx_source_dept` (`source_dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='跨部门分摊记录表';

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 企业专业版表清单（共40张）：
-- 高级版38张 + 企业版新增2张
-- 新增：fee_loan_ledger, fee_cross_dept_allocation_log
--
-- 完整表清单：
-- 【基础版20张】
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
-- 【高级版新增18张】
-- 21. fee_over_budget_approval  超预算审批表
-- 22. fee_budget_adjustment_app 预算调整申请表
-- 23. fee_reserved_budget       预留预算表（资金池）
-- 24. fee_reserved_allocation_log 预留预算分摊记录表
-- 25. fee_cost_allocation       费用分摊记录表
-- 26. fee_cost_allocation_rule  费用分摊规则表
-- 27. fee_compliance_config     发票合规级别配置表
-- 28. fee_compliance_record     发票合规校验记录表
-- 29. fee_industry_config       行业配置主数据表
-- 30. fee_budget_track_config   三轨预算管控配置表
-- 31. fee_asset_card            资产卡片表
-- 32. fee_contract_ledger       合同台账表
-- 33. fee_accrual_record        计提记录表
-- 34. fee_profit_tip_config     盈利提示配置表
-- 35. fee_subsidy_standard      补助标准配置表
-- 36. fee_exchange_rate         月度汇率表
-- 37. fee_ai_prompt_config      AI引导话术配置表
-- 38. fee_desensitize_rule      数据脱敏规则表
-- 【企业版新增2张】
-- 39. fee_loan_ledger           借款台账表
-- 40. fee_cross_dept_allocation_log 跨部门分摊记录表
-- ============================================================
