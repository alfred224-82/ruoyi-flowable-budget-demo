-- =============================================
-- 多租户改造：为所有业务表添加 tenant_id 字段
-- 版本：v1.0
-- 日期：2026-08-01
-- 说明：执行前请备份数据库，默认租户ID为1
-- =============================================

-- ----------------------------
-- 1. 报销单表 fee_expense_bill
-- ----------------------------
ALTER TABLE `fee_expense_bill` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_expense_bill` (`tenant_id`);

-- ----------------------------
-- 2. 用途标签表 fee_purpose_tag
-- ----------------------------
ALTER TABLE `fee_purpose_tag` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_purpose_tag` (`tenant_id`);

-- ----------------------------
-- 3. 映射规则表 fee_mapping_rule
-- ----------------------------
ALTER TABLE `fee_mapping_rule` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_mapping_rule` (`tenant_id`);

-- ----------------------------
-- 4. 超预算审批表 fee_over_budget_approval
-- ----------------------------
ALTER TABLE `fee_over_budget_approval` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_over_budget_approval` (`tenant_id`);

-- ----------------------------
-- 5. 预算科目调剂流水表 fee_budget_adjustment_log
-- ----------------------------
ALTER TABLE `fee_budget_adjustment_log` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_budget_adjustment_log` (`tenant_id`);

-- ----------------------------
-- 6. 预算调整申请表 fee_budget_adjustment_app
-- ----------------------------
ALTER TABLE `fee_budget_adjustment_app` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_budget_adjustment_app` (`tenant_id`);

-- ----------------------------
-- 7. 费用分摊记录表 fee_cost_allocation
-- ----------------------------
ALTER TABLE `fee_cost_allocation` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_cost_allocation` (`tenant_id`);

-- ----------------------------
-- 8. 费用分摊规则表 fee_cost_allocation_rule
-- ----------------------------
ALTER TABLE `fee_cost_allocation_rule` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_cost_allocation_rule` (`tenant_id`);

-- ----------------------------
-- 9. 预算编制主表 budget_preparation
-- ----------------------------
ALTER TABLE `budget_preparation` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `budget_preparation` (`tenant_id`);

-- ----------------------------
-- 10. 预算编制明细表 budget_preparation_detail
-- ----------------------------
ALTER TABLE `budget_preparation_detail` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `budget_preparation_detail` (`tenant_id`);

-- ----------------------------
-- 11. 预算科目表 budget_subject
-- ----------------------------
ALTER TABLE `budget_subject` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `budget_subject` (`tenant_id`);

-- ----------------------------
-- 12. 预算驳回历史表 budget_reject_history
-- ----------------------------
ALTER TABLE `budget_reject_history` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `budget_reject_history` (`tenant_id`);

-- ----------------------------
-- 13. 预算校验规则表 budget_validation_rule
-- ----------------------------
ALTER TABLE `budget_validation_rule` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `budget_validation_rule` (`tenant_id`);

-- ----------------------------
-- 14. 预算表单 budget_sheet
-- ----------------------------
ALTER TABLE `budget_sheet` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `budget_sheet` (`tenant_id`);

-- ----------------------------
-- 15. 预留预算表 fee_reserved_budget
-- ----------------------------
ALTER TABLE `fee_reserved_budget` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_reserved_budget` (`tenant_id`);

-- ----------------------------
-- 16. 预留预算分摊记录表 fee_reserved_allocation_log
-- ----------------------------
ALTER TABLE `fee_reserved_allocation_log` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_reserved_allocation_log` (`tenant_id`);

-- ----------------------------
-- 17. 操作日志表 fee_operation_log
-- ----------------------------
ALTER TABLE `fee_operation_log` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `log_id`;
CREATE INDEX `idx_tenant_id` ON `fee_operation_log` (`tenant_id`);

-- ----------------------------
-- 18. 补助标准表 fee_subsidy_standard
-- ----------------------------
ALTER TABLE `fee_subsidy_standard` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_subsidy_standard` (`tenant_id`);

-- ----------------------------
-- 19. 合同台账表 fee_contract_ledger
-- ----------------------------
ALTER TABLE `fee_contract_ledger` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_contract_ledger` (`tenant_id`);

-- ----------------------------
-- 20. 资金池配置表 fee_fund_pool_config
-- ----------------------------
ALTER TABLE `fee_fund_pool_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_fund_pool_config` (`tenant_id`);

-- ----------------------------
-- 21. 工作流配置表 fee_workflow_config
-- ----------------------------
ALTER TABLE `fee_workflow_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_workflow_config` (`tenant_id`);

-- ----------------------------
-- 22. 工作流节点配置表 fee_workflow_node_config
-- ----------------------------
ALTER TABLE `fee_workflow_node_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_workflow_node_config` (`tenant_id`);

-- ----------------------------
-- 23. 福利政策配置表 fee_welfare_config（已有tenant_id则跳过）
-- ----------------------------
-- 如果表中已存在tenant_id字段，请注释掉下面两行
-- ALTER TABLE `fee_welfare_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
-- CREATE INDEX `idx_tenant_id` ON `fee_welfare_config` (`tenant_id`);

-- ----------------------------
-- 24. 计提记录表 fee_accrual_record
-- ----------------------------
ALTER TABLE `fee_accrual_record` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_accrual_record` (`tenant_id`);

-- ----------------------------
-- 25. AI引导话术配置表 fee_ai_prompt_config
-- ----------------------------
ALTER TABLE `fee_ai_prompt_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_ai_prompt_config` (`tenant_id`);

-- ----------------------------
-- 26. 资产卡片表 fee_asset_card
-- ----------------------------
ALTER TABLE `fee_asset_card` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_asset_card` (`tenant_id`);

-- ----------------------------
-- 27. 三轨预算管控配置表 fee_budget_track_config
-- ----------------------------
ALTER TABLE `fee_budget_track_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_budget_track_config` (`tenant_id`);

-- ----------------------------
-- 28. 发票合规配置表 fee_compliance_config
-- ----------------------------
ALTER TABLE `fee_compliance_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_compliance_config` (`tenant_id`);

-- ----------------------------
-- 29. 发票合规校验记录表 fee_compliance_record
-- ----------------------------
ALTER TABLE `fee_compliance_record` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_compliance_record` (`tenant_id`);

-- ----------------------------
-- 30. 跨部门分摊记录表 fee_cross_dept_allocation_log
-- ----------------------------
ALTER TABLE `fee_cross_dept_allocation_log` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_cross_dept_allocation_log` (`tenant_id`);

-- ----------------------------
-- 31. 数据脱敏规则表 fee_desensitize_rule
-- ----------------------------
ALTER TABLE `fee_desensitize_rule` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_desensitize_rule` (`tenant_id`);

-- ----------------------------
-- 32. 月度汇率表 fee_exchange_rate
-- ----------------------------
ALTER TABLE `fee_exchange_rate` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_exchange_rate` (`tenant_id`);

-- ----------------------------
-- 33. 行业配置表 fee_industry_config
-- ----------------------------
ALTER TABLE `fee_industry_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_industry_config` (`tenant_id`);

-- ----------------------------
-- 34. 借款台账表 fee_loan_ledger
-- ----------------------------
ALTER TABLE `fee_loan_ledger` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_loan_ledger` (`tenant_id`);

-- ----------------------------
-- 35. 盈利提示配置表 fee_profit_tip_config
-- ----------------------------
ALTER TABLE `fee_profit_tip_config` ADD COLUMN `tenant_id` BIGINT DEFAULT 1 COMMENT '租户ID' AFTER `id`;
CREATE INDEX `idx_tenant_id` ON `fee_profit_tip_config` (`tenant_id`);

-- ----------------------------
-- 36. 预算科目表 budget_subject（补充subject_type字段，如已存在请忽略）
-- ----------------------------
-- ALTER TABLE `budget_subject` ADD COLUMN `subject_type` VARCHAR(20) DEFAULT 'EXPENSE' COMMENT '科目类型' AFTER `description`;

-- ----------------------------
-- 37. sys_user 表添加 tenant_id（用户归属租户）
-- ----------------------------
ALTER TABLE `sys_user` ADD COLUMN `tenant_id` BIGINT DEFAULT NULL COMMENT '租户ID' AFTER `user_id`;
CREATE INDEX `idx_tenant_id` ON `sys_user` (`tenant_id`);

-- ----------------------------
-- 完成提示
-- ----------------------------
-- 所有业务表已添加 tenant_id 字段，默认值为 1（初始租户）
-- 请根据实际租户分配情况更新各表 tenant_id 值
