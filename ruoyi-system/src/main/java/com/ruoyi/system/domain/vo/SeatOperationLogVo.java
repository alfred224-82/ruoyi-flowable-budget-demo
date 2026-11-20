package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 活动座位操作日志视图对象（T26.8 / T28.4，小微版 M26 / M28）
 * <p>★Vo 约定：仅用 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class SeatOperationLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 活动ID */
    private Long activityId;

    /** 活动标题（回填，非表字段） */
    private String activityTitle;

    /** ASSIGN/MOVE/SWAP/LOCK/UNLOCK/COPY_LAYOUT/PRESET/RELEASE */
    private String operationType;

    /** 操作类型中文（回填，非表字段） */
    private String operationTypeStr;

    /** 操作人 user_id */
    private Long operatorUserId;

    /** 操作人昵称（回填，非表字段） */
    private String operatorNickName;

    /** 操作人角色：boss/salesman/customer */
    private String operatorRole;

    /** 操作前快照 JSON */
    private String beforeSnapshot;

    /** 操作后快照 JSON */
    private String afterSnapshot;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;

    /** 备注 */
    private String remark;
}
