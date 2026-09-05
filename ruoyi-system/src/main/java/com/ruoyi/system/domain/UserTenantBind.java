package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户租户绑定对象 user_tenant_bind（T1.7 / T10.1 / T13.1）
 * <p>openid 多租户归属；登录必须显式选定门店，禁止按 openid 自动猜测租户（双阶段租户识别）。
 * 表仅含 create_time/update_time（无 create_by/update_by），故不继承 BaseEntity，
 * tenant_id 由服务层在引导态显式写入（此时 TenantContext 未设置，属系统级共享查询）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("user_tenant_bind")
public class UserTenantBind implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 微信 openid
     */
    private String openid;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 绑定的系统用户ID（可空=仅引导态绑定，登录后回填）
     */
    private Long userId;

    /**
     * 是否默认门店（0否 1是；多绑定时登录页仍须显式选定）
     */
    private Integer isDefault;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
