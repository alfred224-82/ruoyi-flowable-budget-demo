package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 内容安全审核日志视图对象 biz_content_security_log（T12.4）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ContentSecurityLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 场景 */
    private String scene;

    /** 内容类型：image/text */
    private String contentType;

    /** 内容引用 */
    private String contentRef;

    /** 审核提供方 */
    private String provider;

    /** 审核结果：PASS/REJECT/ERROR */
    private String result;

    /** 命中标签 */
    private String label;

    private Date createTime;
}
