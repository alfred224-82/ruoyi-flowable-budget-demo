package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户级功能配置对象 sys_tenant_config
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_config")
public class SysTenantConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 配置键（如 approval_flow / welfare_policy / subject_system）
     */
    private String configKey;

    /**
     * JSON 配置值
     */
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
