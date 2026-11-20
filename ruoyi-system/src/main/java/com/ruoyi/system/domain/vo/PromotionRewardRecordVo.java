package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 拉新奖励发放记录视图对象 promotion_reward_record（T16.7，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class PromotionRewardRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long promoterUserId;
    /** 推广人昵称（回填，非表字段） */
    private String promoterName;
    private String promoterRoleCode;
    private Long customerUserId;
    /** 顾客昵称（回填，非表字段） */
    private String customerName;
    private Long firstOrderId;
    private String rewardType;
    private BigDecimal rewardAmount;
    private Long couponRecordId;
    private String issueStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date issuedTime;

    private Integer notified;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
