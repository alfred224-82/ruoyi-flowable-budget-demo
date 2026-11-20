package com.ruoyi.common.exception;

/**
 * 异常分层类型枚举
 * <p>
 * 对应开发规范"二、异常分层设计"中的三类异常，
 * 作为结构化日志、排障手册、单元测试三者统一的分类基础。
 *
 * @author ruoyi
 */
public enum ExceptionType {

    /**
     * 业务异常：用户可感知的业务规则校验失败（如订单不存在、余额不足）
     */
    BUSINESS,

    /**
     * 技术异常：外部组件故障（如 MySQL、Redis、HTTP 下游不可用）
     */
    TECHNICAL,

    /**
     * 系统 Bug：代码逻辑缺陷（如空指针、数组越界），需修复代码并发布
     */
    SYSTEM_BUG
}
