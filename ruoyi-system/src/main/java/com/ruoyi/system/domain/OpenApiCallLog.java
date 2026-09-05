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
 * 开放平台API调用审计对象 open_api_call_log（T13.5 全量调用审计）
 * <p>表仅含 create_time，故不继承 BaseEntity。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("open_api_call_log")
public class OpenApiCallLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 凭证ID(open_api_credential) */
    private Long credentialId;

    /** AppKey(冗余，凭证删除后仍可追溯) */
    private String appKey;

    /** 接口路径 */
    private String apiPath;

    /** HTTP方法(GET/POST/...) */
    private String httpMethod;

    /** 请求参数(脱敏) */
    private String requestParam;

    /** 响应码(200/401/429等) */
    private Integer responseCode;

    /** 响应体(截断) */
    private String responseBody;

    /** 耗时(毫秒) */
    private Long costMs;

    /** 来源IP */
    private String clientIp;

    /** 是否成功(0否 1是) */
    private Integer success;

    /** 失败原因(签名错误/超配额/...) */
    private String errorMsg;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
