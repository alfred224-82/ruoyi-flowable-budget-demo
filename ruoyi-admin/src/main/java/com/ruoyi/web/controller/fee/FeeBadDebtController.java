package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeBadDebtBo;
import com.ruoyi.system.domain.vo.FeeBadDebtVo;
import com.ruoyi.system.service.IFeeBadDebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 烂账管理Controller（T5.6）
 * 风险标记 + 坏账计提 + 项目状态 LOST + 看板风险预警区联动
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/baddebt")
@RequireFeature("FEAT_BAD_DEBT")
public class FeeBadDebtController extends BaseController {

    private final IFeeBadDebtService badDebtService;

    /**
     * 烂账列表
     */
    @SaCheckPermission("fee:baddebt:list")
    @GetMapping("/list")
    public TableDataInfo<FeeBadDebtVo> list(FeeBadDebtBo bo, PageQuery pageQuery) {
        return badDebtService.queryPageList(bo, pageQuery);
    }

    /**
     * 烂账详情
     */
    @SaCheckPermission("fee:baddebt:query")
    @GetMapping("/{id}")
    public R<FeeBadDebtVo> getInfo(@PathVariable Long id) {
        return R.ok(badDebtService.queryById(id));
    }

    /**
     * 风险标记（带计提金额时同步计提坏账）
     */
    @SaCheckPermission("fee:baddebt:mark")
    @Log(title = "烂账标记", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> mark(@Validated(AddGroup.class) @RequestBody FeeBadDebtBo bo) {
        return R.ok(badDebtService.insertBadDebt(bo));
    }

    /**
     * 追加坏账计提
     */
    @SaCheckPermission("fee:baddebt:mark")
    @Log(title = "坏账计提", businessType = BusinessType.UPDATE)
    @PutMapping("/provision/{id}")
    public R<Void> provision(@PathVariable Long id, @RequestParam BigDecimal amount) {
        return toAjax(badDebtService.provisionBadDebt(id, amount));
    }

    /**
     * 跟进（追加跟进记录）
     */
    @SaCheckPermission("fee:baddebt:follow")
    @Log(title = "烂账跟进", businessType = BusinessType.UPDATE)
    @PutMapping("/follow/{id}")
    public R<Void> follow(@PathVariable Long id, @RequestParam String notes) {
        return toAjax(badDebtService.followUp(id, notes));
    }

    /**
     * 关闭烂账（项目状态置 LOST）
     */
    @SaCheckPermission("fee:baddebt:follow")
    @Log(title = "烂账关闭", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{id}")
    public R<Void> close(@PathVariable Long id) {
        return toAjax(badDebtService.closeBadDebt(id));
    }
}
