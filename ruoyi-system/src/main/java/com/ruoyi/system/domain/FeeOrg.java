package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 组织树对象 fee_org（T7.1 / FEAT_MULTI_ORG）
 * 租户内二级维度：集团总部 + 子公司独立预算体系
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_org")
public class FeeOrg extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 组织编码
     */
    private String orgCode;

    /**
     * 组织名称
     */
    private String orgName;

    /**
     * 父组织ID(0=集团总部)
     */
    private Long parentId;

    /**
     * 祖级列表（逗号分隔，同 sys_dept.ancestors）
     */
    private String ancestors;

    /**
     * 负责人用户ID
     */
    private Long leaderUserId;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
