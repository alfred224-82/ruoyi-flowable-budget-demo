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
 * 用户协议同意留痕对象 sys_user_agreement_log（T14.4）
 * <p>表仅含 agree_time，故不继承 BaseEntity；记录用户在某版本隐私政策/用户协议上的同意行为。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_user_agreement_log")
public class SysUserAgreementLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 微信 openid(小程序端) */
    private String openid;

    /** 协议类型：privacy/user */
    private String agreementType;

    /** 同意的版本号 */
    private String version;

    /** 来源IP */
    private String clientIp;

    /** 同意时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date agreeTime;
}
