package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户大类启用对象 fee_tenant_category（T1.5 / T5.13 三层角色分工）
 * platform_admin 从 admin 定义的大类中勾选租户需要启用的大类，绑定 tenant_id
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_tenant_category")
public class FeeTenantCategory extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 费用大类编码（关联 fee_expense_category.category_code）
     */
    private String categoryCode;

    /**
     * 大类名称（冗余，便于展示）
     */
    private String categoryName;

    /**
     * 是否启用（0-停用 1-启用）
     */
    private Integer isEnabled;
}
