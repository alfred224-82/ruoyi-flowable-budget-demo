package com.ruoyi.common.exception;

/**
 * 系统 Bug 异常
 * <p>
 * 对应开发规范"二、异常分层设计"中的 SYSTEM_BUG 类型。
 * 用于代码逻辑缺陷导致的异常，如空指针、数组越界等。
 * 此类异常必须修复代码并发布。
 * <p>
 * 关键字段：
 * <ul>
 *   <li>className — 出问题的类名（自动从 StackTrace 提取）</li>
 *   <li>methodName — 出问题的方法名（自动从 StackTrace 提取）</li>
 *   <li>lineNumber — 出问题的行号（自动从 StackTrace 提取）</li>
 *   <li>bugDescription — Bug 描述，如"amount 为 null 导致空指针"</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   // 在 catch 块中包装
 *   catch (NullPointerException e) {
 *       throw new SystemBugException("OrderService.createOrder 第 87 行 空指针异常，amount 为 null", e);
 *   }
 *
 *   // 主动检测
 *   if (amount == null) {
 *       throw new SystemBugException("OrderService.createOrder amount 为 null，不应进入此分支");
 *   }
 * </pre>
 *
 * @author ruoyi
 */
public class SystemBugException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 出问题的类名
     */
    private String className;

    /**
     * 出问题的方法名
     */
    private String methodName;

    /**
     * 出问题的行号
     */
    private int lineNumber;

    /**
     * Bug 描述
     */
    private final String bugDescription;

    public SystemBugException(String bugDescription) {
        super(bugDescription);
        this.bugDescription = bugDescription;
        extractLocationFromStackTrace();
    }

    public SystemBugException(String bugDescription, Throwable cause) {
        super(bugDescription, cause);
        this.bugDescription = bugDescription;
        // 从 cause 的堆栈中提取位置信息
        if (cause != null && cause.getStackTrace() != null && cause.getStackTrace().length > 0) {
            StackTraceElement element = cause.getStackTrace()[0];
            this.className = element.getClassName();
            this.methodName = element.getMethodName();
            this.lineNumber = element.getLineNumber();
        } else {
            extractLocationFromStackTrace();
        }
    }

    /**
     * 从当前堆栈中提取调用位置（跳过 SystemBugException 自身的构造方法）
     */
    private void extractLocationFromStackTrace() {
        StackTraceElement[] stackTrace = getStackTrace();
        if (stackTrace != null && stackTrace.length > 1) {
            // stackTrace[0] 是本类的构造方法，stackTrace[1] 是真正的调用位置
            StackTraceElement element = stackTrace[1];
            this.className = element.getClassName();
            this.methodName = element.getMethodName();
            this.lineNumber = element.getLineNumber();
        }
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getBugDescription() {
        return bugDescription;
    }

    /**
     * 异常分层类型
     */
    public ExceptionType getExceptionType() {
        return ExceptionType.SYSTEM_BUG;
    }
}
