package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 待召回顾客视图对象（T15.22 customerTagJob）
 * <p>由 {@code customerTagJob} 逐租户识别：最近到店时间早于阈值（默认 7 天）的顾客即「待召回」。
 * <p>★delivered M15 schema 无顾客标签表/列，故「打标签」实现为识别 + 输出待召回清单（写 XXL-JOB 日志，
 * 租户ID 已由 {@code TenantJobAspect} 注入 MDC，日志可按租户检索），不臆造持久化表。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class MicroRecallCustomerVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 顾客用户ID（可空=游客，仅凭手机号识别） */
    private Long customerUserId;

    /** 顾客昵称（回填自 sys_user，可空） */
    private String customerName;

    /** 顾客手机号 */
    private String customerPhone;

    /** 最近到店时间（已完成/已收款订单的完成时间或收款时间取最大） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastVisitTime;

    /** 距最近到店的天数（未到店时长） */
    private Long inactiveDays;
}
