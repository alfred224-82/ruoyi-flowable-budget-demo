package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 资金池配置业务对象
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
public class FundPoolConfigBo {

    /**
     * 主键（编辑时传入）
     */
    private Long id;

    /**
     * 层级类型（DEPT/BRANCH/HQ）
     */
    @NotBlank(message = "层级类型不能为空")
    private String levelType;

    /**
     * 层级名称
     */
    @NotBlank(message = "层级名称不能为空")
    private String levelName;

    /**
     * 关联角色ID
     */
    private Long roleId;

    /**
     * 角色标识
     */
    private String roleKey;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 初始额度
     */
    @NotNull(message = "初始额度不能为空")
    private BigDecimal initialAmount;

    /**
     * 预警阈值百分比
     */
    @NotNull(message = "预警阈值不能为空")
    private BigDecimal warningThreshold;

    /**
     * 状态（ENABLED/DISABLED）
     */
    private String status;
}
