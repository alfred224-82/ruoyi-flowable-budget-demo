package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.PriceChangeLog;
import com.ruoyi.system.domain.ServiceCategory;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.bo.ServiceItemBo;
import com.ruoyi.system.domain.enums.PricingType;
import com.ruoyi.system.domain.vo.ServiceItemVo;
import com.ruoyi.system.mapper.PriceChangeLogMapper;
import com.ruoyi.system.mapper.ServiceCategoryMapper;
import com.ruoyi.system.mapper.ServiceItemMapper;
import com.ruoyi.system.service.IServiceItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 服务项目服务实现（T15.4/T15.11，小微版）
 * <p>service_item 租户级表，多租户插件自动隔离。
 * <p>★改价留痕（T15.11）：{@link #update} 检测到 price 变更、或 {@link #changePrice}（老板小程序「改价确认」）
 * 均通过 {@link #recordPriceChange} 自动写入 price_change_log（old/new 单价 + 操作人 + 改价时间）；
 * price_change_log.tenant_id 由 insertFill 从登录上下文自动填充，change_time 显式设置（plain 实体不走审计填充）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceItemServiceImpl implements IServiceItemService {

    private final ServiceItemMapper itemMapper;
    private final ServiceCategoryMapper categoryMapper;
    private final PriceChangeLogMapper priceChangeLogMapper;

    @Override
    public TableDataInfo<ServiceItemVo> queryPageList(ServiceItemBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .eq(bo.getCategoryId() != null, ServiceItem::getCategoryId, bo.getCategoryId())
            .like(StrUtil.isNotBlank(bo.getName()), ServiceItem::getName, bo.getName())
            .eq(StrUtil.isNotBlank(bo.getPricingType()), ServiceItem::getPricingType, bo.getPricingType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ServiceItem::getStatus, bo.getStatus())
            .orderByAsc(ServiceItem::getSortOrder)
            .orderByAsc(ServiceItem::getId);
        Page<ServiceItemVo> page = itemMapper.selectVoPage(pageQuery.build(), wrapper);
        fillCategoryName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<ServiceItemVo> queryPageListByCategoryType(ServiceItemBo bo, PageQuery pageQuery, boolean spaceOnly) {
        // ★V3.7 原子资源：空间分类（category_type='space'）ID 集，项目页排除/房间页仅含
        List<Long> spaceCategoryIds = categoryMapper.selectList(Wrappers.<ServiceCategory>lambdaQuery()
                .eq(ServiceCategory::getCategoryType, ServiceCategoryServiceImpl.CATEGORY_TYPE_SPACE))
            .stream().map(ServiceCategory::getId).collect(Collectors.toList());
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .eq(bo.getCategoryId() != null, ServiceItem::getCategoryId, bo.getCategoryId())
            .like(StrUtil.isNotBlank(bo.getName()), ServiceItem::getName, bo.getName())
            .eq(StrUtil.isNotBlank(bo.getPricingType()), ServiceItem::getPricingType, bo.getPricingType())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ServiceItem::getStatus, bo.getStatus());
        if (spaceOnly) {
            // 房间/空间页：无空间分类时防御性 IN(-1) 返回空集
            wrapper.in(ServiceItem::getCategoryId,
                CollUtil.isEmpty(spaceCategoryIds) ? Collections.singletonList(-1L) : spaceCategoryIds);
            // ★重复房间修复：同租户+同空间分类下同名房间仅保留 min(id)（SQL 级去重防御）——
            //   阻断历史并发自愈残留的重复数据进入房间资源管理/房间布局配置页，分页 count 与数据同口径
            wrapper.inSql(ServiceItem::getId,
                "SELECT si.id FROM service_item si WHERE si.del_flag = '0' AND NOT EXISTS ("
                    + "SELECT 1 FROM service_item dup WHERE dup.del_flag = '0' AND dup.tenant_id = si.tenant_id "
                    + "AND dup.category_id = si.category_id AND dup.name = si.name AND dup.id < si.id)");
        } else if (CollUtil.isNotEmpty(spaceCategoryIds)) {
            // 项目页：排除空间分类项（未分类 category_id IS NULL 或哨兵值 0 保留）
            // ★v1.40 兼容：categoryId=0 为项目哨兵值（等同 IS NULL）
            wrapper.and(w -> w.isNull(ServiceItem::getCategoryId).or().eq(ServiceItem::getCategoryId, 0L)
                .or().notIn(ServiceItem::getCategoryId, spaceCategoryIds));
        }
        wrapper.orderByAsc(ServiceItem::getSortOrder).orderByAsc(ServiceItem::getId);
        Page<ServiceItemVo> page = itemMapper.selectVoPage(pageQuery.build(), wrapper);
        fillCategoryName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public ServiceItemVo queryById(Long id) {
        ServiceItemVo vo = itemMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("服务项目不存在");
        }
        fillCategoryName(CollUtil.newArrayList(vo));
        return vo;
    }

    @Override
    public List<ServiceItemVo> listOnSale(Long categoryId) {
        List<ServiceItemVo> list = itemMapper.selectVoList(Wrappers.<ServiceItem>lambdaQuery()
            .eq(categoryId != null, ServiceItem::getCategoryId, categoryId)
            .eq(ServiceItem::getStatus, "0")
            .orderByAsc(ServiceItem::getSortOrder)
            .orderByAsc(ServiceItem::getId));
        fillCategoryName(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(ServiceItemBo bo) {
        validatePricingType(bo.getPricingType());
        ServiceItem entity = BeanCopyUtils.copy(bo, ServiceItem.class);
        entity.setId(null);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        if (StrUtil.isBlank(entity.getPricingType())) {
            entity.setPricingType(PricingType.TIMES.getCode());
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        // ★T26.2 房间二态字段默认值：has_seat_layout 缺省 0（无位置）；seat_capacity 缺省回退 capacity（有位置时可排座位数）
        if (entity.getHasSeatLayout() == null) {
            entity.setHasSeatLayout(0);
        }
        if (entity.getSeatCapacity() == null && entity.getHasSeatLayout() == 1 && entity.getCapacity() != null) {
            entity.setSeatCapacity(entity.getCapacity());
        }
        // ★重复房间修复：房间（空间分类）同名禁建——同名会在各选房间入口视觉重复
        assertRoomNameUnique(entity.getCategoryId(), entity.getName(), null);
        itemMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(ServiceItemBo bo) {
        ServiceItem exist = itemMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("服务项目不存在");
        }
        validatePricingType(bo.getPricingType());
        // ★重复房间修复：房间（空间分类）禁止改为同名（排除自身）
        assertRoomNameUnique(bo.getCategoryId() != null ? bo.getCategoryId() : exist.getCategoryId(),
            bo.getName() != null ? bo.getName() : exist.getName(), exist.getId());
        ServiceItem update = BeanCopyUtils.copy(bo, ServiceItem.class);
        int rows = itemMapper.updateById(update);
        // ★改价留痕：仅当提交了新单价且与原价不同才写记录（updateById 为 NOT_NULL 策略，price=null 表示不改价）
        BigDecimal newPrice = bo.getPrice();
        if (newPrice != null && priceChanged(exist.getPrice(), newPrice)) {
            recordPriceChange(exist.getId(), exist.getPrice(), newPrice, bo.getRemark());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changePrice(Long id, BigDecimal newPrice, String remark) {
        if (id == null) {
            throw new ServiceException("服务项目ID不能为空");
        }
        if (newPrice == null) {
            throw new ServiceException("新单价不能为空");
        }
        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("单价不能为负数");
        }
        ServiceItem exist = itemMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("服务项目不存在");
        }
        ServiceItem update = new ServiceItem();
        update.setId(id);
        update.setPrice(newPrice);
        int rows = itemMapper.updateById(update);
        // ★改价确认：价格未变化则不重复留痕
        if (priceChanged(exist.getPrice(), newPrice)) {
            recordPriceChange(id, exist.getPrice(), newPrice, remark);
        }
        return rows;
    }

    @Override
    public int toggle(Long id, String status) {
        if (id == null) {
            throw new ServiceException("服务项目ID不能为空");
        }
        ServiceItem update = new ServiceItem();
        update.setId(id);
        update.setStatus(StrUtil.isBlank(status) ? "0" : status);
        return itemMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return itemMapper.deleteBatchIds(ids);
    }

    /**
     * 写入改价记录（price_change_log）：operator/operatorId 取当前登录人，change_time 显式设置。
     * tenant_id 由 CreateAndUpdateMetaObjectHandler.insertFill 从登录上下文自动填充。
     */
    private void recordPriceChange(Long itemId, BigDecimal oldPrice, BigDecimal newPrice, String remark) {
        PriceChangeLog entity = new PriceChangeLog();
        entity.setItemId(itemId);
        entity.setOldPrice(oldPrice);
        entity.setNewPrice(newPrice);
        entity.setOperator(LoginHelper.getUsername());
        entity.setOperatorId(LoginHelper.getUserId());
        entity.setChangeTime(new Date());
        entity.setRemark(remark);
        priceChangeLogMapper.insert(entity);
    }

    /**
     * 价格是否发生变化（BigDecimal compareTo，null 安全）
     */
    private boolean priceChanged(BigDecimal oldPrice, BigDecimal newPrice) {
        if (oldPrice == null && newPrice == null) {
            return false;
        }
        if (oldPrice == null || newPrice == null) {
            return true;
        }
        return oldPrice.compareTo(newPrice) != 0;
    }

    /**
     * 校验计价方式合法性（空值放行，走 DB/insert 默认 times）
     */
    private void validatePricingType(String pricingType) {
        if (StrUtil.isNotBlank(pricingType) && !PricingType.isValid(pricingType)) {
            throw new ServiceException("非法计价方式：" + pricingType);
        }
    }

    /**
     * ★重复房间修复：房间（空间分类）同名唯一校验
     * <p>房间名重复会在预约/活动/布局配置等所有选房间入口出现「重复房间」，写入时直接拒绝；
     *   项目（非空间分类）不受限，保持存量语义。多租户插件自动追加 tenant_id 过滤。
     *
     * @param categoryId 目标分类ID（可空=非房间，放行）
     * @param name       房间名称
     * @param excludeId  排除自身ID（编辑场景）
     */
    private void assertRoomNameUnique(Long categoryId, String name, Long excludeId) {
        if (categoryId == null || StrUtil.isBlank(name)) {
            return;
        }
        List<Long> spaceCatIds = categoryMapper.selectList(Wrappers.<ServiceCategory>lambdaQuery()
                .eq(ServiceCategory::getCategoryType, ServiceCategoryServiceImpl.CATEGORY_TYPE_SPACE))
            .stream().map(ServiceCategory::getId).collect(Collectors.toList());
        if (!spaceCatIds.contains(categoryId)) {
            return;
        }
        Long dupCount = itemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery()
            .in(ServiceItem::getCategoryId, spaceCatIds)
            .eq(ServiceItem::getName, name)
            .ne(excludeId != null, ServiceItem::getId, excludeId));
        if (dupCount != null && dupCount > 0) {
            throw new ServiceException("同名房间已存在：" + name);
        }
    }

    /**
     * 批量回填服务分类名（service_category.name）
     */
    private void fillCategoryName(List<ServiceItemVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> categoryIds = list.stream().map(ServiceItemVo::getCategoryId)
            .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (CollUtil.isEmpty(categoryIds)) {
            return;
        }
        List<ServiceCategory> categories = categoryMapper.selectList(Wrappers.<ServiceCategory>lambdaQuery()
            .in(ServiceCategory::getId, categoryIds));
        Map<Long, String> nameMap = categories.stream()
            .collect(Collectors.toMap(ServiceCategory::getId, ServiceCategory::getName, (a, b) -> a));
        list.forEach(vo -> vo.setCategoryName(nameMap.get(vo.getCategoryId())));
    }
}
