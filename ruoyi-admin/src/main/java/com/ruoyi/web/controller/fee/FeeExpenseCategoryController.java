package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.FeeCategorySplitRatioVo;
import com.ruoyi.system.domain.vo.FeeExpenseCategoryVo;
import com.ruoyi.system.service.IFeeExpenseCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 费用大类主数据Controller（T5.13 / T1.5 / 设计文档 2.2 #3）
 * admin 维护大类主数据与默认拆分比例；映射B（预算科目→大类）系统内置不可在此变更
 * <p>★T1.5：整体受 FEAT_SUBJECT_3LAYER 管控，基础版不启用（高级版+ 可用）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/category")
@RequireFeature("FEAT_SUBJECT_3LAYER")
public class FeeExpenseCategoryController extends BaseController {

    private final IFeeExpenseCategoryService categoryService;

    /**
     * 费用大类列表
     */
    @SaCheckPermission("fee:category:list")
    @GetMapping("/list")
    public R<List<FeeExpenseCategoryVo>> list() {
        return R.ok(categoryService.queryList());
    }

    /**
     * 启用的费用大类（编制/报销下拉用，登录即可）
     */
    @GetMapping("/activeList")
    public R<List<FeeExpenseCategoryVo>> activeList() {
        return R.ok(categoryService.queryActiveList());
    }

    /**
     * 费用大类详情
     */
    @SaCheckPermission("fee:category:query")
    @GetMapping("/{id}")
    public R<FeeExpenseCategoryVo> getInfo(@PathVariable Long id) {
        return R.ok(categoryService.queryById(id));
    }

    /**
     * 新增/修改费用大类
     */
    @SaCheckPermission("fee:category:add")
    @Log(title = "费用大类", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> save(@RequestBody FeeExpenseCategoryVo vo) {
        return R.ok(categoryService.saveCategory(vo));
    }

    /**
     * 修改费用大类
     */
    @SaCheckPermission("fee:category:edit")
    @Log(title = "费用大类", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Long> edit(@RequestBody FeeExpenseCategoryVo vo) {
        return R.ok(categoryService.saveCategory(vo));
    }

    /**
     * 删除费用大类（内置不可删）
     */
    @SaCheckPermission("fee:category:remove")
    @Log(title = "费用大类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(categoryService.deleteCategory(Arrays.asList(ids)));
    }

    /**
     * 查询大类默认拆分比例
     */
    @SaCheckPermission("fee:category:list")
    @GetMapping("/ratio/{categoryId}")
    public R<List<FeeCategorySplitRatioVo>> listRatio(@PathVariable Long categoryId) {
        return R.ok(categoryService.listRatios(categoryId));
    }

    /**
     * 保存大类默认拆分比例（合计=100%，财务维护，支撑 T5.14 编制拆分）
     */
    @SaCheckPermission("fee:categoryRatio:edit")
    @Log(title = "大类拆分比例", businessType = BusinessType.UPDATE)
    @PostMapping("/ratio/{categoryId}")
    public R<Void> saveRatio(@PathVariable Long categoryId, @RequestBody List<FeeCategorySplitRatioVo> ratios) {
        categoryService.saveRatios(categoryId, ratios);
        return R.ok();
    }
}
