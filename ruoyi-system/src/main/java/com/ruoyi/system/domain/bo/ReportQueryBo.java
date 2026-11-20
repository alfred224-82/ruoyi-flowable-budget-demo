package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 报表查询业务对象（T15.14 利润报表/趋势/资金水位按日期范围统计）
 * <p>周报/月报/今日概览由服务层内部计算周期，无需前端传参；利润报表与趋势查询用本 BO 指定范围。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class ReportQueryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 统计周期起（含，yyyy-MM-dd） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date beginDate;

    /** 统计周期止（含，yyyy-MM-dd） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
}
