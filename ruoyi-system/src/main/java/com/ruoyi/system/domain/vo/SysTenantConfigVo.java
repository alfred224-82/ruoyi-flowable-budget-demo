package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户级功能配置视图对象
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
public class SysTenantConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 配置键
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

    private Date createTime;

    private Date updateTime;
}
