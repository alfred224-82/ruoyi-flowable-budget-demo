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
import com.ruoyi.system.domain.ServiceCategory;
import com.ruoyi.system.domain.bo.ServiceCategoryBo;
import com.ruoyi.system.domain.vo.ServiceCategoryVo;
import com.ruoyi.system.mapper.ServiceCategoryMapper;
import com.ruoyi.system.service.IServiceCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 服务分类服务实现（T15.4/T15.11，小微版）
 * <p>service_category 租户级表，多租户插件自动隔离；平台管理员/老板维护分类。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceCategoryServiceImpl implements IServiceCategoryService {

    private final ServiceCategoryMapper categoryMapper;

    /** ★V3.7 原子资源：分类类型——服务分类（默认） */
    public static final String CATEGORY_TYPE_SERVICE = "service";
    /** ★V3.7 原子资源：分类类型——房间·空间分类 */
    public static final String CATEGORY_TYPE_SPACE = "space";

    @Override
    public TableDataInfo<ServiceCategoryVo> queryPageList(ServiceCategoryBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ServiceCategory> wrapper = Wrappers.<ServiceCategory>lambdaQuery()
            .like(StrUtil.isNotBlank(bo.getName()), ServiceCategory::getName, bo.getName())
            .eq(StrUtil.isNotBlank(bo.getIndustryType()), ServiceCategory::getIndustryType, bo.getIndustryType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ServiceCategory::getStatus, bo.getStatus())
            .orderByAsc(ServiceCategory::getSortOrder)
            .orderByAsc(ServiceCategory::getId);
        Page<ServiceCategoryVo> page = categoryMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public ServiceCategoryVo queryById(Long id) {
        ServiceCategoryVo vo = categoryMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("服务分类不存在");
        }
        return vo;
    }

    @Override
    public List<ServiceCategoryVo> listEnabled() {
        return categoryMapper.selectVoList(Wrappers.<ServiceCategory>lambdaQuery()
            .eq(ServiceCategory::getStatus, "0")
            .orderByAsc(ServiceCategory::getSortOrder)
            .orderByAsc(ServiceCategory::getId));
    }

    @Override
    public List<ServiceCategoryVo> listEnabledByType(String categoryType) {
        return categoryMapper.selectVoList(Wrappers.<ServiceCategory>lambdaQuery()
            .eq(ServiceCategory::getStatus, "0")
            // ★V3.7 原子资源：category_type 空值（存量行）视为 service
            .and(StrUtil.isNotBlank(categoryType), w -> {
                if (CATEGORY_TYPE_SPACE.equals(categoryType)) {
                    w.eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SPACE);
                } else {
                    w.isNull(ServiceCategory::getCategoryType)
                        .or().eq(ServiceCategory::getCategoryType, "")
                        .or().eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SERVICE);
                }
            })
            .orderByAsc(ServiceCategory::getSortOrder)
            .orderByAsc(ServiceCategory::getId));
    }

    @Override
    public Long ensureSpaceCategory() {
        ServiceCategory exist = categoryMapper.selectOne(Wrappers.<ServiceCategory>lambdaQuery()
            .eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SPACE)
            .orderByAsc(ServiceCategory::getId)
            .last("LIMIT 1"));
        if (exist != null) {
            return exist.getId();
        }
        // 无空间分类 → 自动创建默认「房间/空间」分类（当前租户上下文，tenant_id 由插件/审计填充）
        ServiceCategory entity = new ServiceCategory();
        entity.setName("房间/空间");
        entity.setCategoryType(CATEGORY_TYPE_SPACE);
        entity.setSortOrder(0);
        entity.setStatus("0");
        entity.setRemark("系统自动创建（V3.7 原子资源·房间空间管理）");
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public Long insert(ServiceCategoryBo bo) {
        ServiceCategory entity = BeanCopyUtils.copy(bo, ServiceCategory.class);
        entity.setId(null);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        categoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(ServiceCategoryBo bo) {
        ServiceCategory exist = categoryMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("服务分类不存在");
        }
        ServiceCategory update = BeanCopyUtils.copy(bo, ServiceCategory.class);
        return categoryMapper.updateById(update);
    }

    @Override
    public int toggle(Long id, String status) {
        if (id == null) {
            throw new ServiceException("服务分类ID不能为空");
        }
        ServiceCategory update = new ServiceCategory();
        update.setId(id);
        update.setStatus(StrUtil.isBlank(status) ? "0" : status);
        return categoryMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return categoryMapper.deleteBatchIds(ids);
    }
}
