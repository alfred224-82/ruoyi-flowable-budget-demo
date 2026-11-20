package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 爽约明细视图对象 no_show_record（T15.31，顾客完整爽约历史 micro:customer:creditDetail）
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Data
public class NoShowRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 顾客用户ID */
    private Long userId;

    /** 爽约订单编号（booking_order.order_no） */
    private String orderNo;

    /** 原预约到店时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date bookTime;

    /** 标记爽约时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date markedAt;

    /** 标记来源：system(定时任务自动)/boss(老板手动) */
    private String markedBy;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
