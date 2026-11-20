package com.ruoyi.common.exception;

/**
 * 业务异常
 * <p>
 * 对应开发规范"二、异常分层设计"中的 BUSINESS 类型。
 * 用于用户可感知的业务规则校验失败，如"订单不存在"、"余额不足"等。
 * <p>
 * 关键字段：
 * <ul>
 *   <li>errorCode — 机器可读码，如 ORDER_NOT_FOUND，用于日志检索与前端判断</li>
 *   <li>userMessage — 给用户看的提示文案</li>
 *   <li>tenantId — 租户隔离标识，便于多租户场景定位</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   throw new BusinessException("ORDER_NOT_FOUND", "订单不存在")
 *       .tenantId(LoginHelper.getTenantId());
 * </pre>
 *
 * @author ruoyi
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 机器可读错误码，如 ORDER_NOT_FOUND
     */
    private final String errorCode;

    /**
     * 给用户看的提示文案
     */
    private final String userMessage;

    /**
     * 租户隔离标识
     */
    private Long tenantId;

    public BusinessException(String errorCode, String userMessage) {
        super(userMessage);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    public BusinessException(String errorCode, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.errorCode = errorCode;
        this.userMessage = userMessage;
    }

    /**
     * 设置租户标识（链式调用）
     */
    public BusinessException tenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public Long getTenantId() {
        return tenantId;
    }

    /**
     * 异常分层类型
     */
    public ExceptionType getExceptionType() {
        return ExceptionType.BUSINESS;
    }
}
