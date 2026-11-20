package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 游客扫码登录响应视图对象（M31 T31.4/T31.5）
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Data
public class VisitorLoginVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** JWT token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 是否为新建账号（true=首次登录新建，false=复用已有临时账号） */
    private Boolean isNewAccount;

    /** 用户昵称 */
    private String nickName;

    /** 手机号 */
    private String phone;

    /** 临时账号状态：0=有效 1=已补全 2=已废弃 */
    private Integer visitorStatus;
}
