package com.ruoyi.common.exception;

/**
 * 业务错误码常量
 * <p>
 * 对应开发规范"二、异常分层设计"中 BusinessException 的 errorCode 字段。
 * errorCode 为机器可读码，用于日志检索与前端判断，格式为大写下划线风格。
 * <p>
 * 命名规范：{模块}_{实体}_{原因}，如 ORDER_NOT_FOUND、BUDGET_AMOUNT_EXCEED。
 * 后续按业务模块逐步补充。
 *
 * @author ruoyi
 */
public final class ErrorCodeConstants {

    private ErrorCodeConstants() {
    }

    // ==================== 通用 ====================

    /** 数据不存在（通用） */
    public static final String DATA_NOT_FOUND = "DATA_NOT_FOUND";

    /** 数据已存在（通用，用于唯一约束冲突） */
    public static final String DATA_ALREADY_EXISTS = "DATA_ALREADY_EXISTS";

    /** 参数校验失败（通用） */
    public static final String PARAM_INVALID = "PARAM_INVALID";

    /** 状态不允许操作 */
    public static final String STATUS_NOT_ALLOWED = "STATUS_NOT_ALLOWED";

    // ==================== 预算模块 ====================

    /** 预算编制不存在 */
    public static final String BUDGET_NOT_FOUND = "BUDGET_NOT_FOUND";

    /** 预算金额超限 */
    public static final String BUDGET_AMOUNT_EXCEED = "BUDGET_AMOUNT_EXCEED";

    /** 预算状态不允许提交 */
    public static final String BUDGET_STATUS_NOT_ALLOWED = "BUDGET_STATUS_NOT_ALLOWED";

    // ==================== 费用模块 ====================

    /** 报销单不存在 */
    public static final String EXPENSE_BILL_NOT_FOUND = "EXPENSE_BILL_NOT_FOUND";

    /** 报销单状态不允许操作 */
    public static final String EXPENSE_BILL_STATUS_NOT_ALLOWED = "EXPENSE_BILL_STATUS_NOT_ALLOWED";

    /** 超预算 */
    public static final String OVER_BUDGET = "OVER_BUDGET";

    // ==================== 租户模块 ====================

    /** 租户不存在 */
    public static final String TENANT_NOT_FOUND = "TENANT_NOT_FOUND";

    /** 租户已停用 */
    public static final String TENANT_DISABLED = "TENANT_DISABLED";

    /** 租户已过期 */
    public static final String TENANT_EXPIRED = "TENANT_EXPIRED";

    /** License 密钥无效 */
    public static final String LICENSE_INVALID = "LICENSE_INVALID";

    /** 功能未授权 */
    public static final String FEATURE_NOT_AUTHORIZED = "FEATURE_NOT_AUTHORIZED";

    // ==================== 用户/权限模块 ====================

    /** 用户不存在 */
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

    /** 用户已禁用 */
    public static final String USER_DISABLED = "USER_DISABLED";

    /** 密码错误 */
    public static final String PASSWORD_ERROR = "PASSWORD_ERROR";
}
