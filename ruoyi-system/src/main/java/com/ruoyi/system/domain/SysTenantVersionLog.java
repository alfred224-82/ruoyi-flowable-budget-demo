package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户版本变更日志对象 sys_tenant_version_log
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_version_log")
public class SysTenantVersionLog extends BaseEntity {

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
     * 变更前版本
     */
    private String fromEdition;

    /**
     * 变更后版本
     */
    private String toEdition;

    /**
     * 变更类型：INIT/UPGRADE/DOWNGRADE/LICENSE_IMPORT/LICENSE_REPLACE/LICENSE_EXPIRE/LICENSE_DOWNGRADE
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
}
