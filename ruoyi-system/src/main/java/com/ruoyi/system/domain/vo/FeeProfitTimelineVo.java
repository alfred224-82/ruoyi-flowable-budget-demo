package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目收支时间线事件（T6.5 / FEAT_PROFIT_REPORT）
 * 合同→首付→进度款→验收→付款→收款全链路追溯
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProfitTimelineVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件类型(CONTRACT/INCOME_CONFIRM/PAYMENT/RECEIPT/EXPENSE) */
    private String eventType;

    /** 事件描述 */
    private String eventLabel;

    /** 发生时间 */
    private Date occurDate;

    /** 金额 */
    private BigDecimal amount;

    /** 关联单号/依据编号 */
    private String refNo;

    /** 单据状态（收入确认核验状态/付款收款审批状态） */
    private String status;
}
