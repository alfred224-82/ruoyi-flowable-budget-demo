package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FeePaymentBo;
import com.ruoyi.system.domain.vo.FeePaymentVo;

/**
 * 付款单Service接口（T5.2）
 * 必选项目、财务登记、老板审批、通过扣项目预算 + 记总支出
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeePaymentService {

    /**
     * 分页查询
     */
    TableDataInfo<FeePaymentVo> queryPageList(FeePaymentBo bo, PageQuery pageQuery);

    /**
     * 详情
     */
    FeePaymentVo queryById(Long id);

    /**
     * 财务登记付款单（状态=待审批）
     */
    Long insertPayment(FeePaymentBo bo);

    /**
     * 老板审批通过 → 扣项目预算 + 记总支出
     */
    int approvePayment(Long id);

    /**
     * 老板驳回
     */
    int rejectPayment(Long id, String reason);
}
