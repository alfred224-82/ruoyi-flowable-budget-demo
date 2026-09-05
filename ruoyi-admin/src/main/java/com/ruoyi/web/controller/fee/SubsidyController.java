package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeSubsidyStandard;
import com.ruoyi.system.mapper.FeeSubsidyStandardMapper;
import com.ruoyi.system.service.ISubsidyService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 补助管理Controller
 * 设计文档 Phase 9 Task 9.3
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/subsidy")
public class SubsidyController extends BaseController {

    private final ISubsidyService subsidyService;
    private final FeeSubsidyStandardMapper subsidyStandardMapper;

    /**
     * 补助标准列表
     */
    @RequireFeature("FEE_SUBSIDY_CONFIG")
    @SaCheckPermission("fee:subsidy:list")
    @GetMapping("/standards")
    public TableDataInfo<FeeSubsidyStandard> standards(
            @RequestParam(required = false) String industryCode, PageQuery pageQuery) {
        Page<FeeSubsidyStandard> page = subsidyStandardMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeSubsidyStandard>lambdaQuery()
                .eq(industryCode != null, FeeSubsidyStandard::getIndustryCode, industryCode)
                .orderByAsc(FeeSubsidyStandard::getSubsidyType)
        );
        return TableDataInfo.build(page);
    }

    /**
     * 保存补助标准
     */
    @RequireFeature("FEE_SUBSIDY_CONFIG")
    @SaCheckPermission("fee:subsidy:edit")
    @Log(title = "补助标准", businessType = BusinessType.UPDATE)
    @PostMapping("/standard")
    public R<Void> saveStandard(@RequestBody FeeSubsidyStandard standard) {
        if (standard.getId() != null) {
            return toAjax(subsidyStandardMapper.updateById(standard) > 0);
        }
        return toAjax(subsidyStandardMapper.insert(standard) > 0);
    }

    /**
     * 试算补助金额
     */
    @GetMapping("/calculate")
    public R<ISubsidyService.SubsidyCalcResult> calculate(
            @RequestParam String type,
            @RequestParam int days,
            @RequestParam(required = false, defaultValue = "*") String industryCode) {
        return R.ok(subsidyService.calculate(type, days, industryCode));
    }

    /**
     * 删除补助标准
     */
    @RequireFeature("FEE_SUBSIDY_CONFIG")
    @SaCheckPermission("fee:subsidy:remove")
    @Log(title = "补助标准", businessType = BusinessType.DELETE)
    @DeleteMapping("/standard/{id}")
    public R<Void> removeStandard(@PathVariable Long id) {
        return toAjax(subsidyStandardMapper.deleteById(id) > 0);
    }
}
