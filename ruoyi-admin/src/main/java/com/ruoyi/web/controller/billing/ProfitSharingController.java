package com.ruoyi.web.controller.billing;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ProfitSharingRuleBo;
import com.ruoyi.system.domain.vo.SysProfitSharingRecordVo;
import com.ruoyi.system.domain.vo.SysProfitSharingRuleVo;
import com.ruoyi.system.service.IProfitSharingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 分账Controller（T14.3，平台级；菜单 perms billing:sharing:*）
 * <p>分账规则（平台服务费率 + 租户应得率，二者合计=1）与分账记录查询；
 * 分账在订单收款成功时由 {@link com.ruoyi.system.service.IBillingOrderService} 自动触发。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/billing/sharing")
public class ProfitSharingController extends BaseController {

    private final IProfitSharingService profitSharingService;

    /**
     * 查询租户分账规则（无则返回默认：平台 0、租户全额）
     */
    @SaCheckPermission("billing:sharing:list")
    @GetMapping("/rule")
    public R<SysProfitSharingRuleVo> getRule(@RequestParam Long tenantId) {
        return R.ok(profitSharingService.getRule(tenantId));
    }

    /**
     * 保存租户分账规则（tenant_rate = 1 - platform_rate）
     */
    @SaCheckPermission("billing:sharing:edit")
    @Log(title = "分账规则", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public R<Void> saveRule(@RequestParam Long tenantId,
                            @Validated(EditGroup.class) @RequestBody ProfitSharingRuleBo bo) {
        return toAjax(profitSharingService.saveRule(tenantId, bo));
    }

    /**
     * 分账记录分页查询
     */
    @SaCheckPermission("billing:sharing:query")
    @GetMapping("/records")
    public TableDataInfo<SysProfitSharingRecordVo> records(@RequestParam(required = false) String orderNo,
                                                           @RequestParam(required = false) Long tenantId,
                                                           PageQuery pageQuery) {
        return profitSharingService.queryRecordPageList(orderNo, tenantId, pageQuery);
    }
}
