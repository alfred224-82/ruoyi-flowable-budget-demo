package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysTenantMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serial;
import java.io.Serializable;

/**
 * 门店信息 Controller（T31.5，设计方案 §八之二 / §六 TMP.8 联系我们）
 * <p>门店级公开联系方式（L1 门店信息），复用 sys_tenant 表既有列：
 * <ul>
 *   <li>contact_phone：门店联系电话（一键拨号 tel:）</li>
 *   <li>contact_wechat：门店客服微信号（长按识别二维码）★T31.5 新增列</li>
 *   <li>address / city / company_name：门店地址与名称（已有）</li>
 * </ul>
 * <p>★权限：GET 登录即可（L1 公开信息）；PUT 需 micro:store:manage（老板/租户管理员）。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/store")
public class MicroStoreController extends BaseController {

    private final SysTenantMapper tenantMapper;

    /**
     * 查询门店联系方式（L1 门店信息，登录即可）
     * <p>供顾客端「联系我们」页调用；不返回 wecom_secret / mp_app_id 等敏感凭据。
     */
    @GetMapping("/contactInfo")
    public R<StoreContactVo> getContactInfo() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("门店不存在");
        }
        StoreContactVo vo = new StoreContactVo();
        vo.setCompanyName(tenant.getCompanyName());
        vo.setContactPhone(tenant.getContactPhone());
        vo.setContactWechat(tenant.getContactWechat());
        vo.setAddress(tenant.getAddress());
        vo.setCity(tenant.getCity());
        vo.setLogoUrl(tenant.getLogoUrl());
        return R.ok(vo);
    }

    /**
     * 更新门店联系方式（PC 端门店设置，micro:store:manage）
     * <p>★仅允许修改公开联系字段；不允许修改版本/状态/凭据等敏感字段。
     */
    @SaCheckPermission("micro:store:manage")
    @Log(title = "门店联系方式", businessType = BusinessType.UPDATE)
    @PutMapping("/contactInfo")
    public R<Void> updateContactInfo(@RequestBody StoreContactVo vo) {
        if (vo == null) {
            throw new ServiceException("参数不能为空");
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("门店不存在");
        }
        if (vo.getContactPhone() != null) {
            tenant.setContactPhone(StrUtil.trim(vo.getContactPhone()));
        }
        if (vo.getContactWechat() != null) {
            tenant.setContactWechat(StrUtil.trim(vo.getContactWechat()));
        }
        if (vo.getAddress() != null) {
            tenant.setAddress(vo.getAddress());
        }
        if (vo.getCity() != null) {
            tenant.setCity(vo.getCity());
        }
        tenantMapper.updateById(tenant);
        return R.ok();
    }

    /**
     * 门店联系方式 VO（L1 门店信息，公开可见，不含凭据）
     */
    @Data
    public static class StoreContactVo implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        /** 门店名称 */
        private String companyName;
        /** 门店联系电话（一键拨号） */
        private String contactPhone;
        /** 门店客服微信号（长按识别二维码）★T31.5 新增 */
        private String contactWechat;
        /** 门店地址 */
        private String address;
        /** 所在城市 */
        private String city;
        /** 门店 Logo URL */
        private String logoUrl;
    }
}
