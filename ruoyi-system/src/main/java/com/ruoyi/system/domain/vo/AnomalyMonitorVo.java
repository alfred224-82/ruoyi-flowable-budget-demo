package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 异常监控视图对象
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
public class AnomalyMonitorVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 报销单号
     */
    private String billNo;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 物资小类编码
     */
    private String itemCategory;

    /**
     * 用途标签编码
     */
    private String purposeTag;

    /**
     * 报销金额
     */
    private BigDecimal amount;

    /**
     * 费用发生日期
     */
    private Date expenseDate;

    /**
     * 分摊状态
     */
    private String allocationStatus;

    /**
     * 状态
     */
    private String status;
}
