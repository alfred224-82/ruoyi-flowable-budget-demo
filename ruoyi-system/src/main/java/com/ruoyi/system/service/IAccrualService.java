package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.FeeAccrualRecord;

/**
 * 周期计提引擎Service接口
 * 设计文档 Phase 6 Task 6.4
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IAccrualService {

    /**
     * 执行月度计提（定时任务调用）
     * 包含：资产折旧 + 合同摊销
     *
     * @param period 计提期间（YYYY-MM）
     * @return 计提记录数
     */
    int executeMonthlyAccrual(String period);

    /**
     * 财务复核确认凭证
     *
     * @param accrualRecordId 计提记录ID
     * @return 是否成功
     */
    Boolean confirmVoucher(Long accrualRecordId);

    /**
     * 作废计提记录
     *
     * @param accrualRecordId 计提记录ID
     * @param reason          作废原因
     * @return 是否成功
     */
    Boolean voidVoucher(Long accrualRecordId, String reason);

    /**
     * 查询计提记录列表
     *
     * @param period     计提期间
     * @param sourceType 来源类型
     * @param pageQuery  分页参数
     * @return 分页结果
     */
    Page<FeeAccrualRecord> listRecords(String period, String sourceType, PageQuery pageQuery);
}
