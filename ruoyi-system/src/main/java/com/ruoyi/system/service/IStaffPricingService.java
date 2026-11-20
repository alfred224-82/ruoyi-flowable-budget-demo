package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.StaffPricingBo;
import com.ruoyi.system.domain.vo.StaffPricingVo;

import java.util.List;

/**
 * 助教价格服务接口（T21.7，小微版，支持 4 种计价模式）
 * <p>人员定价支持计时/套餐/计量/一口价，对齐 jjadmin 定价体系。
 * <p>★房间价格复用 service_item.price（已有字段，通过 IServiceItemService 管理）。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
public interface IStaffPricingService {

    /**
     * 查询本租户全部助教价格列表（回填昵称/头像）
     */
    List<StaffPricingVo> listAll();

    /**
     * 设置/更新助教小时价格（upsert：同一助教仅一条记录）
     *
     * @return 记录ID
     */
    Long saveOrUpdatePrice(StaffPricingBo bo);

    /**
     * 查询单个助教的小时单价（顾客端用，未设置返回 null）
     *
     * @param userId 助教 user_id
     * @return 小时单价（未设置返回 null）
     */
    java.math.BigDecimal getHourlyRate(Long userId);

    /**
     * 批量查询助教小时单价（顾客端用）
     *
     * @param userIds 助教 user_id 列表
     * @return Map<userId, hourlyRate>（未设置的助教不包含在内）
     */
    java.util.Map<Long, java.math.BigDecimal> batchGetHourlyRates(List<Long> userIds);
}
