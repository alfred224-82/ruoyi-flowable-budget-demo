package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.PriceChangeLog;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.bo.PriceChangeLogBo;
import com.ruoyi.system.domain.vo.PriceChangeLogVo;
import com.ruoyi.system.mapper.PriceChangeLogMapper;
import com.ruoyi.system.mapper.ServiceItemMapper;
import com.ruoyi.system.service.IPriceChangeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 价格调整记录服务实现（T15.4/T15.11 ④ 改价历史查询）
 * <p>price_change_log 租户级表，多租户插件自动隔离；只读查询，记录由 {@link ServiceItemServiceImpl} 改价时写入。
 * 列表回填 itemName（service_item.name），change_time 倒序（最近改价在前）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceChangeLogServiceImpl implements IPriceChangeLogService {

    private final PriceChangeLogMapper priceChangeLogMapper;
    private final ServiceItemMapper itemMapper;

    @Override
    public TableDataInfo<PriceChangeLogVo> queryPageList(PriceChangeLogBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<PriceChangeLog> wrapper = Wrappers.<PriceChangeLog>lambdaQuery()
            .eq(bo.getItemId() != null, PriceChangeLog::getItemId, bo.getItemId())
            .ge(bo.getBeginTime() != null, PriceChangeLog::getChangeTime, bo.getBeginTime())
            .le(bo.getEndTime() != null, PriceChangeLog::getChangeTime, bo.getEndTime())
            .orderByDesc(PriceChangeLog::getChangeTime)
            .orderByDesc(PriceChangeLog::getId);
        Page<PriceChangeLogVo> page = priceChangeLogMapper.selectVoPage(pageQuery.build(), wrapper);
        fillItemName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<PriceChangeLogVo> listByItem(Long itemId) {
        List<PriceChangeLogVo> list = priceChangeLogMapper.selectVoList(Wrappers.<PriceChangeLog>lambdaQuery()
            .eq(itemId != null, PriceChangeLog::getItemId, itemId)
            .orderByDesc(PriceChangeLog::getChangeTime)
            .orderByDesc(PriceChangeLog::getId));
        fillItemName(list);
        return list;
    }

    /**
     * 批量回填服务项目名称（service_item.name）
     */
    private void fillItemName(List<PriceChangeLogVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> itemIds = list.stream().map(PriceChangeLogVo::getItemId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        if (CollUtil.isEmpty(itemIds)) {
            return;
        }
        List<ServiceItem> items = itemMapper.selectList(Wrappers.<ServiceItem>lambdaQuery()
            .in(ServiceItem::getId, itemIds));
        Map<Long, String> nameMap = items.stream()
            .collect(Collectors.toMap(ServiceItem::getId, ServiceItem::getName, (a, b) -> a));
        list.forEach(vo -> vo.setItemName(nameMap.get(vo.getItemId())));
    }
}
