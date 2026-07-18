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
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.bo.ExpenseBillBo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;
import com.ruoyi.system.service.IExpenseApprovalService;
import com.ruoyi.system.service.IExpenseBillService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 报销单管理Controller
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/expense")
public class ExpenseBillController extends BaseController {

    private final IExpenseBillService expenseBillService;
    private final IExpenseApprovalService expenseApprovalService;

    /**
     * 查询报销单列表
     */
    @SaCheckPermission("fee:expense:list")
    @GetMapping("/list")
    public TableDataInfo<ExpenseBillVo> list(ExpenseBillBo bo, PageQuery pageQuery) {
        Page<ExpenseBillVo> page = expenseBillService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 查询待审批报销单列表（部门经理/上级查看，无需权限注解）
     */
    @GetMapping("/pendingApproval")
    public TableDataInfo<ExpenseBillVo> pendingApproval(PageQuery pageQuery) {
        Page<ExpenseBillVo> page = expenseApprovalService.selectPendingApprovalPage(pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 获取报销单详细信息（查看模式，无需权限）
     */
    @GetMapping("/{id}")
    public R<ExpenseBillVo> getInfo(@PathVariable Long id) {
        return R.ok(expenseBillService.queryById(id));
    }

    /**
     * 新增报销单（草稿，填写报销单用，无需权限）
     */
    @Log(title = "报销单", businessType = BusinessType.INSERT)
    @PostMapping
    public R<ExpenseBill> add(@Validated(AddGroup.class) @RequestBody ExpenseBillBo bo) {
        return R.ok(expenseBillService.insertByBo(bo));
    }

    /**
     * 提交报销单（填写报销单用，无需权限）
     */
    @Log(title = "报销单提交", businessType = BusinessType.UPDATE)
    @PostMapping("/submit/{id}")
    public R<ExpenseBill> submit(@PathVariable Long id) {
        return R.ok(expenseBillService.submitBill(id));
    }

    /**
     * 部门经理审批通过
     */
    @RepeatSubmit
    @Log(title = "报销单审批", businessType = BusinessType.UPDATE)
    @PostMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id, @RequestParam(required = false) String opinion) {
        return toAjax(expenseApprovalService.approve(id, opinion));
    }

    /**
     * 部门经理驳回
     */
    @RepeatSubmit
    @Log(title = "报销单审批", businessType = BusinessType.UPDATE)
    @PostMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam String reason,
                          @RequestParam(required = false) String opinion) {
        return toAjax(expenseApprovalService.reject(id, reason, opinion));
    }

    /**
     * 删除报销单
     */
    @SaCheckPermission("fee:expense:remove")
    @Log(title = "报销单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(expenseBillService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
