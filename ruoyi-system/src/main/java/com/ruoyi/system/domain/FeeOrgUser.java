package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户-组织关系对象 fee_org_user（T7.1 / FEAT_MULTI_ORG）
 * 用户挂组织：数据范围=本组织+下级组织
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_org_user")
public class FeeOrgUser extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 组织ID(fee_org)
     */
    private Long orgId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名(冗余)
     */
    private String userName;

    /**
     * 是否主组织(多组织用户登录选择用)
     */
    private Integer isPrimary;
}
