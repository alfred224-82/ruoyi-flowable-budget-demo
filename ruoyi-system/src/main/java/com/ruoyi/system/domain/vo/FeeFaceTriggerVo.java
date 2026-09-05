package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 人脸核身触发判定结果（T7.2 / FEAT_FACE_RECOGNITION）
 * 报销提交前调用：判断是否需要人脸核身
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeFaceTriggerVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否需要人脸核身 */
    private Boolean required;

    /** 触发类型 AMOUNT_THRESHOLD/SENSITIVE_CATEGORY/NONE */
    private String triggerType;

    /** 触发原因说明 */
    private String reason;

    /** 当前配置的大额阈值 */
    private BigDecimal amountThreshold;
}
