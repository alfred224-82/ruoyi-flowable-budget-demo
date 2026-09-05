package com.ruoyi.system.service.wecom;

import lombok.Data;

import java.io.Serializable;

/**
 * 企微 OAuth 用户信息（T13.2）
 * <p>企微自建应用网页授权（snsapi_base/privateinfo）换取的用户身份；桩实现返回模拟值。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class WecomUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 企微成员 UserID */
    private String userId;

    /** 企微 CorpID */
    private String corpId;

    /** 成员名称 */
    private String name;

    /** 手机号（privateinfo 授权时返回，脱敏） */
    private String mobile;

    /** 是否企微成员（true=企业成员，false=外部联系人/游客） */
    private boolean member;
}
