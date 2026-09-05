package com.ruoyi.framework.interceptor;

import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 功能开关拦截器
 * 拦截标注了 @RequireFeature 注解的Controller方法
 * 校验当前License是否包含该功能，未授权时抛出异常
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeatureInterceptor implements HandlerInterceptor {

    private final FeatureChecker featureChecker;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        // admin用户不受功能开关限制
        if (isAdminUser()) {
            return true;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        // 优先检查方法级注解
        RequireFeature methodAnn = handlerMethod.getMethodAnnotation(RequireFeature.class);
        if (methodAnn != null) {
            checkFeature(methodAnn.value());
            return true;
        }
        // 再检查类级注解
        RequireFeature classAnn = handlerMethod.getBeanType().getAnnotation(RequireFeature.class);
        if (classAnn != null) {
            checkFeature(classAnn.value());
        }
        return true;
    }

    private void checkFeature(String featureCode) {
        if (!featureChecker.isFeatureEnabled(featureCode)) {
            throw new ServiceException("当前版本不支持此功能，请升级License");
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
