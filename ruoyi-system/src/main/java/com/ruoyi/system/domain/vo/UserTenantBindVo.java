package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户租户绑定视图对象 user_tenant_bind（T10.1 / T13.1）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class UserTenantBindVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 微信 openid */
    private String openid;

    /** 租户ID */
    private Long tenantId;

    /** 绑定的系统用户ID */
    private Long userId;

    /** 是否默认门店（0否 1是） */
    private Integer isDefault;

    private Date createTime;

    private Date updateTime;
}
