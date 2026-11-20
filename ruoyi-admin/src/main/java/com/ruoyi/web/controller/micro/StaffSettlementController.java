package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.StaffSettlementBo;
import com.ruoyi.system.domain.vo.SettlementOrderDetailVo;
import com.ruoyi.system.domain.vo.StaffSettlementVo;
import com.ruoyi.system.service.IStaffSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 人员结算Controller（T15.13，老板 PC，★V1 只记账不打款）
 * <p>① 选择员工+周期→自动汇总单量→算应发金额（preview）；② 生成「待发放」结算单（generate）；
 * ③ 线下转账后「标记已线下发放」→改状态 + 记支出流水（markPaid）；④ 结算历史（history）。
 * <p>★零支付合规：全流程无任何支付/转账接口调用。perms 对齐 M15_06：micro:settle:list/create/generate/markPaid/detail/history。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/settle")
public class StaffSettlementController extends BaseController {

    private final IStaffSettlementService staffSettlementService;

    /**
     * 结算单列表
     */
    @SaCheckPermission("micro:settle:list")
    @GetMapping("/list")
    public TableDataInfo<StaffSettlementVo> list(StaffSettlementBo bo, PageQuery pageQuery) {
        return staffSettlementService.queryPageList(bo, pageQuery);
    }

    /**
     * ④ 结算历史（含「已发/待发」状态）
     */
    @SaCheckPermission("micro:settle:history")
    @GetMapping("/history")
    public TableDataInfo<StaffSettlementVo> history(StaffSettlementBo bo, PageQuery pageQuery) {
        return staffSettlementService.queryPageList(bo, pageQuery);
    }

    /**
     * 结算单详情
     */
    @SaCheckPermission("micro:settle:detail")
    @GetMapping("/{id}")
    public R<StaffSettlementVo> getInfo(@PathVariable Long id) {
        return R.ok(staffSettlementService.queryById(id));
    }

    /**
     * ① 选择员工 + 周期 → 自动汇总单量 + 算应发金额（预览，不落库）
     */
    @SaCheckPermission("micro:settle:create")
    @GetMapping("/preview")
    public R<StaffSettlementVo> preview(@RequestParam Long staffUserId,
                                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date periodStart,
                                        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date periodEnd) {
        return R.ok(staffSettlementService.preview(staffUserId, periodStart, periodEnd));
    }

    /**
     * ② 生成「待发放」结算单
     */
    @SaCheckPermission("micro:settle:generate")
    @Log(title = "人员结算", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> generate(@Validated(AddGroup.class) @RequestBody StaffSettlementBo bo) {
        return R.ok(staffSettlementService.generate(bo));
    }

    /**
     * ③ 标记已线下发放（★线下转账后点此，改状态 + 记 cash_flow 支出流水，不调任何支付接口）
     */
    @SaCheckPermission("micro:settle:markPaid")
    @Log(title = "人员结算发放", businessType = BusinessType.UPDATE)
    @PutMapping("/markPaid/{id}")
    public R<Void> markPaid(@PathVariable Long id) {
        return toAjax(staffSettlementService.markPaidOffline(id));
    }

    /**
     * 删除结算单（仅允许删除「待发放」状态）
     */
    @SaCheckPermission("micro:settle:remove")
    @Log(title = "人员结算删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Collection<Long> ids) {
        return toAjax(staffSettlementService.deleteByIds(ids));
    }

    /**
     * 查询结算单关联的订单明细（含顾客、活动、时长、起止时间）
     */
    @SaCheckPermission("micro:settle:detail")
    @GetMapping("/{id}/orders")
    public R<List<SettlementOrderDetailVo>> listOrders(@PathVariable Long id) {
        return R.ok(staffSettlementService.listSettlementOrders(id));
    }
}
