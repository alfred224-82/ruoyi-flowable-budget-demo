package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.ReservedBudget;
import com.ruoyi.system.domain.bo.ReservedBudgetBo;
import com.ruoyi.system.domain.vo.ReservedBudgetVo;
import com.ruoyi.system.service.IReservedBudgetService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 预留预算管理Controller
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/reservedBudget")
public class ReservedBudgetController extends BaseController {

    private final IReservedBudgetService reservedBudgetService;

    /**
     * 查询预留预算列表
     */
    @SaCheckPermission("fee:reservedBudget:list")
    @GetMapping("/list")
    public TableDataInfo<ReservedBudgetVo> list(ReservedBudgetBo bo, PageQuery pageQuery) {
        Page<ReservedBudgetVo> page = reservedBudgetService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 获取预留预算详细信息
     */
    @SaCheckPermission("fee:reservedBudget:query")
    @GetMapping("/{id}")
    public R<ReservedBudgetVo> getInfo(@PathVariable Long id) {
        return R.ok(reservedBudgetService.queryById(id));
    }

    /**
     * 新增预留预算
     */
    @SaCheckPermission("fee:reservedBudget:add")
    @Log(title = "预留预算", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReservedBudgetBo bo) {
        return toAjax(reservedBudgetService.insertByBo(bo));
    }

    /**
     * 修改预留预算
     */
    @SaCheckPermission("fee:reservedBudget:edit")
    @Log(title = "预留预算", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody ReservedBudgetBo bo) {
        return toAjax(reservedBudgetService.updateByBo(bo));
    }

    /**
     * 预留预算总览（本级）
     */
    @GetMapping("/overview")
    public R<ReservedBudget> overview(@RequestParam String budgetPeriod) {
        return R.ok(reservedBudgetService.getMyReservedBudget(budgetPeriod));
    }

    /**
     * 删除预留预算
     */
    @SaCheckPermission("fee:reservedBudget:remove")
    @Log(title = "预留预算", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(reservedBudgetService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
