package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 烂账记录对象 fee_bad_debt（T5.6）
 * 风险标记 + 坏账计提 + 项目状态 LOST 联动
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_bad_debt")
public class FeeBadDebt extends TenantEntity {

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
     * 标记人
     */
    private Long markedBy;

    /**
     * 标记时间
     */
    private Date markedAt;

    /**
     * 原因说明
     */
    private String reason;

    /**
     * 烂账类型(LOST_CONTACT/QUALITY_DISPUTE/PRICE_ISSUE/INCOMPLETE/OTHER)
     */
    private String type;

    /**
     * 处置建议(BAD_DEBT/FOLLOW_UP/LEGAL)
     */
    private String proposal;

    /**
     * 计提坏账金额
     */
    private BigDecimal provisionAmount;

    /**
     * 状态(OPEN/FOLLOWING/CLOSED)
     */
    private String status;

    /**
     * 跟进记录
     */
    private String followUpNotes;
}
