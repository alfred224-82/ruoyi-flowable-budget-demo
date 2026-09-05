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
import com.ruoyi.system.domain.bo.BudgetCategoryQuotaBo;
import com.ruoyi.system.service.IBudgetPreparationService;
import com.ruoyi.system.service.IBudgetPreparationDetailService;
import com.ruoyi.system.service.IBudgetValidationRuleService;
import com.ruoyi.system.service.ICategoryBudgetService;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.vo.BudgetCategoryQuotaItemVo;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 预算编制
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/preparation")
public class BudgetPreparationController extends BaseController {

    private final IBudgetPreparationService budgetPreparationService;
    private final IBudgetPreparationDetailService budgetPreparationDetailService;
    private final IBudgetValidationRuleService budgetValidationRuleService;
    private final ICategoryBudgetService categoryBudgetService;

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
    public R<Map<String, Object>> getPreviousMonthDetails(
            @RequestParam Long orgId,
            @RequestParam Integer budgetYear,
            @RequestParam Integer budgetMonth) {
        return R.ok(budgetPreparationService.getPreviousMonthApprovedDetails(orgId, budgetYear, budgetMonth));
    }

    /**
     * 查询大类预算额度（T5.14 ① 老板视角：启用大类 + 当期已录额度 + 单据状态）
     * T5.13 v1.8 增强：返回各大类使用率统计（已用/使用率/调整下限）
     *
     * @param budgetYear 预算年度
     * @param budgetMonth 预算月份（1-12）
     * @param departmentId 部门ID（年月+部门维度，可选，null=用户所属部门）
     */
    @SaCheckPermission("system:preparation:categoryQuota")
    @GetMapping("/categoryQuota")
    public R<Map<String, Object>> getCategoryQuota(
            @NotNull(message = "预算年度不能为空") @RequestParam Integer budgetYear,
            @NotNull(message = "预算月份不能为空") @RequestParam Integer budgetMonth,
            @RequestParam(required = false) Long departmentId) {
        Map<String, Object> result = budgetPreparationService.getCategoryQuotas(budgetYear, budgetMonth, departmentId);
        // T5.13: 追加各大类使用率统计（已用额度、使用率、调整下限）——按部门维度查询
        Long targetDeptId = departmentId != null ? departmentId : com.ruoyi.common.helper.LoginHelper.getDeptId();
        Map<String, java.util.Map<String, java.math.BigDecimal>> usageStats = categoryBudgetService.getCategoryUsageStats(targetDeptId, budgetYear, budgetMonth);
        result.put("usageStats", usageStats);
        return R.ok(result);
    }

    /**
     * 保存大类预算额度（老板设定粗粒度额度，只管大类不管科目）
     * T5.13 v1.8 增强：预算金额调整下限控制（已使用 X% 则新金额不得低于原金额 X%）
     * T5.14 v1.8 增强：保存后自动触发拆分初稿生成（预算科目由系统自动生成）
     */
    @SaCheckPermission("system:preparation:categoryQuotaSave")
    @Log(title = "大类预算额度", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PostMapping("/categoryQuota")
    public R<Map<String, Object>> saveCategoryQuota(@Validated @RequestBody BudgetCategoryQuotaBo bo) {
        // ===== T5.13: 预算金额调整下限控制 =====
        // 保存前获取旧的使用率统计，用于校验调减下限（按部门维度）
        Long targetDeptId = bo.getDepartmentId() != null ? bo.getDepartmentId() : com.ruoyi.common.helper.LoginHelper.getDeptId();
        Map<String, java.util.Map<String, java.math.BigDecimal>> oldUsageStats =
            categoryBudgetService.getCategoryUsageStats(targetDeptId, bo.getBudgetYear(), bo.getBudgetMonth());

        // 校验每个大类的调减幅度
        for (BudgetCategoryQuotaItemVo item : bo.getQuotas()) {
            java.util.Map<String, java.math.BigDecimal> stats = oldUsageStats.get(item.getCategoryCode());
            if (stats != null) {
                java.math.BigDecimal oldQuota = stats.get("quota");
                java.math.BigDecimal used = stats.get("used");
                java.math.BigDecimal newAmount = item.getAmount();
                // 只有当新金额 < 旧额度 且 有已使用金额时才触发下限控制
                if (newAmount.compareTo(oldQuota) < 0
                    && used.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    // 下限 = 已使用金额（即 quota × usageRate / 100）
                    if (newAmount.compareTo(used) < 0) {
                        java.math.BigDecimal usageRate = stats.get("usageRate");
                        throw new com.ruoyi.common.exception.ServiceException(
                            "费用大类[" + item.getCategoryName() + "]已使用 " + used
                            + "（使用率 " + usageRate + "%），调整后的新预算金额不得低于 " + used
                            + "（防止通过降低预算额度变相绕过已发生的超支）");
                    }
                }
            }
        }

        // 保存大类额度
        Long sheetId = budgetPreparationService.saveCategoryQuotas(bo);

        // ===== T5.14: 保存后自动触发拆分初稿（预算科目由系统自动生成） =====
        Map<String, Object> splitResult = null;
        try {
            splitResult = budgetPreparationService.generateSplitDraft(sheetId);
            log.info("[T5.14] 大类额度保存后自动拆分成功: sheetId={}, detailCount={}",
                sheetId, splitResult.get("detailCount"));
        } catch (Exception e) {
            // 拆分失败不阻断保存（可能尚无预算科目映射），仅记录日志
            log.warn("[T5.14] 自动拆分初稿失败（不影响额度保存）: sheetId={}, error={}", sheetId, e.getMessage());
        }

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("sheetId", sheetId);
        result.put("splitResult", splitResult);
        return R.ok(result);
    }

    /**
     * 生成拆分初稿（T5.14 ②：大类额度按比例拆分到预算科目）
     */
    @SaCheckPermission("system:preparation:splitDraft")
    @Log(title = "预算编制拆分初稿", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/splitDraft/{id}")
    public R<Map<String, Object>> splitDraft(@NotNull(message = "预算单ID不能为空") @PathVariable Long id) {
        return R.ok(budgetPreparationService.generateSplitDraft(id));
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
