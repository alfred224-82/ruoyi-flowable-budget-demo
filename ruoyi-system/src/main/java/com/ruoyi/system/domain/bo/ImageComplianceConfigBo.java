package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 图片合规配置业务对象 image_compliance_config（M18 T18.1）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class ImageComplianceConfigBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 违规类型：porn/violence/politics */
    @NotNull(message = "违规类型不能为空")
    private String violationType;

    /** 是否启用该类型检测（0关闭 1启用） */
    private Integer enabled;

    /** 置信度阈值（0.00~1.00） */
    @DecimalMin(value = "0.00", message = "置信度阈值不能小于0")
    @DecimalMax(value = "1.00", message = "置信度阈值不能大于1")
    private BigDecimal confidenceThreshold;

    /** 是否启用 OCR 文字提取（0关闭 1启用） */
    private Integer ocrEnabled;

    /** 是否启用视觉审核 API（0关闭 1启用） */
    private Integer visualApiEnabled;

    /** 最大文件大小（MB） */
    private Integer maxFileSizeMb;

    /** 允许的格式（逗号分隔） */
    private String allowedFormats;

    private String remark;
}
