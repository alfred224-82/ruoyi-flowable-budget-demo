package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 行业角色映射业务对象 industry_role_map（T15.10 ② 配置角色映射）
 * <p>业务角色名 → 标准角色 code；同行业业务角色名唯一（uk_industry_bizrole）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class IndustryRoleMapBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "行业角色映射ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 行业编码 */
    @NotBlank(message = "行业编码不能为空", groups = {AddGroup.class})
    private String industryCode;

    /** 业务角色显示名（桌游老板/助教/顾客...） */
    @NotBlank(message = "业务角色名不能为空", groups = {AddGroup.class})
    private String bizRoleName;

    /** 映射到的标准角色 code（boss/salesman/customer） */
    @NotBlank(message = "标准角色编码不能为空", groups = {AddGroup.class})
    private String roleCode;

    /** 是否该行业默认启用角色（0否 1是） */
    private Integer defaultFlag;

    /** 排序 */
    private Integer sortOrder;
}
