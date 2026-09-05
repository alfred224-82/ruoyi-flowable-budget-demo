package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户业态单店覆盖视图对象 tenant_biz_config（T10.4）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class TenantBizConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 单店覆盖配置 JSON */
    private String moduleOverrides;

    private Date updateTime;
}
