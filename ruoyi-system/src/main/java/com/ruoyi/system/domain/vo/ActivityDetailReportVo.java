package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 活动明细表报表视图对象（M39 T39.1，7.14）
 * <p>rows=明细行；totalRow=底部固定合计行；unsettledCount=未结算场次提醒。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class ActivityDetailReportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 明细行列表 */
    private List<ActivityDetailRowVo> rows;

    /** 底部固定合计行（收入/成本/利润/差额各列汇总） */
    private ActivityDetailRowVo totalRow;

    /** 未结算场次数（DRAFT 状态，报表未计入合计的提醒） */
    private Integer unsettledCount;

    /** 总记录数（分页用） */
    private Long total;
}
