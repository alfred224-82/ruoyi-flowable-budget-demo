package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.PriceChangeLogBo;
import com.ruoyi.system.domain.vo.PriceChangeLogVo;

import java.util.List;

/**
 * 价格调整记录服务接口（T15.4/T15.11 ④ 改价历史查询）
 * <p>只读查询：记录由 {@link IServiceItemService} 改价时自动写入，本服务不提供新增/修改入口。
 * 租户级表，多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IPriceChangeLogService {

    /**
     * 分页查询改价记录（按服务项目 + 改价时间范围过滤，change_time 倒序；回填 itemName）
     */
    TableDataInfo<PriceChangeLogVo> queryPageList(PriceChangeLogBo bo, PageQuery pageQuery);

    /**
     * 查询指定服务项目的改价历史（change_time 倒序；回填 itemName）
     */
    List<PriceChangeLogVo> listByItem(Long itemId);
}
