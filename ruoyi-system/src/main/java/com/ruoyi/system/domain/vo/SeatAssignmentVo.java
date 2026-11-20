package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 活动座位占用视图对象（T26.8，小微版 M26）
 * <p>★Vo 约定：仅用 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class SeatAssignmentVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（assignmentId，顾客取消座位时使用） */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 活动ID */
    private Long activityId;

    /** 座位ID */
    private Long seatId;

    /** 座位号冗余 */
    private String seatNo;

    /** 占座人 user_id */
    private Long userId;

    /** 占座人昵称（回填，非表字段） */
    private String userNickName;

    /** 占座人头像 URL（回填，非表字段；顾客端仅显示小图，不展示手机号） */
    private String userAvatar;

    /** PRESET / BOSS_ASSIGN / CUSTOMER_PICK */
    private String assignmentType;

    /** 占座人标准角色编码（回填，非表字段：boss/salesman/customer；T26.10 画布回显区分员工/游客） */
    private String roleCode;

    /** 占座类型中文（回填，非表字段） */
    private String assignmentTypeStr;

    /** 占座时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date assignTime;

    /** 活动锁定后=1，禁止改动 */
    private Integer locked;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
