package com.ruoyi.framework.interceptor;

import com.ruoyi.common.core.service.TenantChecker;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.spring.SpringUtils;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户只读宽限期写操作拦截器（T14.2）
 * <p>
 * 当租户处于 GRACE 宽限期状态时，拦截所有非只读请求（POST/PUT/DELETE/PATCH），
 * 返回 403 提示续费，仅允许 GET 查询通过。
 * <p>
 * 拦截顺序：SaTokenConfig（登录+租户状态校验）→ FeatureInterceptor（功能开关）→ 本拦截器（宽限期写拦截）
 *
 * @author ruoyi
 * @date 2026-09-10
 */
@Slf4j
@Component
public class TenantWriteBlockInterceptor implements HandlerInterceptor {

    /** 只读 HTTP 方法 */
    private static final String[] READ_METHODS = {"GET", "HEAD", "OPTIONS"};

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只读方法直接放行
        String method = request.getMethod();
        for (String readMethod : READ_METHODS) {
            if (readMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        // 写操作：检查租户是否在宽限期内
        try {
            TenantChecker checker = SpringUtils.getBean(TenantChecker.class);
            if (checker.isCurrentTenantInGracePeriod()) {
                throw new ServiceException("租户授权已到期，当前处于只读宽限期，禁止写入操作，请及时续费");
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            // 检测异常不阻塞主流程，放行
            log.warn("[TenantWriteBlock] 宽限期检测异常，放行请求", e);
        }
        return true;
    }
}
