package com.ruoyi.system.domain.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 价格调整记录查询业务对象 price_change_log（T15.4/T15.11 ④ 改价记录查询）
 * <p>只读查询：按服务项目 + 改价时间范围过滤；记录由服务层改价时自动写入，不接受前端提交。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class PriceChangeLogBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 服务项目ID（精确过滤） */
    private Long itemId;

    /** 改价时间范围起（含，yyyy-MM-dd HH:mm:ss） */
    private Date beginTime;

    /** 改价时间范围止（含，yyyy-MM-dd HH:mm:ss） */
    private Date endTime;
}
