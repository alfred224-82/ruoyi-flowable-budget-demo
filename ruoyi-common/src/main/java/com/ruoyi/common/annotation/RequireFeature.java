package com.ruoyi.common.annotation;

import java.lang.annotation.*;

/**
 * 功能开关注解
 * 标注在Controller方法上，拦截器校验当前License是否包含该功能
 * 未授权时返回403 + "该功能未授权，请联系厂商升级"
 *
 * 用法：@RequireFeature("FEE_MULTI_CURRENCY")
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFeature {

    /**
     * 功能开关编码
     */
    String value();
}
