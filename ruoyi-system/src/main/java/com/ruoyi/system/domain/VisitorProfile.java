package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 游客信息补全对象 visitor_profile（M31 T31.3/T31.6）
 * <p>与 sys_user 一对一关联（user_id 为主键），记录游客补全的实名信息。
 * <p>租户级表（含 tenant_id 由 {@link TenantEntity} 自动填充）。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("visitor_profile")
public class VisitorProfile extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 = FK→sys_user.user_id */
    @TableId(value = "user_id")
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 身份证号（脱敏存储） */
    private String idCard;

    /** 生日 */
    private Date birthday;

    /** 性别：1=男 2=女 */
    private Integer gender;

    /** 地址 */
    private String address;

    /** 微信 openid（升级正式账号时绑定） */
    private String wechatOpenid;

    /** 支付宝 user_id（升级正式账号时绑定） */
    private String alipayUserId;

    /** 信息是否完善：0=未完善 1=已完善 */
    private Integer isComplete;
}
