package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.RoleTemplate;
import com.ruoyi.system.domain.bo.RoleTemplateBo;
import com.ruoyi.system.domain.vo.RoleTemplateVo;
import com.ruoyi.system.mapper.RoleTemplateMapper;
import com.ruoyi.system.service.IRoleTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 角色模板服务实现（T15.7，admin 维护系统级共享配方）
 * <p>role_template 系统级共享表（无 tenant_id、无 del_flag），已在 IGNORE_TENANT_TABLES，不做租户隔离；
 * 删除为物理删除。version_type × role_code 唯一（uk_version_role），新建/编辑均保护身份字段唯一性。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleTemplateServiceImpl implements IRoleTemplateService {

    private final RoleTemplateMapper templateMapper;

    @Override
    public TableDataInfo<RoleTemplateVo> queryPageList(RoleTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<RoleTemplate> wrapper = Wrappers.<RoleTemplate>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getVersionType()), RoleTemplate::getVersionType, bo.getVersionType())
            .eq(StrUtil.isNotBlank(bo.getRoleCode()), RoleTemplate::getRoleCode, bo.getRoleCode())
            .eq(StrUtil.isNotBlank(bo.getStatus()), RoleTemplate::getStatus, bo.getStatus())
            .orderByAsc(RoleTemplate::getVersionType)
            .orderByAsc(RoleTemplate::getRoleCode);
        Page<RoleTemplateVo> page = templateMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public RoleTemplateVo queryById(Long id) {
        RoleTemplateVo vo = templateMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("角色模板不存在");
        }
        return vo;
    }

    @Override
    public List<RoleTemplateVo> listByVersion(String versionType) {
        return templateMapper.selectVoList(Wrappers.<RoleTemplate>lambdaQuery()
            .eq(StrUtil.isNotBlank(versionType), RoleTemplate::getVersionType, versionType)
            .orderByAsc(RoleTemplate::getRoleCode));
    }

    @Override
    public RoleTemplateVo getByVersionAndRole(String versionType, String roleCode) {
        if (StrUtil.isBlank(versionType) || StrUtil.isBlank(roleCode)) {
            return null;
        }
        return templateMapper.selectVoOne(Wrappers.<RoleTemplate>lambdaQuery()
            .eq(RoleTemplate::getVersionType, versionType)
            .eq(RoleTemplate::getRoleCode, roleCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(RoleTemplateBo bo) {
        // 唯一性校验：version_type + role_code（uk_version_role）
        RoleTemplate exist = templateMapper.selectOne(Wrappers.<RoleTemplate>lambdaQuery()
            .eq(RoleTemplate::getVersionType, bo.getVersionType())
            .eq(RoleTemplate::getRoleCode, bo.getRoleCode()));
        if (exist != null) {
            throw new ServiceException("该版本下角色模板已存在：" + bo.getVersionType() + " / " + bo.getRoleCode());
        }
        RoleTemplate entity = BeanCopyUtils.copy(bo, RoleTemplate.class);
        entity.setId(null);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(RoleTemplateBo bo) {
        RoleTemplate exist = templateMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("角色模板不存在");
        }
        RoleTemplate update = BeanCopyUtils.copy(bo, RoleTemplate.class);
        // 身份字段（version_type/role_code）不允许通过编辑篡改，保证配方唯一键稳定
        update.setVersionType(null);
        update.setRoleCode(null);
        return templateMapper.updateById(update);
    }

    @Override
    public int bindMenu(RoleTemplateBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("角色模板ID不能为空");
        }
        RoleTemplate exist = templateMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("角色模板不存在");
        }
        RoleTemplate update = new RoleTemplate();
        update.setId(bo.getId());
        update.setMenuKeys(bo.getMenuKeys());
        update.setBlockKeys(bo.getBlockKeys());
        return templateMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        // role_template 无 del_flag，物理删除
        return templateMapper.deleteBatchIds(ids);
    }
}
