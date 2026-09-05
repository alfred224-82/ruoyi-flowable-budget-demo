package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeIncomeConfirmationBo;
import com.ruoyi.system.domain.vo.FeeIncomeConfirmationVo;
import com.ruoyi.system.service.IFeeIncomeConfirmationService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 收入确认Controller（T5.4）
 * 三态：项目经理创建→待核验；财务核验通过/驳回；收款单联动
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/income")
public class FeeIncomeConfirmationController extends BaseController {

    private final IFeeIncomeConfirmationService confirmationService;

    /**
     * 收入确认列表（数据范围：项目经理仅本人参与项目）
     */
    @SaCheckPermission("fee:income:list")
    @GetMapping("/list")
    public TableDataInfo<FeeIncomeConfirmationVo> list(FeeIncomeConfirmationBo bo, PageQuery pageQuery) {
        return confirmationService.queryPageList(bo, pageQuery);
    }

    /**
     * 收入确认详情
     */
    @SaCheckPermission("fee:income:query")
    @GetMapping("/{id}")
    public R<FeeIncomeConfirmationVo> getInfo(@PathVariable Long id) {
        return R.ok(confirmationService.queryById(id));
    }

    /**
     * 项目经理创建收入确认（待核验）
     */
    @SaCheckPermission("fee:income:add")
    @Log(title = "收入确认", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody FeeIncomeConfirmationBo bo) {
        return R.ok(confirmationService.insertConfirmation(bo));
    }

    /**
     * 财务核验通过（待核验收入转已核验收入）
     */
    @SaCheckPermission("fee:income:verify")
    @Log(title = "收入核验", businessType = BusinessType.UPDATE)
    @PutMapping("/verify/{id}")
    public R<Void> verify(@PathVariable Long id) {
        return toAjax(confirmationService.verifyConfirmation(id));
    }

    /**
     * 财务核验驳回（冲销待核验收入）
     */
    @SaCheckPermission("fee:income:reject")
    @Log(title = "收入核验", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return toAjax(confirmationService.rejectConfirmation(id, reason));
    }
}
