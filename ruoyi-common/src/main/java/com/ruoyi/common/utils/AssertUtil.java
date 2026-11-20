package com.ruoyi.common.utils;

import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.exception.BusinessException;
import com.ruoyi.common.exception.SystemBugException;
import com.ruoyi.common.exception.TechnicalException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Collection;

/**
 * 判空断言工具（fail-fast 模式）
 * <p>
 * 对应开发规范"六、后端代码约束：防空指针"。
 * 核心原则：fail-fast，入口处拦截，不让 null 往下流动。
 * <p>
 * 三类断言对应三类异常：
 * <ul>
 *   <li>notNull / notBlank — 抛 SystemBugException（代码 bug，不该为 null）</li>
 *   <li>notNullBiz / notBlankBiz — 抛 BusinessException（业务预期为空，如数据不存在）</li>
 *   <li>notNullTech — 抛 TechnicalException（外部调用返回 null）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 * // Service 入口参数判空（代码 bug）
 * AssertUtil.notNull(bo, "bo");
 * AssertUtil.notBlank(bo.getName(), "name");
 *
 * // DB 查询返回判空（业务预期为空）
 * SomeEntity entity = mapper.selectById(id);
 * AssertUtil.notNullBiz(entity, "ENTITY_NOT_FOUND", "数据不存在");
 *
 * // 外部调用返回判空（技术组件故障）
 * ExternalResult result = externalClient.call(param);
 * AssertUtil.notNullTech(result, "http-client", "callExternalService");
 *
 * // 集合判空（业务预期为空）
 * AssertUtil.notEmptyBiz(list, "ORDER_ITEM_EMPTY", "订单明细不能为空");
 * </pre>
 *
 * @author ruoyi
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssertUtil {

    // ==================== SystemBug 断言（代码 bug，不该为 null） ====================

    /**
     * 对象不为 null 断言
     * 为 null 时抛 SystemBugException，表示代码逻辑缺陷
     *
     * @param obj       待检查对象
     * @param fieldName 字段名（用于定位）
     */
    public static void notNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new SystemBugException(fieldName + " 为 null，不应进入此分支");
        }
    }

    /**
     * 字符串不为空（非 null 且非空白）断言
     * 为空时抛 SystemBugException
     *
     * @param str       待检查字符串
     * @param fieldName 字段名
     */
    public static void notBlank(String str, String fieldName) {
        if (StrUtil.isBlank(str)) {
            throw new SystemBugException(fieldName + " 为空白，不应进入此分支");
        }
    }

    /**
     * 集合不为空断言
     * 为空时抛 SystemBugException
     *
     * @param collection 待检查集合
     * @param fieldName  字段名
     */
    public static void notEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new SystemBugException(fieldName + " 为空集合，不应进入此分支");
        }
    }

    // ==================== Business 断言（业务预期为空） ====================

    /**
     * 对象不为 null 断言（业务语义）
     * 为 null 时抛 BusinessException，表示业务数据不存在
     *
     * @param obj       待检查对象
     * @param errorCode 机器可读错误码
     * @param message   用户提示文案
     */
    public static void notNullBiz(Object obj, String errorCode, String message) {
        if (obj == null) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 对象不为 null 断言（业务语义，简化版）
     * 为 null 时抛 BusinessException，使用通用错误码 DATA_NOT_FOUND
     *
     * @param obj     待检查对象
     * @param message 用户提示文案
     */
    public static void notNullBiz(Object obj, String message) {
        if (obj == null) {
            throw new BusinessException("DATA_NOT_FOUND", message);
        }
    }

    /**
     * 字符串不为空断言（业务语义）
     * 为空时抛 BusinessException
     *
     * @param str       待检查字符串
     * @param errorCode 机器可读错误码
     * @param message   用户提示文案
     */
    public static void notBlankBiz(String str, String errorCode, String message) {
        if (StrUtil.isBlank(str)) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 集合不为空断言（业务语义）
     * 为空时抛 BusinessException
     *
     * @param collection 待检查集合
     * @param errorCode  机器可读错误码
     * @param message    用户提示文案
     */
    public static void notEmptyBiz(Collection<?> collection, String errorCode, String message) {
        if (collection == null || collection.isEmpty()) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 条件为真断言（业务语义）
     * 条件为 false 时抛 BusinessException
     *
     * @param condition 条件表达式
     * @param errorCode 机器可读错误码
     * @param message   用户提示文案
     */
    public static void assertTrueBiz(boolean condition, String errorCode, String message) {
        if (!condition) {
            throw new BusinessException(errorCode, message);
        }
    }

    // ==================== Technical 断言（外部调用返回 null） ====================

    /**
     * 对象不为 null 断言（技术组件语义）
     * 为 null 时抛 TechnicalException，表示外部组件返回空
     *
     * @param obj       待检查对象（外部调用返回值）
     * @param component 组件名，如 mysql、redis、http-client
     * @param operation 操作名，如 queryOrder、getUserCache
     */
    public static void notNullTech(Object obj, String component, String operation) {
        if (obj == null) {
            throw new TechnicalException(component, operation)
                .retryable(true)
                .reason(component + " " + operation + " 返回 null");
        }
    }

    /**
     * 字符串不为空断言（技术组件语义）
     * 为空时抛 TechnicalException
     *
     * @param str       待检查字符串
     * @param component 组件名
     * @param operation 操作名
     */
    public static void notBlankTech(String str, String component, String operation) {
        if (StrUtil.isBlank(str)) {
            throw new TechnicalException(component, operation)
                .retryable(true)
                .reason(component + " " + operation + " 返回空字符串");
        }
    }

    // ==================== 状态断言 ====================

    /**
     * 状态相等断言（业务语义）
     * 不相等时抛 BusinessException，用于状态机校验
     *
     * @param actual    实际状态
     * @param expected  期望状态
     * @param errorCode 机器可读错误码
     * @param message   用户提示文案
     */
    public static void assertStateEq(String actual, String expected, String errorCode, String message) {
        if (!expected.equals(actual)) {
            throw new BusinessException(errorCode, message);
        }
    }
}
