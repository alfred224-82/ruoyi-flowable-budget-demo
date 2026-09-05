package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户版本变更日志视图对象
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
public class SysTenantVersionLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 变更前版本
     */
    private String fromEdition;

    /**
     * 变更后版本
     */
    private String toEdition;

    /**
     * 变更类型
     */
    private String changeType;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 备注
     */
    private String remark;

    private Date createTime;
}
