/**
 * 字段长度映射表
 * <p>
 * 对应开发规范"七、前端页面约束：防脏数据入库" + "八、数据库约束"。
 * 每个常量对应数据库表字段的 maxLength，前端文本框据此限制输入长度。
 * <p>
 * 命名规范：{表名/模块}_{字段名}_MAX
 * 维护规则：新增/修改表字段时，同步更新此文件中的常量。
 * <p>
 * 使用方式：
 * <pre>
 * import { fieldLimits } from '@/utils/fieldLimits'
 * // 在模板中
 * <el-input maxlength="fieldLimits.BUDGET_SUBJECT_NAME_MAX" />
 * </pre>
 */

// ==================== 预算科目 budget_subject ====================
export const BUDGET_SUBJECT_CODE_MAX = 20
export const BUDGET_SUBJECT_NAME_MAX = 100
export const BUDGET_SUBJECT_ANCESTORS_MAX = 500
export const BUDGET_SUBJECT_DESCRIPTION_MAX = 500
export const BUDGET_SUBJECT_REMARK_MAX = 500

// ==================== 预算编制 budget_preparation ====================
export const BUDGET_PREPARATION_SHEET_NO_MAX = 30
export const BUDGET_PREPARATION_ORG_NAME_MAX = 100
export const BUDGET_PREPARATION_REMARK_MAX = 1000

// ==================== 报销单 fee_expense_bill ====================
export const EXPENSE_BILL_BILL_NO_MAX = 30
export const EXPENSE_BILL_ITEM_CATEGORY_MAX = 20
export const EXPENSE_BILL_PURPOSE_TAG_MAX = 20
export const EXPENSE_BILL_PROJECT_CODE_MAX = 30
export const EXPENSE_BILL_REMARK_MAX = 500

// ==================== 收款单 fee_receipt ====================
export const FEE_RECEIPT_RECEIPT_NO_MAX = 30
export const FEE_RECEIPT_PAYER_MAX = 100
export const FEE_RECEIPT_PROOF_URLS_MAX = 2000

// ==================== 成本中心 fee_cost_center ====================
export const FEE_COST_CENTER_CODE_MAX = 30
export const FEE_COST_CENTER_NAME_MAX = 100
export const FEE_COST_CENTER_REMARK_MAX = 500

// ==================== 预算科目映射 fee_mapping_rule ====================
export const FEE_MAPPING_RULE_CATEGORY_CODE_MAX = 20
export const FEE_MAPPING_RULE_SUBJECT_CODE_MAX = 20

// ==================== 系统参数 ====================
export const SYS_CONFIG_KEY_MAX = 100
export const SYS_CONFIG_VALUE_MAX = 2000
export const SYS_CONFIG_NAME_MAX = 100
export const SYS_CONFIG_REMARK_MAX = 500

// ==================== 用户/部门 ====================
export const SYS_USER_NAME_MAX = 30
export const SYS_USER_NICK_MAX = 30
export const SYS_USER_PHONE_MAX = 11
export const SYS_USER_EMAIL_MAX = 50
export const SYS_DEPT_NAME_MAX = 30
export const SYS_DEPT_PHONE_MAX = 11
export const SYS_DEPT_EMAIL_MAX = 50

// ==================== 便捷查询对象 ====================
export const fieldLimits = {
  // 预算科目
  BUDGET_SUBJECT_CODE_MAX,
  BUDGET_SUBJECT_NAME_MAX,
  BUDGET_SUBJECT_ANCESTORS_MAX,
  BUDGET_SUBJECT_DESCRIPTION_MAX,
  BUDGET_SUBJECT_REMARK_MAX,
  // 预算编制
  BUDGET_PREPARATION_SHEET_NO_MAX,
  BUDGET_PREPARATION_ORG_NAME_MAX,
  BUDGET_PREPARATION_REMARK_MAX,
  // 报销单
  EXPENSE_BILL_BILL_NO_MAX,
  EXPENSE_BILL_ITEM_CATEGORY_MAX,
  EXPENSE_BILL_PURPOSE_TAG_MAX,
  EXPENSE_BILL_PROJECT_CODE_MAX,
  EXPENSE_BILL_REMARK_MAX,
  // 收款单
  FEE_RECEIPT_RECEIPT_NO_MAX,
  FEE_RECEIPT_PAYER_MAX,
  FEE_RECEIPT_PROOF_URLS_MAX,
  // 成本中心
  FEE_COST_CENTER_CODE_MAX,
  FEE_COST_CENTER_NAME_MAX,
  FEE_COST_CENTER_REMARK_MAX,
  // 映射规则
  FEE_MAPPING_RULE_CATEGORY_CODE_MAX,
  FEE_MAPPING_RULE_SUBJECT_CODE_MAX,
  // 系统参数
  SYS_CONFIG_KEY_MAX,
  SYS_CONFIG_VALUE_MAX,
  SYS_CONFIG_NAME_MAX,
  SYS_CONFIG_REMARK_MAX,
  // 用户/部门
  SYS_USER_NAME_MAX,
  SYS_USER_NICK_MAX,
  SYS_USER_PHONE_MAX,
  SYS_USER_EMAIL_MAX,
  SYS_DEPT_NAME_MAX,
  SYS_DEPT_PHONE_MAX,
  SYS_DEPT_EMAIL_MAX
}

export default fieldLimits
