package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 人脸核身配置业务对象（T7.2 / FEAT_FACE_RECOGNITION）
 * 存储于 sys_tenant_config（configKey=face_verify_config）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeFaceConfigBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否启用人脸核身 */
    private Boolean enabled;

    /** 大额触发阈值（默认2000，金额≥阈值触发） */
    private BigDecimal amountThreshold;

    /** 相似度低于该值判定存疑（默认0.6） */
    private BigDecimal suspiciousSimilarity;

    /** 敏感大类列表（提交时命中即触发，如 差旅招待/业务招待） */
    private List<String> sensitiveCategories;
}
