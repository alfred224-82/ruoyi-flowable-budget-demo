package com.ruoyi.system.service.wx;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.io.Serializable;

/**
 * 微信 code2session 返回结果（T13.1）
 * <p>session_key 属敏感凭据，仅服务端留存，【严禁】下发前端或写入日志。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class WxCode2SessionResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户唯一标识 openid
     */
    private String openid;

    /**
     * 会话密钥（敏感，仅服务端留存）
     */
    private String sessionKey;

    /**
     * 开放平台唯一标识 unionid（同一主体下多应用时返回）
     */
    private String unionid;

    /**
     * 错误码（0 或 null 表示成功）
     */
    private Integer errcode;

    /**
     * 错误信息
     */
    private String errmsg;

    /**
     * 是否成功：无错误码且拿到 openid
     */
    public boolean isSuccess() {
        return (errcode == null || errcode == 0) && StrUtil.isNotBlank(openid);
    }
}
