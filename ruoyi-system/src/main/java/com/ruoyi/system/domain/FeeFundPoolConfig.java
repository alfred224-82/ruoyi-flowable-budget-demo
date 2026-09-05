package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 资金池初始额度配置表 fee_fund_pool_config
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_fund_pool_config")
public class FeeFundPoolConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 层级类型（DEPT/BRANCH/HQ）
     */
    private String levelType;

    /**
     * 层级名称（部门级/分公司级/总公司级）
     */
    private String levelName;

    /**
     * 关联角色ID
     */
    private Long roleId;

    /**
     * 角色标识（冗余）
     */
    private String roleKey;

    /**
     * 角色名称（冗余）
     */
    private String roleName;

    /**
     * 初始额度
     */
    private BigDecimal initialAmount;

    /**
     * 预警阈值百分比
     */
    private BigDecimal warningThreshold;

    /**
     * 状态（ENABLED/DISABLED）
     */
    private String status;
}
