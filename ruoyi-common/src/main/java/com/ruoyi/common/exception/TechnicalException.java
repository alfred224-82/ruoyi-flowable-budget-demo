package com.ruoyi.common.exception;

/**
 * 技术异常
 * <p>
 * 对应开发规范"二、异常分层设计"中的 TECHNICAL 类型。
 * 用于外部技术组件故障，如 MySQL 连接断开、Redis 不可用、HTTP 下游超时等。
 * <p>
 * 关键字段：
 * <ul>
 *   <li>component — 出问题的组件，如 mysql、redis、http-client</li>
 *   <li>operation — 当时正在做的操作，如 queryOrder、sendSms</li>
 *   <li>retryable — 是否可重试</li>
 *   <li>reason — 原因描述，遵循"组件 + 具体对象 + 状态/问题"的句式</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   throw new TechnicalException("redis", "getUserCache")
 *       .retryable(true)
 *       .reason("redis 连接池耗尽");
 * </pre>
 *
 * @author ruoyi
 */
public class TechnicalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 出问题的组件，如 mysql、redis、http-client、mq、oss
     */
    private final String component;

    /**
     * 当时正在做的操作
     */
    private final String operation;

    /**
     * 是否可重试
     */
    private boolean retryable;

    /**
     * 原因描述，遵循"组件 + 具体对象 + 状态/问题"的句式
     */
    private String reason;

    public TechnicalException(String component, String operation) {
        super(component + " " + operation + " 异常");
        this.component = component;
        this.operation = operation;
    }

    public TechnicalException(String component, String operation, Throwable cause) {
        super(component + " " + operation + " 异常: " + cause.getMessage(), cause);
        this.component = component;
        this.operation = operation;
    }

    /**
     * 设置是否可重试（链式调用）
     */
    public TechnicalException retryable(boolean retryable) {
        this.retryable = retryable;
        return this;
    }

    /**
     * 设置原因描述（链式调用）
     */
    public TechnicalException reason(String reason) {
        this.reason = reason;
        return this;
    }

    public String getComponent() {
        return component;
    }

    public String getOperation() {
        return operation;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public String getReason() {
        return reason;
    }

    /**
     * 异常分层类型
     */
    public ExceptionType getExceptionType() {
        return ExceptionType.TECHNICAL;
    }
}
