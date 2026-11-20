package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 角色模板视图对象 role_template（T15.1/T15.7）
 * <p>系统级共享配方（version_type × role_code）；menu_keys/block_keys 为 JSON 字符串。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class RoleTemplateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 版本类型：micro/basic/advanced/professional */
    private String versionType;

    /** 标准角色编码（sys_role.role_key） */
    private String roleCode;

    /** 绑定菜单集合（perms 数组 JSON） */
    private String menuKeys;

    /** 绑定小程序区块集合（block_key 数组 JSON） */
    private String blockKeys;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
