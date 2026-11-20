package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.ComplianceAgreementVo;
import com.ruoyi.system.service.IComplianceAgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 合规协议 Controller（T25.5，小微版 M25）
 * <p>登录守卫用：检查是否已签署最新协议。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/complianceAgreement")
public class MicroComplianceAgreementController extends BaseController {

    private final IComplianceAgreementService agreementService;

    /**
     * 获取当前协议版本/状态
     */
    @SaCheckPermission("micro:complianceAgreement:view")
    @GetMapping("/current")
    public R<ComplianceAgreementVo> current() {
        return R.ok(agreementService.getCurrent());
    }

    /**
     * 签署协议
     */
    @SaCheckPermission("micro:complianceAgreement:sign")
    @Log(title = "合规协议-签署", businessType = BusinessType.INSERT)
    @PostMapping("/sign")
    public R<Void> sign(@RequestParam String agreementVersion, HttpServletRequest request) {
        String ip = getClientIp(request);
        agreementService.sign(agreementVersion, ip);
        return R.ok();
    }

    /**
     * 检查是否已签署（登录守卫用）
     */
    @SaCheckPermission("micro:complianceAgreement:view")
    @GetMapping("/check")
    public R<Boolean> check() {
        return R.ok(agreementService.checkSigned());
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
