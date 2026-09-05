package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目成员对象 fee_project_member（T5.1 / T2.3 数据范围过滤依据）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_project_member")
public class FeeProjectMember extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 项目ID
     */
    private Long projectId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 成员角色(MANAGER/MEMBER/VIEWER)
     */
    private String role;

    /**
     * 投入比例%(专业版成员负载预警,FEAT_MEMBER_LOAD)
     */
    private Integer investRatio;

    /**
     * 是否有效
     */
    private Integer isActive;
}
