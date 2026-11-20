package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 推广归因关系对象 promotion_attribution（T16.1/T16.6，小微版 M16）
 * <p>谁最后扫码业绩归谁；交接点锁定不回溯；从未扫码 customer 推广人为 NULL(自然流量)。
 * <p>UNIQUE(tenant_id, customer_user_id) 保证每个顾客只有一个「当前推广人」。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("promotion_attribution")
public class PromotionAttribution extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 被归因顾客用户ID */
    private Long customerUserId;

    /** 顾客手机号(身份锚点) */
    private String customerPhone;

    /** 当前推广人用户ID(NULL=自然流量) */
    private Long promoterUserId;

    /** 当前推广人角色 */
    private String promoterRoleCode;

    /** 最后一次扫码时间(交接点) */
    private Date lastScanTime;

    /** 归因状态：0有效 1失效 */
    private String status;

    private String remark;
}
