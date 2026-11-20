package com.ruoyi.sms.numberauth;

import lombok.Data;

/**
 * 号码认证 H5 鉴权 Token（GetAuthToken 返回，下发前端 SDK 使用）
 * <p>accessToken 有效期 10 分钟、jwtToken 有效期 1 小时，有效期内可重复使用。
 *
 * @author ruoyi
 * @date 2026-10-25
 */
@Data
public class NumberAuthToken {

    /**
     * 业务鉴权 Token
     */
    private String accessToken;

    /**
     * API 鉴权 Token
     */
    private String jwtToken;

    /**
     * 探测失败原因（仅未开启/取 Token 失败时返回，成功时为 null）
     * <p>★前端探测接口约定 code=200 静默降级，失败原因改由本字段下发，供登录页展示定位（SDK/后端/运营商哪一段失败）。
     */
    private String failReason;

}
