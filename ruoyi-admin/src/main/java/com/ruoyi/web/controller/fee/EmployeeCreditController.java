package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeCreditRule;
import com.ruoyi.system.domain.FeeEmployeeCredit;
import com.ruoyi.system.service.IEmployeeCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 员工信用体系Controller（T6.3 / FEAT_CREDIT_SCORE，高级版增量）
 * 三等级差异化：优秀免老板审批+14天期限 / 正常标准流程 / 受限禁用无发票报销
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/credit")
public class EmployeeCreditController extends BaseController {

    private final IEmployeeCreditService creditService;

    /**
     * 员工信用分分页查询
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @SaCheckPermission("fee:credit:list")
    @GetMapping("/list")
    public TableDataInfo<FeeEmployeeCredit> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String creditLevel,
            PageQuery pageQuery) {
        Page<FeeEmployeeCredit> page = creditService.pageCredits(userId, creditLevel, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 查询指定员工信用（不存在自动初始化）
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @SaCheckPermission("fee:credit:query")
    @GetMapping("/user/{userId}")
    public R<FeeEmployeeCredit> getByUser(@PathVariable Long userId) {
        return R.ok(creditService.getOrInit(userId));
    }

    /**
     * 我的信用分（小程序"我的"页/员工自查，无需管理权限）
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @GetMapping("/my")
    public R<FeeEmployeeCredit> myCredit() {
        return R.ok(creditService.getOrInit(LoginHelper.getUserId()));
    }

    /**
     * 财务手工调整信用分
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @SaCheckPermission("fee:credit:edit")
    @Log(title = "信用分调整", businessType = BusinessType.UPDATE)
    @PostMapping("/adjust")
    public R<Void> adjust(@RequestParam Long userId,
                          @RequestParam int delta,
                          @RequestParam String reason) {
        return toAjax(creditService.adjustScore(userId, delta, reason));
    }

    /**
     * 信用规则配置列表（内置模板 + 租户级覆盖）
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @SaCheckPermission("fee:creditRule:list")
    @GetMapping("/rule/list")
    public R<List<FeeCreditRule>> ruleList() {
        return R.ok(creditService.listRules());
    }

    /**
     * 新增租户级信用规则
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @SaCheckPermission("fee:creditRule:edit")
    @Log(title = "信用规则", businessType = BusinessType.INSERT)
    @PostMapping("/rule")
    public R<Void> addRule(@Validated @RequestBody FeeCreditRule rule) {
        return toAjax(creditService.saveRule(rule));
    }

    /**
     * 修改信用规则（仅限本租户行）
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @SaCheckPermission("fee:creditRule:edit")
    @Log(title = "信用规则", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public R<Void> editRule(@Validated @RequestBody FeeCreditRule rule) {
        return toAjax(creditService.updateRule(rule));
    }

    /**
     * 删除信用规则（仅限本租户行）
     */
    @RequireFeature("FEAT_CREDIT_SCORE")
    @SaCheckPermission("fee:creditRule:edit")
    @Log(title = "信用规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/rule/{id}")
    public R<Void> removeRule(@PathVariable Long id) {
        return toAjax(creditService.deleteRule(id));
    }
}
