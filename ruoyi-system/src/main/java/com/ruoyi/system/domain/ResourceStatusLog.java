package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 资源状态变更日志 resource_status_log（T21.5，小微版）
 * <p>记录助教/房间资源状态流转（idle→reserved→in_use→idle），便于审计追溯。
 * 租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_status_log")
public class ResourceStatusLog extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 资源类型：staff=助教 / room=房间
     */
    private String resourceType;

    /**
     * 资源ID（助教=sys_user.user_id / 房间=service_item.id）
     */
    private Long resourceId;

    /**
     * 资源名称（冗余快照，便于日志查看）
     */
    private String resourceName;

    /**
     * 变更前状态：idle/reserved/in_use
     */
    private String fromStatus;

    /**
     * 变更后状态：idle/reserved/in_use
     */
    private String toStatus;

    /**
     * 操作人 user_id（系统自动操作为 NULL）
     */
    private Long operatorId;

    /**
     * 操作时间
     */
    private Date operateTime;

    /**
     * 变更原因（手动释放需填写/预约提交/活动取消等）
     */
    private String reason;

    /**
     * 关联 booking_order.id（预约触发时填写，手动释放为 NULL）
     */
    private Long bookingId;
}
