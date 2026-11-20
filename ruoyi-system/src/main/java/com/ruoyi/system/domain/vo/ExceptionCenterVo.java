package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 业务异常处理中心 VO（T18.8，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class ExceptionCenterVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 异常项列表 */
    private List<ExceptionItem> items;

    /** 异常总数 */
    private Integer totalCount;

    @Data
    public static class ExceptionItem implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 异常ID（来源记录ID） */
        private Long id;

        /** 异常类型：BOOKING_CONFLICT/BOOKING_FAILED/FIXED_COST_ANOMALY/COMPLIANCE_HIT */
        private String exceptionType;

        /** 异常类型描述 */
        private String exceptionTypeLabel;

        /** 严重程度：HIGH/MEDIUM/LOW */
        private String severity;

        /** 标题 */
        private String title;

        /** 详细描述 */
        private String description;

        /** 关联订单号（预约类异常有） */
        private String orderNo;

        /** 发生时间 */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private Date occurTime;

        /** 处理状态：PENDING/PROCESSING/RESOLVED */
        private String status;

        /** 建议操作 */
        private String suggestedAction;

        /** 是否支持一键重新预约 */
        private Boolean canRetryBooking;
    }
}
