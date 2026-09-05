package com.ruoyi.web.controller.compliance;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.domain.bo.AgreementVersionBo;
import com.ruoyi.system.domain.vo.SysAgreementVersionVo;
import com.ruoyi.system.domain.vo.SysUserAgreementLogVo;
import com.ruoyi.system.service.IAgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 隐私政策/用户协议Controller（T14.4，菜单 perms compliance:agreement:*）
 * <p>协议版本发布/查询为平台超管职能；获取生效协议(active)与记录用户同意(agree)为客户端登录态接口。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/compliance/agreement")
public class ComplianceAgreementController extends BaseController {

    private final IAgreementService agreementService;

    /**
     * 协议版本列表（可按类型过滤，按发布时间倒序）
     */
    @SaCheckPermission("compliance:agreement:list")
    @GetMapping("/list")
    public R<List<SysAgreementVersionVo>> list(@RequestParam(required = false) String agreementType) {
        return R.ok(agreementService.listVersions(agreementType));
    }

    /**
     * 当前各类型生效协议（客户端展示用，登录态即可）
     */
    @GetMapping("/active")
    public R<List<SysAgreementVersionVo>> active() {
        return R.ok(agreementService.listActive());
    }

    /**
     * 协议版本详情
     */
    @SaCheckPermission("compliance:agreement:query")
    @GetMapping("/{id}")
    public R<SysAgreementVersionVo> getInfo(@PathVariable Long id) {
        return R.ok(agreementService.queryById(id));
    }

    /**
     * 发布协议新版本（同类型旧版转历史）
     */
    @SaCheckPermission("compliance:agreement:add")
    @Log(title = "协议版本", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> publish(@Validated(AddGroup.class) @RequestBody AgreementVersionBo bo) {
        return toAjax(agreementService.publish(bo));
    }

    /**
     * 记录用户同意协议（留痕：用户/openid/类型/版本/IP/时间；客户端登录态）
     */
    @Log(title = "协议同意留痕", businessType = BusinessType.INSERT)
    @PostMapping("/agree")
    public R<Void> agree(@RequestParam String agreementType,
                         @RequestParam String version,
                         @RequestParam(required = false) String openid) {
        return toAjax(agreementService.recordAgree(agreementType, version, openid, ServletUtils.getClientIP()));
    }

    /**
     * 用户同意留痕分页查询
     */
    @SaCheckPermission("compliance:agreement:query")
    @GetMapping("/agreeLog")
    public TableDataInfo<SysUserAgreementLogVo> agreeLog(@RequestParam(required = false) String agreementType,
                                                         @RequestParam(required = false) Long userId,
                                                         PageQuery pageQuery) {
        return agreementService.queryAgreeLogPageList(agreementType, userId, pageQuery);
    }
}
