package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 项目分成对象 fee_project_sharing（T5.9 / ADR-025）
 * 老板定比例，项目经理仅可申请
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_project_sharing")
public class FeeProjectSharing extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 分成对象用户ID
     */
    private Long shareToUserId;

    /**
     * 分成对象姓名
     */
    private String shareToUserName;

    /**
     * 角色(INTRODUCER/PARTNER/CHANNEL)
     */
    private String role;

    /**
     * 分成基数类型(CONTRACT/INCOME/PROFIT/FIXED)
     */
    private String baseType;

    /**
     * 分成基数
     */
    private BigDecimal baseAmount;

    /**
     * 分成比例(老板定)
     */
    private BigDecimal ratio;

    /**
     * 分成金额
     */
    private BigDecimal shareAmount;

    /**
     * 状态(PENDING/ACTIVE/PAID)
     */
    private String status;

    /**
     * 依据附件
     */
    private String basisUrl;
}
