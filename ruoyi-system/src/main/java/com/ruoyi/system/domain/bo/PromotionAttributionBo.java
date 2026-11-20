package com.ruoyi.system.domain.bo;

import lombok.Data;

import java.io.Serializable;

/**
 * 推广归因查询业务对象 promotion_attribution（T16.6，小微版 M16）
 * <p>仅查询过滤（扫码归因由引擎内部 upsert，不开放外部写入）。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class PromotionAttributionBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 被归因顾客用户ID */
    private Long customerUserId;

    /** 顾客手机号（模糊查询） */
    private String customerPhone;

    /** 当前推广人用户ID */
    private Long promoterUserId;

    /** 推广人角色 */
    private String promoterRoleCode;

    /** 归因状态：0有效 1失效 */
    private String status;
}
