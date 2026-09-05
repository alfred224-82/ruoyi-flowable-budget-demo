package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeClockRecord;
import com.ruoyi.system.domain.FeeClockTriggerRule;
import com.ruoyi.system.service.IFeeClockService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 打卡定位Controller（T6.2 / FEAT_TIME_CLOCK，高级版增量）
 * 触发规则（差旅招待/≥500/历史异常≥2）+ 定位存疑仅标记不拦截
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/clock")
public class FeeClockController extends BaseController {

    private final IFeeClockService feeClockService;

    /**
     * 打卡记录分页查询
     */
    @RequireFeature("FEAT_TIME_CLOCK")
    @SaCheckPermission("fee:clock:list")
    @GetMapping("/list")
    public TableDataInfo<FeeClockRecord> list(
            @RequestParam(required = false) Long billId,
            @RequestParam(required = false) Long userId,
            PageQuery pageQuery) {
        Page<FeeClockRecord> page = feeClockService.pageRecords(billId, userId, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 打卡记录详情
     */
    @RequireFeature("FEAT_TIME_CLOCK")
    @SaCheckPermission("fee:clock:query")
    @GetMapping("/{id}")
    public R<FeeClockRecord> getInfo(@PathVariable Long id) {
        return R.ok(feeClockService.getRecord(id));
    }

    /**
     * 报销单关联打卡记录（追溯时间线）
     */
    @RequireFeature("FEAT_TIME_CLOCK")
    @SaCheckPermission("fee:clock:query")
    @GetMapping("/bill/{billId}")
    public R<List<FeeClockRecord>> listByBill(@PathVariable Long billId) {
        return R.ok(feeClockService.listByBill(billId));
    }

    /**
     * 打卡触发规则配置列表（内置模板 + 租户级覆盖）
     */
    @RequireFeature("FEAT_TIME_CLOCK")
    @SaCheckPermission("fee:clock:config")
    @GetMapping("/config/list")
    public R<List<FeeClockTriggerRule>> configList() {
        return R.ok(feeClockService.listTriggerRuleConfigs());
    }

    /**
     * 新增租户级打卡触发规则
     */
    @RequireFeature("FEAT_TIME_CLOCK")
    @SaCheckPermission("fee:clock:edit")
    @Log(title = "打卡触发规则", businessType = BusinessType.INSERT)
    @PostMapping("/config")
    public R<Void> addConfig(@Validated @RequestBody FeeClockTriggerRule rule) {
        return toAjax(feeClockService.saveTriggerRule(rule));
    }

    /**
     * 修改打卡触发规则（仅限本租户行）
     */
    @RequireFeature("FEAT_TIME_CLOCK")
    @SaCheckPermission("fee:clock:edit")
    @Log(title = "打卡触发规则", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public R<Void> editConfig(@Validated @RequestBody FeeClockTriggerRule rule) {
        return toAjax(feeClockService.updateTriggerRule(rule));
    }

    /**
     * 删除打卡触发规则（仅限本租户行）
     */
    @RequireFeature("FEAT_TIME_CLOCK")
    @SaCheckPermission("fee:clock:edit")
    @Log(title = "打卡触发规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/config/{id}")
    public R<Void> removeConfig(@PathVariable Long id) {
        return toAjax(feeClockService.deleteTriggerRule(id));
    }
}
