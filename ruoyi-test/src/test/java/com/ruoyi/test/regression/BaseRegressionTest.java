package com.ruoyi.test.regression;

import com.ruoyi.common.exception.BusinessException;
import com.ruoyi.common.exception.SystemBugException;
import com.ruoyi.common.exception.TechnicalException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 回归测试基类
 * <p>
 * 对应开发规范"十、日志到单元测试的映射"。
 * 提供针对三层异常体系的结构化断言方法，使每条线上日志都能映射为一个回归测试。
 * <p>
 * 使用方式：
 * <pre>
 * // 在 regression/business/ 下
 * class OrderNotFoundRegressionTest extends BaseRegressionTest {
 *     @Test
 *     void test_ORDER_NOT_FOUND_orderIsNull() {
 *         assertBusinessException("ORDER_NOT_FOUND", () -> {
 *             // 复现日志中的入参
 *             orderService.getOrder(null);
 *         });
 *     }
 * }
 * </pre>
 * <p>
 * 命名规范：test_{errorCode/component}_{reason}()
 *
 * @author ruoyi
 */
@Tag("regression")
@DisplayName("日志驱动回归测试")
public abstract class BaseRegressionTest {

    // ==================== BusinessException 断言 ====================

    /**
     * 断言抛出 BusinessException 且 errorCode 匹配
     *
     * @param expectedErrorCode 期望的错误码
     * @param executable        待执行的业务逻辑
     */
    protected void assertBusinessException(String expectedErrorCode, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertEquals(expectedErrorCode, ex.getErrorCode(),
            "errorCode 不匹配：期望 " + expectedErrorCode + "，实际 " + ex.getErrorCode());
    }

    /**
     * 断言抛出 BusinessException 且 errorCode 和 userMessage 均匹配
     *
     * @param expectedErrorCode 期望的错误码
     * @param expectedMessage   期望包含的用户提示（子串匹配）
     * @param executable        待执行的业务逻辑
     */
    protected void assertBusinessException(String expectedErrorCode, String expectedMessage, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertEquals(expectedErrorCode, ex.getErrorCode(),
            "errorCode 不匹配：期望 " + expectedErrorCode + "，实际 " + ex.getErrorCode());
        assertTrue(ex.getUserMessage().contains(expectedMessage),
            "userMessage 不包含期望内容：期望包含 '" + expectedMessage + "'，实际 '" + ex.getUserMessage() + "'");
    }

    /**
     * 断言抛出 BusinessException 且 errorCode 和 tenantId 均匹配
     *
     * @param expectedErrorCode 期望的错误码
     * @param expectedTenantId  期望的租户 ID
     * @param executable        待执行的业务逻辑
     */
    protected void assertBusinessExceptionWithTenant(String expectedErrorCode, Long expectedTenantId, Executable executable) {
        BusinessException ex = assertThrows(BusinessException.class, executable);
        assertEquals(expectedErrorCode, ex.getErrorCode());
        assertEquals(expectedTenantId, ex.getTenantId(),
            "tenantId 不匹配：期望 " + expectedTenantId + "，实际 " + ex.getTenantId());
    }

    // ==================== TechnicalException 断言 ====================

    /**
     * 断言抛出 TechnicalException 且 component 匹配
     *
     * @param expectedComponent 期望的组件名
     * @param executable        待执行的业务逻辑
     */
    protected void assertTechnicalException(String expectedComponent, Executable executable) {
        TechnicalException ex = assertThrows(TechnicalException.class, executable);
        assertEquals(expectedComponent, ex.getComponent(),
            "component 不匹配：期望 " + expectedComponent + "，实际 " + ex.getComponent());
    }

    /**
     * 断言抛出 TechnicalException 且 component 和 operation 均匹配
     *
     * @param expectedComponent 期望的组件名
     * @param expectedOperation 期望的操作名
     * @param executable        待执行的业务逻辑
     */
    protected void assertTechnicalException(String expectedComponent, String expectedOperation, Executable executable) {
        TechnicalException ex = assertThrows(TechnicalException.class, executable);
        assertEquals(expectedComponent, ex.getComponent());
        assertEquals(expectedOperation, ex.getOperation(),
            "operation 不匹配：期望 " + expectedOperation + "，实际 " + ex.getOperation());
    }

    // ==================== SystemBugException 断言 ====================

    /**
     * 断言抛出 SystemBugException
     *
     * @param executable 待执行的业务逻辑
     */
    protected void assertSystemBugException(Executable executable) {
        assertThrows(SystemBugException.class, executable);
    }

    /**
     * 断言抛出 SystemBugException 且 bugDescription 包含期望内容
     *
     * @param expectedDescription 期望包含的 Bug 描述（子串匹配）
     * @param executable          待执行的业务逻辑
     */
    protected void assertSystemBugException(String expectedDescription, Executable executable) {
        SystemBugException ex = assertThrows(SystemBugException.class, executable);
        assertTrue(ex.getBugDescription().contains(expectedDescription),
            "bugDescription 不包含期望内容：期望包含 '" + expectedDescription + "'，实际 '" + ex.getBugDescription() + "'");
    }

    /**
     * 断言抛出 SystemBugException 且定位到指定的类名
     *
     * @param expectedClassName 期望的类名（子串匹配）
     * @param executable        待执行的业务逻辑
     */
    protected void assertSystemBugExceptionAtClass(String expectedClassName, Executable executable) {
        SystemBugException ex = assertThrows(SystemBugException.class, executable);
        assertNotNull(ex.getClassName(), "className 为 null");
        assertTrue(ex.getClassName().contains(expectedClassName),
            "className 不匹配：期望包含 '" + expectedClassName + "'，实际 '" + ex.getClassName() + "'");
    }
}
