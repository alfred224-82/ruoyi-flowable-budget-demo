package com.ruoyi.framework.aspectj;

import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 功能开关拦截切面
 * 拦截标注了 @RequireFeature 注解的方法，校验当前License是否包含该功能
 * 未授权时抛出异常 → 返回403 "该功能未授权，请联系厂商升级"
 *
 * 这是实现三版本（BASIC/ADVANCED/PROFESSIONAL，兼容存量 ENTERPRISE）功能隔离的核心机制
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class FeatureGuardAspect {

    private final FeatureChecker featureChecker;

    @Before("@annotation(com.ruoyi.common.annotation.RequireFeature)")
    public void checkFeature(JoinPoint joinPoint) {
        // admin用户不受功能开关限制
        if (isAdminUser()) {
            return;
        }
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireFeature annotation = method.getAnnotation(RequireFeature.class);
        if (annotation == null) {
            // 尝试从类级别获取
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequireFeature.class);
        }
        if (annotation != null) {
            String featureCode = annotation.value();
            if (!featureChecker.isFeatureEnabled(featureCode)) {
                log.warn("[FeatureGuard] 功能未授权: {} | 当前版本: {}", featureCode, featureChecker.getCurrentEdition());
                throw new ServiceException("该功能【" + featureCode + "】未授权，当前版本为 " + featureChecker.getCurrentEdition() + "，请联系厂商升级");
            }
        }
    }

    /**
     * 判断当前用户是否为admin（超级管理员不受功能开关限制）
     */
    private boolean isAdminUser() {
        try {
            return LoginHelper.isAdmin();
        } catch (Exception e) {
            // 未登录或无法获取用户信息时，不视为admin
            return false;
        }
    }
}
