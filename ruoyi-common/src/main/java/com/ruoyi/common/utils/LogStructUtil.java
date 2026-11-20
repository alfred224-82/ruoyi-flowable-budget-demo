package com.ruoyi.common.utils;

import com.ruoyi.common.exception.BusinessException;
import com.ruoyi.common.exception.ExceptionType;
import com.ruoyi.common.exception.SystemBugException;
import com.ruoyi.common.exception.TechnicalException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结构化日志工具
 * <p>
 * 对应开发规范"三、结构化日志规范"。
 * 每条 ERROR / WARN 日志统一输出为 JSON 格式，包含标准字段，方便检索与 AI 解析。
 * <p>
 * 标准字段：
 * <ul>
 *   <li>timestamp — ISO 8601 时间戳</li>
 *   <li>exceptionType — BUSINESS / TECHNICAL / SYSTEM_BUG</li>
 *   <li>errorCode — 业务错误码（BUSINESS 时）</li>
 *   <li>component — 技术组件名（TECHNICAL 时）</li>
 *   <li>reason — 原因描述</li>
 *   <li>operation — 操作名（TECHNICAL 时）</li>
 *   <li>tenantId — 租户标识（BUSINESS 时）</li>
 *   <li>className / methodName / lineNumber — 代码位置（SYSTEM_BUG 时）</li>
 *   <li>retryable — 是否可重试（TECHNICAL 时）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   // 业务异常日志
 *   LogStructUtil.logBusinessError(log, businessException);
 *
 *   // 技术异常日志
 *   LogStructUtil.logTechnicalError(log, technicalException);
 *
 *   // 系统 Bug 日志
 *   LogStructUtil.logSystemBugError(log, systemBugException);
 *
 *   // 通用（根据异常类型自动分发）
 *   LogStructUtil.logStructuredError(log, exception);
 * </pre>
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LogStructUtil {

    /**
     * 根据异常类型自动分发到对应的结构化日志方法
     */
    public static void logStructuredError(Logger logger, Throwable exception) {
        if (exception instanceof BusinessException) {
            logBusinessError(logger, (BusinessException) exception);
        } else if (exception instanceof TechnicalException) {
            logTechnicalError(logger, (TechnicalException) exception);
        } else if (exception instanceof SystemBugException) {
            logSystemBugError(logger, (SystemBugException) exception);
        } else {
            // 未知异常，按 SYSTEM_BUG 处理
            logSystemBugError(logger, new SystemBugException(exception.getMessage(), exception));
        }
    }

    /**
     * 输出 BUSINESS 类型结构化 ERROR 日志
     */
    public static void logBusinessError(Logger logger, BusinessException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("errorCode", e.getErrorCode());
        fields.put("reason", e.getUserMessage());
        if (e.getTenantId() != null) {
            fields.put("tenantId", String.valueOf(e.getTenantId()));
        }
        logger.error(buildJson(ExceptionType.BUSINESS, fields), e);
    }

    /**
     * 输出 TECHNICAL 类型结构化 ERROR 日志
     */
    public static void logTechnicalError(Logger logger, TechnicalException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", e.getComponent());
        fields.put("operation", e.getOperation());
        fields.put("reason", e.getReason());
        fields.put("retryable", String.valueOf(e.isRetryable()));
        logger.error(buildJson(ExceptionType.TECHNICAL, fields), e);
    }

    /**
     * 输出 SYSTEM_BUG 类型结构化 ERROR 日志
     */
    public static void logSystemBugError(Logger logger, SystemBugException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("className", e.getClassName());
        fields.put("methodName", e.getMethodName());
        fields.put("lineNumber", String.valueOf(e.getLineNumber()));
        fields.put("reason", e.getBugDescription());
        logger.error(buildJson(ExceptionType.SYSTEM_BUG, fields), e);
    }

    /**
     * 输出 WARN 级别的结构化日志（用于需要关注但非错误的场景）
     */
    public static void logWarn(Logger logger, ExceptionType type, Map<String, String> fields) {
        logger.warn(buildJson(type, fields));
    }

    // ==================== 内部方法 ====================

    private static String buildJson(ExceptionType type, Map<String, String> fields) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{\"timestamp\":\"").append(Instant.now()).append("\"");
        sb.append(",\"exceptionType\":\"").append(type.name()).append("\"");
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                sb.append(",\"").append(escapeJson(entry.getKey()))
                    .append("\":\"").append(escapeJson(entry.getValue())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 简易 JSON 转义（避免引入额外依赖）
     */
    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}
