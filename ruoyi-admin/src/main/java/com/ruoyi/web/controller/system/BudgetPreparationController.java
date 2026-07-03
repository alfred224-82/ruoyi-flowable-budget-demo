package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.Arrays;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.domain.vo.BudgetPreparationDetailVo;
import com.ruoyi.system.domain.vo.BudgetRejectHistoryVo;
import com.ruoyi.system.domain.vo.ValidationResultVo;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.bo.BudgetPreparationDetailBo;
import com.ruoyi.system.service.IBudgetPreparationService;
import com.ruoyi.system.service.IBudgetPreparationDetailService;
import com.ruoyi.system.service.IBudgetValidationRuleService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 预算编制
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/preparation")
public class BudgetPreparationController extends BaseController {

    private final IBudgetPreparationService budgetPreparationService;
    private final IBudgetPreparationDetailService budgetPreparationDetailService;
    private final IBudgetValidationRuleService budgetValidationRuleService;

    /**
     * 查询预算编制列表
     */
    @SaCheckPermission("system:preparation:list")
    @GetMapping("/list")
    public TableDataInfo<BudgetPreparationVo> list(BudgetPreparationBo bo, PageQuery pageQuery) {
        return budgetPreparationService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出预算编制列表
     */
    @SaCheckPermission("system:preparation:export")
    @Log(title = "预算编制", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(BudgetPreparationBo bo, HttpServletResponse response) {
        List<BudgetPreparationVo> list = budgetPreparationService.queryList(bo);
        ExcelUtil.exportExcel(list, "预算编制", BudgetPreparationVo.class, response);
    }

    /**
     * 获取预算编制详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/{id}")
    public R<BudgetPreparationVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(budgetPreparationService.queryById(id));
    }

    /**
     * 新增预算编制
     */
    @SaCheckPermission("system:preparation:add")
    @Log(title = "预算编制", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Long> add(@Validated(AddGroup.class) @RequestBody BudgetPreparationBo bo) {
        budgetPreparationService.insertByBo(bo);
        return R.ok(bo.getId());
    }

    /**
     * 修改预算编制
     */
    @SaCheckPermission("system:preparation:edit")
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody BudgetPreparationBo bo) {
        return toAjax(budgetPreparationService.updateByBo(bo));
    }

    /**
     * 删除预算编制
     *
     * @param ids 主键
     */
    @SaCheckPermission("system:preparation:remove")
    @Log(title = "预算编制", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(budgetPreparationService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 完成编制
     */
    @SaCheckPermission("system:preparation:submit")
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @PostMapping("/complete/{id}")
    public R<Void> complete(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(budgetPreparationService.completePreparation(id));
    }

    /**
     * 提交审核（单个）
     */
    @SaCheckPermission("system:preparation:submit")
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{id}")
    public R<Void> submit(@NotNull(message = "主键不能为空") @PathVariable Long id) {
        return toAjax(budgetPreparationService.submitForReview(id));
    }

    /**
     * 批量提交审核
     */
    @SaCheckPermission("system:preparation:submit")
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSubmit")
    public R<Void> batchSubmit(@RequestBody List<Long> ids) {
        return toAjax(budgetPreparationService.batchSubmitForReview(ids));
    }

    /**
     * 批量审批通过
     * 权限校验由 Service 层 checkCurrentUserCanApprove 按审批阶段+角色匹配
     */
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @PostMapping("/batchApprove")
    public R<Void> batchApprove(@RequestBody BatchApproveRequest request) {
        return toAjax(budgetPreparationService.batchApprove(request.getIds(), request.getRemark()));
    }

    /**
     * 审批通过（单个）
     * 权限校验由 Service 层 checkCurrentUserCanApprove 按审批阶段+角色匹配
     */
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{id}")
    public R<Void> approve(@NotNull(message = "主键不能为空") @PathVariable Long id,
                          @RequestParam(required = false) String remark) {
        return toAjax(budgetPreparationService.approve(id, remark));
    }

    /**
     * 审批驳回（单个）
     * 权限校验由 Service 层 checkCurrentUserCanApprove 按审批阶段+角色匹配
     */
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{id}")
    public R<Void> reject(@NotNull(message = "主键不能为空") @PathVariable Long id,
                         @RequestParam(required = false) String reason) {
        return toAjax(budgetPreparationService.reject(id, reason));
    }

    /**
     * 获取校验结果
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/validation/{sheetId}")
    public R<List<ValidationResultVo>> getValidationResults(@NotNull(message = "预算单ID不能为空") @PathVariable Long sheetId) {
        return R.ok(budgetPreparationService.getValidationResults(sheetId));
    }

    /**
     * 执行规则校验（前端传入明细数据，实时校验）
     */
    @PostMapping("/validation/execute")
    public R<List<ValidationResultVo>> executeValidation(@RequestBody List<Map<String, Object>> details) {
        return R.ok(budgetValidationRuleService.executeValidation(details));
    }

    /**
     * 打回至部门（分公司操作）
     */
    @SaCheckPermission("system:preparation:reject")
    @Log(title = "预算编制", businessType = BusinessType.UPDATE)
    @PostMapping("/sendBack/{id}")
    public R<Void> sendBack(@NotNull(message = "主键不能为空") @PathVariable Long id,
                            @NotNull @RequestParam Long deptId,
                            @RequestParam String reason) {
        return toAjax(budgetPreparationService.sendBackToDept(id, deptId, reason));
    }

    /**
     * 查询驳回历史
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/reject/history/{sheetId}")
    public R<List<BudgetRejectHistoryVo>> rejectHistory(@NotNull(message = "预算单ID不能为空") @PathVariable Long sheetId) {
        return R.ok(budgetPreparationService.queryRejectHistory(sheetId));
    }

    /**
     * 获取上月已审批通过的预算明细（用于初始化本月预算金额）
     * 实现「上月实绩驱动下月预算」
     *
     * @param orgId 部门ID
     * @param budgetYear 预算年度
     * @param budgetMonth 预算月份
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/previousMonthDetails")
    public R<List<BudgetPreparationDetailVo>> getPreviousMonthDetails(
            @RequestParam Long orgId,
            @RequestParam Integer budgetYear,
            @RequestParam Integer budgetMonth) {
        return R.ok(budgetPreparationService.getPreviousMonthApprovedDetails(orgId, budgetYear, budgetMonth));
    }

    /**
     * 查询预算编制明细列表
     *
     * @param sheetId 预算单ID
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/detail/list/{sheetId}")
    public R<List<BudgetPreparationDetailVo>> listDetail(@NotNull(message = "预算单ID不能为空") @PathVariable Long sheetId) {
        return R.ok(budgetPreparationDetailService.queryListBySheetId(sheetId));
    }

    /**
     * 保存预算编制明细
     */
    @SaCheckPermission("system:preparation:edit")
    @Log(title = "预算编制明细", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/detail")
    public R<Void> saveDetail(@Validated @RequestBody BudgetPreparationDetailBo bo) {
        if (bo.getId() != null) {
            return toAjax(budgetPreparationDetailService.updateByBo(bo));
        } else {
            return toAjax(budgetPreparationDetailService.insertByBo(bo));
        }
    }

    /**
     * 批量保存预算编制明细
     */
    @SaCheckPermission("system:preparation:edit")
    @Log(title = "预算编制明细", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/detail/batch")
    public R<Void> batchSaveDetail(@Validated @RequestBody List<BudgetPreparationDetailBo> detailList) {
        return toAjax(budgetPreparationDetailService.batchSave(detailList));
    }

    /**
     * 批量审批请求对象
     */
    public static class BatchApproveRequest {
        private List<Long> ids;
        private String remark;

        public List<Long> getIds() {
            return ids;
        }

        public void setIds(List<Long> ids) {
            this.ids = ids;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
