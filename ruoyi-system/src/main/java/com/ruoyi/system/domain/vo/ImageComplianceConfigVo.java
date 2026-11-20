package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 图片合规配置视图对象 image_compliance_config（M18 T18.1）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class ImageComplianceConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    /** 租户ID（0=全局默认） */
    private Long tenantId;
    /** 违规类型：porn/violence/politics */
    private String violationType;
    /** 是否启用（0关闭 1启用） */
    private Integer enabled;
    /** 置信度阈值 */
    private BigDecimal confidenceThreshold;
    /** 是否启用 OCR（0关闭 1启用） */
    private Integer ocrEnabled;
    /** 是否启用视觉审核 API（0关闭 1启用） */
    private Integer visualApiEnabled;
    /** 最大文件大小（MB） */
    private Integer maxFileSizeMb;
    /** 允许的格式（逗号分隔） */
    private String allowedFormats;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
