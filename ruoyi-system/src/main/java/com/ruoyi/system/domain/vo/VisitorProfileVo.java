package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 游客信息补全视图对象（M31 T31.6）
 * <p>Vo 类约定：仅 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Data
public class VisitorProfileVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 手机号后 4 位（temp_visitor_account.phone_last4） */
    private String phoneLast4;

    /** 用户昵称（sys_user.nick_name） */
    private String nickName;

    /** 手机号（sys_user.phonenumber） */
    private String phone;

    /** 真实姓名 */
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 生日 */
    private Date birthday;

    /** 性别：1=男 2=女 */
    private Integer gender;

    /** 地址 */
    private String address;

    /** 微信 openid */
    private String wechatOpenid;

    /** 支付宝 user_id */
    private String alipayUserId;

    /** 信息是否完善：0=未完善 1=已完善 */
    private Integer isComplete;

    /** 临时账号状态：0=有效 1=已补全 2=已废弃 */
    private Integer visitorStatus;

    /** 临时账号过期时间 */
    private Date expireTime;

    /** 创建时间 */
    private Date createTime;
}
