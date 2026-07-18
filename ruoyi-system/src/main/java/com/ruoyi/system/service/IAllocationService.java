package com.ruoyi.system.service;

/**
 * 费用分摊Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IAllocationService {

    /**
     * 执行月末费用分摊
     *
     * @param year  年度
     * @param month 月份
     */
    void executeMonthlyAllocation(int year, int month);
}
