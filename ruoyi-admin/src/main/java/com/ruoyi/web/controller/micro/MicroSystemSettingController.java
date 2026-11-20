package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.common.helper.LoginHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;

/**
 * 租户管理员系统设置Controller（T18.11，小微版 M18）
 * <p>「系统设置」菜单（micro:systemSetting:*）：
 * <ul>
 *   <li>企业信息编辑（公司名称/行业/联系方式）</li>
 *   <li>安全设置（密码策略提示/登录限制提示）</li>
 * </ul>
 * <p>★租户管理员（platform_admin）专属。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/systemSetting")
public class MicroSystemSettingController extends BaseController {

    private final SysTenantMapper tenantMapper;

    /**
     * 获取当前租户系统设置
     */
    @SaCheckPermission("micro:systemSetting:list")
    @GetMapping
    public R<TenantSettingVo> getSetting() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        TenantSettingVo vo = new TenantSettingVo();
        vo.setCompanyName(tenant.getCompanyName());
        vo.setIndustry(tenant.getIndustry());
        vo.setContactName(tenant.getContactName());
        vo.setContactPhone(tenant.getContactPhone());
        vo.setAddress(tenant.getAddress());
        vo.setCity(tenant.getCity());
        return R.ok(vo);
    }

    /**
     * 保存企业信息
     */
    @SaCheckPermission("micro:systemSetting:edit")
    @Log(title = "系统设置", businessType = BusinessType.UPDATE)
    @PutMapping("/company")
    public R<Void> saveCompany(@RequestBody TenantSettingVo vo) {
        if (vo == null) {
            throw new ServiceException("参数不能为空");
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        // ★只允许修改企业信息字段，不允许修改版本/状态等敏感字段
        if (StrUtil.isNotBlank(vo.getCompanyName())) {
            tenant.setCompanyName(vo.getCompanyName());
        }
        if (vo.getIndustry() != null) {
            tenant.setIndustry(vo.getIndustry());
        }
        if (vo.getContactName() != null) {
            tenant.setContactName(vo.getContactName());
        }
        if (vo.getContactPhone() != null) {
            tenant.setContactPhone(vo.getContactPhone());
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

    @Data
    public static class TenantSettingVo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String companyName;
        private String industry;
        private String contactName;
        private String contactPhone;
        private String address;
        private String city;
    }
}
