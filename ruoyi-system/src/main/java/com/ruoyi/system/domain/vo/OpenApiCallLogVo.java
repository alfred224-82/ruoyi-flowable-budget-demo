package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 开放平台API调用审计视图对象 open_api_call_log（T13.5）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class OpenApiCallLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 凭证ID */
    private Long credentialId;

    /** AppKey */
    private String appKey;

    /** 接口路径 */
    private String apiPath;

    /** HTTP方法 */
    private String httpMethod;

    /** 请求参数(脱敏) */
    private String requestParam;

    /** 响应码 */
    private Integer responseCode;

    /** 响应体(截断) */
    private String responseBody;

    /** 耗时(毫秒) */
    private Long costMs;

    /** 来源IP */
    private String clientIp;

    /** 是否成功(0否 1是) */
    private Integer success;

    /** 失败原因 */
    private String errorMsg;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
