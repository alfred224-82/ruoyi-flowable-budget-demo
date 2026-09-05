package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户大类启用视图对象 fee_tenant_category（T5.13 三层角色分工）
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Data
public class FeeTenantCategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 费用大类编码
     */
    private String categoryCode;

    /**
     * 大类名称
     */
    private String categoryName;

    /**
     * 是否启用（0-停用 1-启用）
     */
    private Integer isEnabled;

    /**
     * 创建时间
     */
    private Date createTime;
}
