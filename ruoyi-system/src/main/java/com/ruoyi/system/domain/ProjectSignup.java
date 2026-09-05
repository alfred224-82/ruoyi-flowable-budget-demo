package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目活动报名对象 project_signup（T11.3，type=activity/team_buy 容器使用）
 * <p>支持游客报名（user_id 可空，记 openid）；满员进候补（is_waitlist）；签到状态机 NOT_SIGNED→SIGNED/ABSENT。
 * 成团/拼桌判定由 ITeamStrategy 策略（FullHouseStrategy/GroupBuyStrategy）驱动，报名服务只负责数据采集与状态回写。
 * 表仅含 create_time/update_time（无 create_by/update_by），故不继承 BaseEntity。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("project_signup")
public class ProjectSignup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 项目容器ID（FK→project）
     */
    private Long projectId;

    /**
     * 报名用户ID（可空=游客报名，记 openid）
     */
    private Long userId;

    /**
     * 报名微信 openid（共用小程序引导态）
     */
    private String openid;

    /**
     * 报名编号
     */
    private String signupNo;

    /**
     * 报名人数
     */
    private Integer peopleCount;

    /**
     * 名额上限（NULL=不限）
     */
    private Integer quota;

    /**
     * 是否候补（0否 1是；满员进候补）
     */
    private Integer isWaitlist;

    /**
     * 签到状态：NOT_SIGNED/SIGNED/ABSENT
     */
    private String signInStatus;

    /**
     * 报名时间
     */
    private Date signupTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
