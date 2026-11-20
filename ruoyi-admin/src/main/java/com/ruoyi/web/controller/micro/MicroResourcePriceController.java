package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.StaffPricingBo;
import com.ruoyi.system.domain.vo.StaffPricingVo;
import com.ruoyi.system.service.IStaffPricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Boss PC 资源价格设置 Controller（T21.7，小微版）
 * <p>人员定价支持 4 种计价模式（计时/套餐/计量/一口价），对齐 jjadmin 定价体系。
 * <p>★房间定价由「房间/空间管理」(platform/room) 统一管理，本接口不再涉及。
 * <p>类级 {@code @RequireFeature("FEAT_SELF_BOOKING")}，与自助预约共用同一功能开关。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Validated
@RequireFeature("FEAT_SELF_BOOKING")
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/resourcePrice")
public class MicroResourcePriceController extends BaseController {

    private final IStaffPricingService staffPricingService;

    /**
     * 查询本租户全部人员价格列表（回填昵称/头像 + 计价模式）
     */
    @SaCheckPermission("micro:resourcePrice:view")
    @GetMapping("/staff/list")
    public R<List<StaffPricingVo>> listStaffPricing() {
        return R.ok(staffPricingService.listAll());
    }

    /**
     * 设置/更新人员价格（upsert：同一人员仅一条记录，支持 4 种计价模式）
     */
    @SaCheckPermission("micro:resourcePrice:staff:set")
    @Log(title = "设置人员价格", businessType = BusinessType.UPDATE)
    @PostMapping("/staff/save")
    public R<Long> saveStaffPrice(@Validated(AddGroup.class) @RequestBody StaffPricingBo bo) {
        return R.ok(staffPricingService.saveOrUpdatePrice(bo));
    }
}
