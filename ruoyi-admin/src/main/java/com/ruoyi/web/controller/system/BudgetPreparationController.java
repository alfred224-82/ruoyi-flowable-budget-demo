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
 * 棰勭畻缂栧埗
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
     * 鏌ヨ棰勭畻缂栧埗鍒楄〃
     */
    @SaCheckPermission("system:preparation:list")
    @GetMapping("/list")
    public TableDataInfo<BudgetPreparationVo> list(BudgetPreparationBo bo, PageQuery pageQuery) {
        return budgetPreparationService.queryPageList(bo, pageQuery);
    }

    /**
     * 瀵煎嚭棰勭畻缂栧埗鍒楄〃
     */
    @SaCheckPermission("system:preparation:export")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(BudgetPreparationBo bo, HttpServletResponse response) {
        List<BudgetPreparationVo> list = budgetPreparationService.queryList(bo);
        ExcelUtil.exportExcel(list, "棰勭畻缂栧埗", BudgetPreparationVo.class, response);
    }

    /**
     * 鑾峰彇棰勭畻缂栧埗璇︾粏淇℃伅
     *
     * @param id 涓婚敭
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/{id}")
    public R<BudgetPreparationVo> getInfo(@NotNull(message = "涓婚敭涓嶈兘涓虹┖")
                                     @PathVariable Long id) {
        return R.ok(budgetPreparationService.queryById(id));
    }

    /**
     * 鏂板棰勭畻缂栧埗
     */
    @SaCheckPermission("system:preparation:add")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody BudgetPreparationBo bo) {
        return toAjax(budgetPreparationService.insertByBo(bo));
    }

    /**
     * 淇敼棰勭畻缂栧埗
     */
    @SaCheckPermission("system:preparation:edit")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody BudgetPreparationBo bo) {
        return toAjax(budgetPreparationService.updateByBo(bo));
    }

    /**
     * 鍒犻櫎棰勭畻缂栧埗
     *
     * @param ids 涓婚敭涓?     */
    @SaCheckPermission("system:preparation:remove")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "涓婚敭涓嶈兘涓虹┖")
                          @PathVariable Long[] ids) {
        return toAjax(budgetPreparationService.deleteWithValidByIds(Arrays.asList(ids), true));
    }

    /**
     * 鎻愪氦瀹℃牳锛堝崟涓級
     */
    @SaCheckPermission("system:preparation:submit")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{id}")
    public R<Void> submit(@NotNull(message = "涓婚敭涓嶈兘涓虹┖") @PathVariable Long id) {
        return toAjax(budgetPreparationService.submitForReview(id));
    }

    /**
     * 鎵归噺鎻愪氦瀹℃牳
     */
    @SaCheckPermission("system:preparation:submit")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.UPDATE)
    @PostMapping("/batchSubmit")
    public R<Void> batchSubmit(@RequestBody List<Long> ids) {
        return toAjax(budgetPreparationService.batchSubmitForReview(ids));
    }

    /**
     * 鎵归噺瀹℃壒閫氳繃
     */
    @SaCheckPermission("system:preparation:approve")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.UPDATE)
    @PostMapping("/batchApprove")
    public R<Void> batchApprove(@RequestBody BatchApproveRequest request) {
        return toAjax(budgetPreparationService.batchApprove(request.getIds(), request.getRemark()));
    }

    /**
     * 瀹℃壒閫氳繃锛堝崟涓級
     */
    @SaCheckPermission("system:preparation:approve")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{id}")
    public R<Void> approve(@NotNull(message = "涓婚敭涓嶈兘涓虹┖") @PathVariable Long id,
                          @RequestParam(required = false) String remark) {
        return toAjax(budgetPreparationService.approve(id, remark));
    }

    /**
     * 瀹℃壒椹冲洖锛堝崟涓級
     */
    @SaCheckPermission("system:preparation:reject")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{id}")
    public R<Void> reject(@NotNull(message = "涓婚敭涓嶈兘涓虹┖") @PathVariable Long id,
                         @RequestParam(required = false) String reason) {
        return toAjax(budgetPreparationService.reject(id, reason));
    }

    /**
     * 鑾峰彇鏍￠獙缁撴灉
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/validation/{sheetId}")
    public R<List<ValidationResultVo>> getValidationResults(@NotNull(message = "棰勭畻鍗旾D涓嶈兘涓虹┖") @PathVariable Long sheetId) {
        return R.ok(budgetPreparationService.getValidationResults(sheetId));
    }

    /**
     * 鎵ц瑙勫垯鏍￠獙锛堝墠绔紶鍏ユ槑缁嗘暟鎹紝瀹炴椂鏍￠獙锛?     */
    @PostMapping("/validation/execute")
    public R<List<ValidationResultVo>> executeValidation(@RequestBody List<Map<String, Object>> details) {
        return R.ok(budgetValidationRuleService.executeValidation(details));
    }

    /**
     * 鎵撳洖鑷抽儴闂紙鍒嗗叕鍙告搷浣滐級
     */
    @SaCheckPermission("system:preparation:reject")
    @Log(title = "棰勭畻缂栧埗", businessType = BusinessType.UPDATE)
    @PostMapping("/sendBack/{id}")
    public R<Void> sendBack(@NotNull(message = "涓婚敭涓嶈兘涓虹┖") @PathVariable Long id,
                            @NotNull @RequestParam Long deptId,
                            @RequestParam String reason) {
        return toAjax(budgetPreparationService.sendBackToDept(id, deptId, reason));
    }

    /**
     * 鏌ヨ椹冲洖鍘嗗彶
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/reject/history/{sheetId}")
    public R<List<BudgetRejectHistoryVo>> rejectHistory(@NotNull(message = "棰勭畻鍗旾D涓嶈兘涓虹┖") @PathVariable Long sheetId) {
        return R.ok(budgetPreparationService.queryRejectHistory(sheetId));
    }

    /**
     * 鏌ヨ棰勭畻缂栧埗鏄庣粏鍒楄〃
     *
     * @param sheetId 棰勭畻鍗旾D
     */
    @SaCheckPermission("system:preparation:query")
    @GetMapping("/detail/list/{sheetId}")
    public R<List<BudgetPreparationDetailVo>> listDetail(@NotNull(message = "棰勭畻鍗旾D涓嶈兘涓虹┖") @PathVariable Long sheetId) {
        return R.ok(budgetPreparationDetailService.queryListBySheetId(sheetId));
    }

    /**
     * 淇濆瓨棰勭畻缂栧埗鏄庣粏
     */
    @SaCheckPermission("system:preparation:edit")
    @Log(title = "棰勭畻缂栧埗鏄庣粏", businessType = BusinessType.INSERT)
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
     * 鎵归噺淇濆瓨棰勭畻缂栧埗鏄庣粏
     */
    @SaCheckPermission("system:preparation:edit")
    @Log(title = "棰勭畻缂栧埗鏄庣粏", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/detail/batch")
    public R<Void> batchSaveDetail(@Validated @RequestBody List<BudgetPreparationDetailBo> detailList) {
        return toAjax(budgetPreparationDetailService.batchSave(detailList));
    }

    /**
     * 鎵ц瑙勫垯鏍￠獙锛堝疄鏃讹級

    /**
     * 鎵归噺瀹℃壒璇锋眰瀵硅薄
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
