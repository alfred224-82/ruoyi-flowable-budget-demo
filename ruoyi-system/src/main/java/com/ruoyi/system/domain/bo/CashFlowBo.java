package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 收支流水业务对象 cash_flow（T15.14 手动补录收入/支出）
 * <p>老板 PC 手动补录（工资/房租/水电/采购...）；系统自动记账（订单收款/结算发放）由服务层内部构造，不走本 BO。
 * 兼作查询过滤：flowType/category/relatedType + occurDate 时间范围（beginTime/endTime）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class CashFlowBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 流水类型：income(收入)/expense(支出)；★由 Controller 按补录入口(收入/支出)服务端赋值，客户端无需传，故不做 AddGroup 非空校验 */
    private String flowType;

    /** 收支分类（订单收入/工资结算/房租/水电/采购...） */
    private String category;

    /** 金额 */
    @NotNull(message = "金额不能为空", groups = {AddGroup.class})
    private BigDecimal amount;

    /** 关联单据ID */
    private Long relatedOrderId;

    /** 关联单据类型（booking/settlement/manual） */
    private String relatedType;

    /** 发生日期（为空则取当天） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date occurDate;

    /** 备注 */
    private String remark;

    /** 查询过滤：发生日期范围起 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date beginTime;

    /** 查询过滤：发生日期范围止 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endTime;
}
