package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 租户级功能配置业务对象
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
public class SysTenantConfigBo {

    /**
     * 主键（编辑时传入）
     */
    private Long id;

    /**
     * 租户ID
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;

    /**
     * 配置键
     */
    @NotBlank(message = "配置键不能为空")
    private String configKey;

    /**
     * JSON 配置值
     */
    @NotBlank(message = "配置值不能为空")
    private String configValue;

    /**
     * 配置分类
     */
    private String configType;

    /**
     * 备注
     */
    private String remark;
}
