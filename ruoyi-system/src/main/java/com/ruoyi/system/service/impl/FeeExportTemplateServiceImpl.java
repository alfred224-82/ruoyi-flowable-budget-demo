package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeExportTemplate;
import com.ruoyi.system.domain.vo.FeeExportTemplateVo;
import com.ruoyi.system.mapper.FeeExportTemplateMapper;
import com.ruoyi.system.service.IFeeExportTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 自定义凭证模板Service实现（T6.6 / FEAT_EXPORT_TEMPLATE_CUSTOM）
 * fee_export_template 在租户拦截豁免清单（内置模板 tenant_id=0 跨租户共享），
 * 服务层手工隔离：可见范围 = 内置(0) + 本租户；内置模板只读，自定义模板可维护
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeExportTemplateServiceImpl implements IFeeExportTemplateService {

    private final FeeExportTemplateMapper templateMapper;

    @Override
    public List<FeeExportTemplateVo> listTemplates(String templateName) {
        return templateMapper.selectVoList(Wrappers.<FeeExportTemplate>lambdaQuery()
            .in(FeeExportTemplate::getTenantId, visibleTenantIds())
            .like(StrUtil.isNotBlank(templateName), FeeExportTemplate::getTemplateName, templateName)
            .orderByAsc(FeeExportTemplate::getSortOrder)
            .orderByAsc(FeeExportTemplate::getId));
    }

    @Override
    public FeeExportTemplateVo getTemplate(Long id) {
        FeeExportTemplateVo vo = templateMapper.selectVoById(id);
        if (vo == null || !visibleTenantIds().contains(tenantIdOf(id))) {
            throw new ServiceException("模板不存在");
        }
        return vo;
    }

    @Override
    public Boolean insertTemplate(FeeExportTemplate template) {
        if (StrUtil.isBlank(template.getTemplateName())) {
            throw new ServiceException("模板名称不能为空");
        }
        template.setId(null);
        template.setIsBuiltin(0);
        template.setTenantId(currentTenantId());
        if (template.getSortOrder() == null) {
            template.setSortOrder(100);
        }
        checkCodeUnique(template.getTemplateCode(), null);
        return templateMapper.insert(template) > 0;
    }

    @Override
    public Boolean updateTemplate(FeeExportTemplate template) {
        FeeExportTemplate existing = loadOwnedTemplate(template.getId());
        checkCodeUnique(template.getTemplateCode(), existing.getId());
        // 租户与内置属性不可篡改
        template.setTenantId(existing.getTenantId());
        template.setIsBuiltin(existing.getIsBuiltin());
        return templateMapper.updateById(template) > 0;
    }

    @Override
    public Boolean deleteTemplate(Long id) {
        loadOwnedTemplate(id);
        return templateMapper.deleteById(id) > 0;
    }

    /**
     * 加载并校验归属：内置模板只读、跨租户不可操作
     */
    private FeeExportTemplate loadOwnedTemplate(Long id) {
        if (id == null) {
            throw new ServiceException("模板ID不能为空");
        }
        FeeExportTemplate template = templateMapper.selectById(id);
        if (template == null || !visibleTenantIds().contains(template.getTenantId())) {
            throw new ServiceException("模板不存在");
        }
        if (template.getIsBuiltin() != null && template.getIsBuiltin() == 1) {
            throw new ServiceException("内置模板不可修改，请复制为自定义模板后调整");
        }
        return template;
    }

    /**
     * 模板编码可见范围内唯一（内置+本租户）
     */
    private void checkCodeUnique(String templateCode, Long excludeId) {
        if (StrUtil.isBlank(templateCode)) {
            return;
        }
        Long count = templateMapper.selectCount(Wrappers.<FeeExportTemplate>lambdaQuery()
            .in(FeeExportTemplate::getTenantId, visibleTenantIds())
            .eq(FeeExportTemplate::getTemplateCode, templateCode)
            .ne(excludeId != null, FeeExportTemplate::getId, excludeId));
        if (count != null && count > 0) {
            throw new ServiceException("模板编码已存在：" + templateCode);
        }
    }

    private Long tenantIdOf(Long id) {
        FeeExportTemplate template = templateMapper.selectById(id);
        return template == null ? null : template.getTenantId();
    }

    /**
     * 可见租户：内置模板(0) + 当前租户
     */
    private List<Long> visibleTenantIds() {
        Long current = LoginHelper.getTenantId();
        return current == null ? Arrays.asList(0L) : Arrays.asList(0L, current);
    }

    private Long currentTenantId() {
        Long current = LoginHelper.getTenantId();
        return current == null ? 0L : current;
    }
}
