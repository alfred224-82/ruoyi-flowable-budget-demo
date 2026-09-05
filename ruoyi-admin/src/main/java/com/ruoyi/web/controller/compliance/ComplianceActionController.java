package com.ruoyi.web.controller.compliance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.AccountActionBo;
import com.ruoyi.system.domain.vo.SysAccountActionLogVo;
import com.ruoyi.system.service.IAccountActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户数据合规Controller（T14.4 数据导出/账号注销，菜单 perms compliance:action:*）
 * <p>申请为用户自助（登录态，7 日内完成）；查询/处理为平台超管职能。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/action")
public class ComplianceActionController extends BaseController {

    private final IAccountActionService accountActionService;

    /**
     * 申请分页列表
     */
    @SaCheckPermission("compliance:action:list")
    @GetMapping("/list")
    public TableDataInfo<SysAccountActionLogVo> list(@RequestParam(required = false) String actionType,
                                                     @RequestParam(required = false) String status,
                                                     PageQuery pageQuery) {
        return accountActionService.queryPageList(actionType, status, pageQuery);
    }

    /**
     * 申请详情
     */
    @SaCheckPermission("compliance:action:query")
    @GetMapping("/{id}")
    public R<SysAccountActionLogVo> getInfo(@PathVariable Long id) {
        return R.ok(accountActionService.queryById(id));
    }

    /**
     * 发起数据导出/账号注销申请（用户自助，7 日内完成）
     */
    @Log(title = "数据合规申请", businessType = BusinessType.INSERT)
    @PostMapping("/apply")
    public R<Void> apply(@Validated(AddGroup.class) @RequestBody AccountActionBo bo) {
        return toAjax(accountActionService.apply(bo));
    }

    /**
     * 处理申请（DONE 完成回填结果 / REJECTED 拒绝）
     */
    @SaCheckPermission("compliance:action:handle")
    @Log(title = "数据合规处理", businessType = BusinessType.UPDATE)
    @PutMapping("/handle")
    public R<Void> handle(@RequestBody AccountActionBo bo) {
        return toAjax(accountActionService.handle(bo));
    }
}
