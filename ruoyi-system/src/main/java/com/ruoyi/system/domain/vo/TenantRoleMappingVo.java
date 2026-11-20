package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 租户角色映射视图对象 tenant_role_mapping（T15.1/T15.9）
 * <p>{@code roleName} 为服务层回填的标准角色名（来自 sys_role.role_name），与租户自定义 {@code displayName} 并列展示；
 * 「挑选标准角色」列表复用本 VO：未映射的标准角色 {@code id=null}，前端据此区分「已挑选/未挑选」。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class TenantRoleMappingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（未映射的标准角色为 null） */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 标准角色编码（sys_role.role_key） */
    private String roleCode;

    /** 标准角色名（展示用，来自 sys_role.role_name） */
    private String roleName;

    /** 角色产品线归属（T15.25，来自 sys_role.tenant_type_scope）：micro/enterprise/both */
    private String tenantTypeScope;

    /** 租户内显示名（如 salesman→"助教"） */
    private String displayName;

    /** 显示排序 */
    private Integer sortOrder;

    /** 状态（0启用 1禁用） */
    private String status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
