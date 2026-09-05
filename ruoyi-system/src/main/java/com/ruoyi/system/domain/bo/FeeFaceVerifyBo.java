package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 人脸核身结果提交业务对象（T7.2 / FEAT_FACE_RECOGNITION）
 * 小程序/PC 采集比对后提交；不一致仅标记存疑不拦截
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeFaceVerifyBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联报销单（可空=登录核身等场景） */
    private Long billId;

    /** 核身用户（默认当前登录用户） */
    private Long userId;

    /** 触发金额 */
    private BigDecimal amount;

    /** 触发类型 AMOUNT_THRESHOLD/SENSITIVE_CATEGORY */
    @NotNull(message = "触发类型不能为空")
    private String triggerType;

    /** 采集人脸图URL */
    private String faceImageUrl;

    /** 入职底照URL */
    private String referenceImageUrl;

    /** 比对结果 MATCH/MISMATCH/PENDING */
    @NotNull(message = "比对结果不能为空")
    private String result;

    /** 比对相似度 */
    private BigDecimal similarity;
}
