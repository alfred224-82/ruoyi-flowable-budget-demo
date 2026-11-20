package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * License版本模板对象 sys_license_template（T18.14，小微版 M18）
 * <p>★平台级表（无 tenant_id），仅 Admin 管理。
 * <p>预置 BASIC/ADVANCED/PROFESSIONAL 三档 × TRIAL/FORMAL 两种授权类型。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_license_template")
public class SysLicenseTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 模板名称（如：基础版-正式） */
    private String templateName;

    /** 版本等级：BASIC/ADVANCED/PROFESSIONAL */
    private String edition;

    /** 授权类型：TRIAL(试用)/FORMAL(正式) */
    private String licenseType;

    /** 试用天数（仅TRIAL有效，NULL=不限） */
    private Integer trialDays;

    /** 适用行业列表JSON */
    private String industries;

    /** 最大用户数 */
    private Integer maxUsers;

    /** 最大角色数（NULL=不限） */
    private Integer maxRoles;

    /** 功能开关列表JSON */
    private String features;

    /** 状态：0启用 1停用 */
    private String status;

    /** 删除标志（0存在 2删除） */
    @TableLogic
    private String delFlag;

    private String remark;
}
