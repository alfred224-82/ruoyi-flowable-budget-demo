package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 组织级预算版本快照对象 fee_org_budget_version（T7.1 / FEAT_MULTI_ORG+FEAT_ORG_SNAPSHOT）
 * 组织分别编制与锁定：锁定后快照不可变，版本号+1不覆盖
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_org_budget_version")
public class FeeOrgBudgetVersion extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 组织ID
     */
    private Long orgId;

    /**
     * 预算期间 yyyy-MM
     */
    private String period;

    /**
     * 版本号(锁定后+1,不覆盖)
     */
    private Integer versionNo;

    /**
     * 状态 DRAFT/PENDING/LOCKED
     */
    private String status;

    /**
     * 预算快照JSON(锁定后不可变)
     */
    private String snapshotData;

    /**
     * 锁定人
     */
    private Long lockedBy;

    /**
     * 锁定时间
     */
    private Date lockedAt;
}
