package com.ruoyi.system.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * 活动明细表查询业务对象（M39 T39.1，7.14）
 * <p>筛选条：日期区间（按活动 end_time 跨月归属）/活动状态/结算状态/本场员工/名称。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class ActivityDetailQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 开始日期（yyyy-MM-dd，含） */
    private String beginDate;

    /** 结束日期（yyyy-MM-dd，含） */
    private String endDate;

    /** 活动状态：DRAFT/PUBLISHED/ENDED（可空） */
    private String activityStatus;

    /** 结算状态：DRAFT/SUBMITTED/LOCKED（可空；空=全部） */
    private String settleStatus;

    /** 本场员工用户ID（可空） */
    private Long staffId;

    /** 活动名称（模糊，可空） */
    private String title;
}
