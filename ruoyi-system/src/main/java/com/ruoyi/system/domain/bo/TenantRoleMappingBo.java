package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 租户角色映射业务对象 tenant_role_mapping（T15.9）
 * <p>平台管理员：挑选（roleCode+displayName 新增）/改名（displayName）/启停（status）/排序（sortOrder）。
 * ★不含菜单权限字段——平台管理员改不了角色权限（菜单/区块由 admin 在 role_template 预置）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class TenantRoleMappingBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "角色映射ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 标准角色编码（sys_role.role_key，挑选时必填） */
    @NotBlank(message = "标准角色编码不能为空", groups = {AddGroup.class})
    private String roleCode;

    /** 租户内显示名（如 salesman→"助教"） */
    @NotBlank(message = "角色显示名不能为空", groups = {AddGroup.class})
    private String displayName;

    /** 显示排序 */
    private Integer sortOrder;

    /** 状态（0启用 1禁用） */
    private String status;

    /** 备注 */
    private String remark;
}
