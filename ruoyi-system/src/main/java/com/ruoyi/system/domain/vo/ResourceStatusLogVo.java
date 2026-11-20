package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 资源状态变更日志视图对象 resource_status_log（T21.5）
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Data
public class ResourceStatusLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 资源类型：staff/room */
    private String resourceType;

    /** 资源ID */
    private Long resourceId;

    /** 资源名称（冗余快照） */
    private String resourceName;

    /** 变更前状态 */
    private String fromStatus;

    /** 变更后状态 */
    private String toStatus;

    /** 操作人 user_id */
    private Long operatorId;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;

    /** 变更原因 */
    private String reason;

    /** 关联 booking_order.id */
    private Long bookingId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
