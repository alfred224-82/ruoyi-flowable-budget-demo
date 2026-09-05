package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 组织树业务对象 fee_org（T7.1 / FEAT_MULTI_ORG）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeOrgBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "组织ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 组织编码 */
    @NotBlank(message = "组织编码不能为空", groups = {AddGroup.class})
    private String orgCode;

    /** 组织名称 */
    @NotBlank(message = "组织名称不能为空", groups = {AddGroup.class})
    private String orgName;

    /** 父组织ID(0=集团总部) */
    private Long parentId;

    /** 负责人用户ID */
    private Long leaderUserId;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;
}
