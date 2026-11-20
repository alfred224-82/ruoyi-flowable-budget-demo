package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.MicroShopConfigBo;
import com.ruoyi.system.domain.vo.MicroShopConfigVo;
import com.ruoyi.system.service.IMicroShopConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 店铺管理Controller（T15.24，老板 PC）
 * <p>营业时间设置（micro:shop:hours）/收款方式说明（micro:shop:payInfo）/店铺配置读取与合并保存（micro:shop:edit）；
 * 通知方式设置见 {@link MicroNotifyController}（micro:notify:config）。配置持久化到 tenant_biz_config.module_overrides。
 * <p>★V1 零支付合规：收款方式说明仅为「到店付/老板收款码」提示文案，不绑定任何支付账户、不含支付密钥字段。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/shop")
public class MicroShopController extends BaseController {

    private final IMicroShopConfigService shopConfigService;

    /**
     * 读取店铺配置（营业时间/收款方式说明/通知方式）
     * <p>★只读接口，登录即可（顾客端「联系我们」页也需要读取营业时间）
     */
    @SaCheckLogin
    @GetMapping("/config")
    public R<MicroShopConfigVo> getConfig() {
        return R.ok(shopConfigService.getConfig());
    }

    /**
     * 合并保存店铺配置（仅覆盖提交的非 null 字段）
     */
    @SaCheckPermission("micro:shop:edit")
    @Log(title = "店铺配置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public R<Void> saveConfig(@RequestBody MicroShopConfigBo bo) {
        return toAjax(shopConfigService.saveConfig(bo));
    }

    /**
     * 营业时间设置（micro:shop:hours）
     */
    @SaCheckPermission("micro:shop:hours")
    @Log(title = "营业时间设置", businessType = BusinessType.UPDATE)
    @PutMapping("/hours")
    public R<Void> saveHours(@RequestBody MicroShopConfigBo bo) {
        return toAjax(shopConfigService.saveBusinessHours(bo.getBusinessHours()));
    }

    /**
     * 收款方式说明（micro:shop:payInfo，★仅提示文案，不绑定支付账户）
     */
    @SaCheckPermission("micro:shop:payInfo")
    @Log(title = "收款方式说明", businessType = BusinessType.UPDATE)
    @PutMapping("/payInfo")
    public R<Void> savePayInfo(@RequestBody MicroShopConfigBo bo) {
        return toAjax(shopConfigService.savePayInfo(bo.getPayInfo()));
    }
}
