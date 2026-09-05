package com.ruoyi.web.controller.billing;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.SysSettlementBillVo;
import com.ruoyi.system.service.ISettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 结算对账Controller（T14.3，平台级；菜单 perms billing:settlement:*）
 * <p>按账期(yyyy-MM)汇总租户已支付订单与分账记录生成对账单，金额可追溯到订单。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/billing/settlement")
public class SettlementController extends BaseController {

    private final ISettlementService settlementService;

    /**
     * 对账单分页列表
     */
    @SaCheckPermission("billing:settlement:list")
    @GetMapping("/list")
    public TableDataInfo<SysSettlementBillVo> list(@RequestParam(required = false) String period,
                                                   @RequestParam(required = false) Long tenantId,
                                                   PageQuery pageQuery) {
        return settlementService.queryPageList(period, tenantId, pageQuery);
    }

    /**
     * 对账单详情
     */
    @SaCheckPermission("billing:settlement:query")
    @GetMapping("/{id}")
    public R<SysSettlementBillVo> getInfo(@PathVariable Long id) {
        return R.ok(settlementService.queryById(id));
    }

    /**
     * 生成（或刷新）指定租户指定账期的对账单
     */
    @SaCheckPermission("billing:settlement:generate")
    @Log(title = "结算对账", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public R<SysSettlementBillVo> generate(@RequestParam Long tenantId, @RequestParam String period) {
        return R.ok(settlementService.generate(tenantId, period));
    }
}
