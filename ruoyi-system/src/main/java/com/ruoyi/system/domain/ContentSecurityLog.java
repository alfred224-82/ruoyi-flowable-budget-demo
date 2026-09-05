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
 * 内容安全审核日志对象 biz_content_security_log（T12.4 imgSecCheck / T14.4 msgSecCheck）
 * <p>审核不通过一律拒绝并留痕；表仅含 create_time，故不继承 BaseEntity。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("biz_content_security_log")
public class ContentSecurityLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 场景：logo/poster/ugc_image/ugc_text/nickname
     */
    private String scene;

    /**
     * 内容类型：image/text
     */
    private String contentType;

    /**
     * 内容引用（图片URL/文本摘要，文本脱敏存储）
     */
    private String contentRef;

    /**
     * 审核提供方：wechat_imgSecCheck/wechat_msgSecCheck/stub
     */
    private String provider;

    /**
     * 审核结果：PASS/REJECT/ERROR
     */
    private String result;

    /**
     * 命中标签（涉政/涉黄/广告等）
     */
    private String label;

    /**
     * 第三方原始响应（排障用，脱敏）
     */
    private String rawResponse;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
