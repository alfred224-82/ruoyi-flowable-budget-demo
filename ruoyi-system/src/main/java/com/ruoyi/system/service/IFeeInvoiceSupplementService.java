package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.FeeInvoiceSupplementVo;

import java.util.Date;

/**
 * 补票记录Service接口（T5.8 / ADR-018）
 * 无发票后补通道：勾选后补+预计补票日期（默认7天）→财务审核必填理由→老板必批
 * →额度预占→补票转实扣→超时释放+异常标记
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeInvoiceSupplementService {

    /**
     * 分页查询
     */
    TableDataInfo<FeeInvoiceSupplementVo> queryPageList(String status, PageQuery pageQuery);

    /**
     * 详情
     */
    FeeInvoiceSupplementVo queryById(Long id);

    /**
     * 报销单审批通过后创建补票记录（额度预占，预计补票日期默认提交日+7天）
     */
    Long createSupplement(Long billId, Date estimatedDate);

    /**
     * 财务审核（后补单理由必填）
     */
    int auditSupplement(Long id, String auditReason);

    /**
     * 补传发票（预占转实扣，状态=COMPLETED）
     */
    int supplementInvoice(Long id, String invoiceUrls);

    /**
     * 超时释放（预占释放+异常标记，状态=OVERDUE，由定时任务调用）
     */
    int markOverdue(Long id);

    /**
     * 扫描超时未补记录并释放（定时任务入口）
     */
    int scanAndReleaseOverdue();
}
