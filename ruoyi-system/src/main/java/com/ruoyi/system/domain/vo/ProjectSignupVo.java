package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目活动报名视图对象 project_signup（T11.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProjectSignupVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 项目容器ID */
    private Long projectId;

    /** 报名用户ID（可空=游客报名） */
    private Long userId;

    /** 报名微信 openid */
    private String openid;

    /** 报名编号 */
    private String signupNo;

    /** 报名人数 */
    private Integer peopleCount;

    /** 名额上限（NULL=不限） */
    private Integer quota;

    /** 是否候补（0否 1是） */
    private Integer isWaitlist;

    /** 签到状态：NOT_SIGNED/SIGNED/ABSENT */
    private String signInStatus;

    /** 报名时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date signupTime;

    private Date createTime;
}
