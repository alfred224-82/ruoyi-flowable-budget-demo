package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.TenantPaymentConfigBo;
import com.ruoyi.system.domain.vo.TenantPaymentConfigVo;
import com.ruoyi.system.service.ITenantPaymentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户收款配置Controller（T13.6 主体独立与收款分离，高级版+）
 * <p>apiKey 加密存储、返回脱敏；每租户唯一。self_merchant 收款直达，platform_proxy 平台代收后分账（M14）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/payment")
public class PaymentController extends BaseController {

    private final ITenantPaymentConfigService paymentConfigService;

    /**
     * 查询当前租户收款配置（apiKey 脱敏）
     */
    @SaCheckPermission("biz:payment:query")
    @GetMapping("/config")
    public R<TenantPaymentConfigVo> getConfig() {
        return R.ok(paymentConfigService.getConfig());
    }

    /**
     * 保存收款配置（apiKey 加密存储，幂等 upsert）
     */
    @SaCheckPermission("biz:payment:edit")
    @Log(title = "收款配置", businessType = BusinessType.UPDATE)
    @PostMapping("/config")
    public R<Void> saveConfig(@Validated(AddGroup.class) @RequestBody TenantPaymentConfigBo bo) {
        return toAjax(paymentConfigService.saveConfig(bo));
    }
}
