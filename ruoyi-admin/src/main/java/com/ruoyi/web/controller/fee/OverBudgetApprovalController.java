package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.OverBudgetApprovalBo;
import com.ruoyi.system.domain.vo.BudgetAdjustmentLogVo;
import com.ruoyi.system.domain.vo.OverBudgetApprovalVo;
import com.ruoyi.system.service.IOverBudgetApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超预算审批Controller
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/approval")
public class OverBudgetApprovalController extends BaseController {

    private final IOverBudgetApprovalService approvalService;

    /**
     * 查询超预算审批列表
     */
    @SaCheckPermission("fee:approval:list")
    @GetMapping("/list")
    public TableDataInfo<OverBudgetApprovalVo> list(OverBudgetApprovalBo bo, PageQuery pageQuery) {
        Page<OverBudgetApprovalVo> page = approvalService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 获取超预算审批详细信息
     */
    @SaCheckPermission("fee:approval:query")
    @GetMapping("/{id}")
    public R<OverBudgetApprovalVo> getInfo(@PathVariable Long id) {
        return R.ok(approvalService.queryById(id));
    }

    /**
     * 处理超预算审批
     */
    @SaCheckPermission("fee:approval:process")
    @Log(title = "超预算审批", businessType = BusinessType.UPDATE)
    @PostMapping("/process")
    public R<Void> process(@Validated @RequestBody OverBudgetApprovalBo bo) {
        return toAjax(approvalService.processApproval(
            bo.getId(), bo.getAction(), bo.getAdjustFromSubject(),
            bo.getAdjustAmount(), bo.getOpinion()));
    }

    /**
     * 查询调剂流水
     */
    @SaCheckPermission("fee:approval:query")
    @GetMapping("/adjustmentLog/{approvalId}")
    public R<List<BudgetAdjustmentLogVo>> adjustmentLog(@PathVariable Long approvalId) {
        return R.ok(approvalService.queryAdjustmentLog(approvalId));
    }
}
