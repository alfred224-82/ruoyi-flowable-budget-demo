package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 人员结算业务对象 staff_settlement（T15.13）
 * <p>老板 PC：选择员工 + 周期 → 服务层自动汇总单量 → 算应发金额 → 生成「待发放」结算单。
 * total_orders/total_amount/settle_no 由服务层计算，不接受前端提交；兼作查询过滤：staffUserId/payStatus。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class StaffSettlementBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 被结算员工/助教用户ID */
    @NotNull(message = "结算员工不能为空", groups = {AddGroup.class})
    private Long staffUserId;

    /** 结算周期起 */
    @NotNull(message = "结算周期起不能为空", groups = {AddGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date periodStart;

    /** 结算周期止 */
    @NotNull(message = "结算周期止不能为空", groups = {AddGroup.class})
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date periodEnd;

    /** 发放状态（查询过滤用）：PENDING/PAID_OFFLINE */
    private String payStatus;

    /** 备注 */
    private String remark;
}
