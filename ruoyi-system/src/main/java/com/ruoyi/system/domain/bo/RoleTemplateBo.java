package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 角色模板业务对象 role_template（T15.7，admin 维护系统级配方）
 * <p>按 version_type × role_code 绑定 menu_keys / block_keys；uk_version_role 保证同版本同角色唯一。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class RoleTemplateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "角色模板ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 版本类型：micro/basic/advanced/professional */
    @NotBlank(message = "版本类型不能为空", groups = {AddGroup.class})
    private String versionType;

    /** 标准角色编码（sys_role.role_key） */
    @NotBlank(message = "标准角色编码不能为空", groups = {AddGroup.class})
    private String roleCode;

    /** 绑定菜单集合（perms 数组 JSON） */
    private String menuKeys;

    /** 绑定小程序区块集合（block_key 数组 JSON） */
    private String blockKeys;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;
}
