package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysLicenseTemplate;
import com.ruoyi.system.mapper.SysLicenseTemplateMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.Set;

/**
 * License版本模板管理Controller（T18.14，小微版 M18）
 * <p>「License模板管理」菜单（system/licenseTemplate）：
 * <ul>
 *   <li>创建/编辑版本模板（BASIC/ADVANCED/PROFESSIONAL × TRIAL/FORMAL）</li>
 *   <li>模板启用/停用</li>
 * </ul>
 * <p>★Admin 专属，平台级操作。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/licenseTemplate")
public class SysLicenseTemplateController extends BaseController {

    private final SysLicenseTemplateMapper templateMapper;

    /** ★合法版本等级白名单 */
    private static final Set<String> VALID_EDITIONS = Set.of("BASIC", "ADVANCED", "PROFESSIONAL");
    /** ★合法授权类型白名单 */
    private static final Set<String> VALID_LICENSE_TYPES = Set.of("TRIAL", "FORMAL");

    /**
     * 模板列表（分页）
     */
    @SaCheckPermission("system:licenseTemplate:list")
    @GetMapping("/list")
    public TableDataInfo<SysLicenseTemplate> list(SysLicenseTemplate query, PageQuery pageQuery) {
        var wrapper = Wrappers.<SysLicenseTemplate>lambdaQuery()
            .eq(StrUtil.isNotBlank(query.getEdition()), SysLicenseTemplate::getEdition, query.getEdition())
            .eq(StrUtil.isNotBlank(query.getLicenseType()), SysLicenseTemplate::getLicenseType, query.getLicenseType())
            .eq(StrUtil.isNotBlank(query.getStatus()), SysLicenseTemplate::getStatus, query.getStatus())
            .like(StrUtil.isNotBlank(query.getTemplateName()), SysLicenseTemplate::getTemplateName, query.getTemplateName())
            .orderByDesc(SysLicenseTemplate::getCreateTime);
        var page = templateMapper.selectPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 模板详情
     */
    @SaCheckPermission("system:licenseTemplate:query")
    @GetMapping("/{id}")
    public R<SysLicenseTemplate> detail(@PathVariable Long id) {
        if (id == null) {
            throw new ServiceException("模板ID不能为空");
        }
        SysLicenseTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }
        return R.ok(template);
    }

    /**
     * 新增模板
     */
    @SaCheckPermission("system:licenseTemplate:add")
    @Log(title = "License模板", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody LicenseTemplateBo bo) {
        validateBo(bo);
        // ★唯一性校验
        Long existCount = templateMapper.selectCount(
            Wrappers.<SysLicenseTemplate>lambdaQuery()
                .eq(SysLicenseTemplate::getEdition, bo.getEdition())
                .eq(SysLicenseTemplate::getLicenseType, bo.getLicenseType()));
        if (existCount != null && existCount > 0) {
            throw new ServiceException("该版本+授权类型的模板已存在：" + bo.getEdition() + "/" + bo.getLicenseType());
        }
        SysLicenseTemplate entity = new SysLicenseTemplate();
        copyBoToEntity(bo, entity);
        templateMapper.insert(entity);
        log.info("[License模板] 新增模板：{}/{}", bo.getEdition(), bo.getLicenseType());
        return R.ok();
    }

    /**
     * 修改模板
     */
    @SaCheckPermission("system:licenseTemplate:edit")
    @Log(title = "License模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody LicenseTemplateBo bo) {
        if (bo == null || bo.getId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        SysLicenseTemplate exist = templateMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("模板不存在");
        }
        validateBo(bo);
        copyBoToEntity(bo, exist);
        templateMapper.updateById(exist);
        log.info("[License模板] 更新模板ID={}", bo.getId());
        return R.ok();
    }

    /**
     * 删除模板
     */
    @SaCheckPermission("system:licenseTemplate:remove")
    @Log(title = "License模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        if (ids == null || ids.length == 0) {
            throw new ServiceException("删除ID不能为空");
        }
        for (Long id : ids) {
            templateMapper.deleteById(id);
        }
        log.info("[License模板] 删除模板IDs={}", (Object) ids);
        return R.ok();
    }

    /**
     * 切换模板状态（启用/停用）
     */
    @SaCheckPermission("system:licenseTemplate:edit")
    @Log(title = "License模板", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestParam Long id, @RequestParam String status) {
        if (id == null) {
            throw new ServiceException("模板ID不能为空");
        }
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new ServiceException("状态值不合法：0=启用/1=停用");
        }
        SysLicenseTemplate template = templateMapper.selectById(id);
        if (template == null) {
            throw new ServiceException("模板不存在");
        }
        template.setStatus(status);
        templateMapper.updateById(template);
        return R.ok();
    }

    // ===== 私有方法 =====

    private void validateBo(LicenseTemplateBo bo) {
        if (bo == null) {
            throw new ServiceException("参数不能为空");
        }
        if (StrUtil.isBlank(bo.getTemplateName())) {
            throw new ServiceException("模板名称不能为空");
        }
        if (StrUtil.isBlank(bo.getEdition()) || !VALID_EDITIONS.contains(bo.getEdition())) {
            throw new ServiceException("版本等级不合法，可选：BASIC/ADVANCED/PROFESSIONAL");
        }
        if (StrUtil.isBlank(bo.getLicenseType()) || !VALID_LICENSE_TYPES.contains(bo.getLicenseType())) {
            throw new ServiceException("授权类型不合法，可选：TRIAL/FORMAL");
        }
        if (bo.getMaxUsers() == null || bo.getMaxUsers() < 1) {
            throw new ServiceException("最大用户数必须>=1");
        }
        if ("TRIAL".equals(bo.getLicenseType()) && bo.getTrialDays() != null && bo.getTrialDays() < 1) {
            throw new ServiceException("试用天数必须>=1");
        }
    }

    private void copyBoToEntity(LicenseTemplateBo bo, SysLicenseTemplate entity) {
        entity.setTemplateName(bo.getTemplateName());
        entity.setEdition(bo.getEdition());
        entity.setLicenseType(bo.getLicenseType());
        entity.setTrialDays(bo.getTrialDays());
        entity.setIndustries(bo.getIndustries());
        entity.setMaxUsers(bo.getMaxUsers());
        entity.setMaxRoles(bo.getMaxRoles());
        entity.setFeatures(bo.getFeatures());
        entity.setStatus(StrUtil.blankToDefault(bo.getStatus(), "0"));
        entity.setRemark(bo.getRemark());
    }

    // ===== BO =====

    @Data
    public static class LicenseTemplateBo implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        private String templateName;
        private String edition;
        private String licenseType;
        private Integer trialDays;
        private String industries;
        private Integer maxUsers;
        private Integer maxRoles;
        private String features;
        private String status;
        private String remark;
    }
}
