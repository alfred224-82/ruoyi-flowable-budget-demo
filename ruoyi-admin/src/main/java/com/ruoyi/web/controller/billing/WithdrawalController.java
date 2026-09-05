package com.ruoyi.web.controller.billing;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.bo.WithdrawalApplyBo;
import com.ruoyi.system.domain.bo.WithdrawalAuditBo;
import com.ruoyi.system.domain.vo.SysVirtualAccountVo;
import com.ruoyi.system.domain.vo.SysWithdrawalRequestVo;
import com.ruoyi.system.service.IVirtualAccountService;
import com.ruoyi.system.service.IWithdrawalService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 提现与虚拟账户Controller（T14.3，菜单 perms billing:withdrawal:*）
 * <p>提现申请为租户自助（登录即可，操作本租户虚拟账户）；查询/审核为平台超管职能。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/billing/withdrawal")
public class WithdrawalController extends BaseController {

    private final IWithdrawalService withdrawalService;
    private final IVirtualAccountService virtualAccountService;

    /**
     * 查询虚拟账户（tenantId 为空则取当前登录租户）
     */
    @SaCheckPermission("billing:withdrawal:list")
    @GetMapping("/account")
    public R<SysVirtualAccountVo> account(@RequestParam(required = false) Long tenantId) {
        Long tid = tenantId != null ? tenantId : LoginHelper.getTenantId();
        return R.ok(virtualAccountService.getOrCreate(tid));
    }

    /**
     * 提现申请分页列表
     */
    @SaCheckPermission("billing:withdrawal:list")
    @GetMapping("/list")
    public TableDataInfo<SysWithdrawalRequestVo> list(@RequestParam(required = false) String status,
                                                      @RequestParam(required = false) Long tenantId,
                                                      PageQuery pageQuery) {
        return withdrawalService.queryPageList(status, tenantId, pageQuery);
    }

    /**
     * 提现申请详情
     */
    @SaCheckPermission("billing:withdrawal:query")
    @GetMapping("/{id}")
    public R<SysWithdrawalRequestVo> getInfo(@PathVariable Long id) {
        return R.ok(withdrawalService.queryById(id));
    }

    /**
     * 发起提现申请（租户自助，冻结本租户可用余额）
     */
    @Log(title = "提现申请", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    public R<Void> apply(@Validated(AddGroup.class) @RequestBody WithdrawalApplyBo bo) {
        return toAjax(withdrawalService.apply(bo));
    }

    /**
     * 提现人工审核（通过扣减冻结余额，驳回解冻）
     */
    @SaCheckPermission("billing:withdrawal:audit")
    @Log(title = "提现审核", businessType = BusinessType.UPDATE)
    @PutMapping("/audit")
    public R<Void> audit(@Validated(EditGroup.class) @RequestBody WithdrawalAuditBo bo) {
        return toAjax(withdrawalService.audit(bo));
    }
}
