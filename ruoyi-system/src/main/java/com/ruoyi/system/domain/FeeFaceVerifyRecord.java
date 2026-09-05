package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 人脸核身记录对象 fee_face_verify_record（T7.2 / FEAT_FACE_RECOGNITION）
 * 大额/敏感大类触发 → 采集比对 → 存疑仅标记不拦截（strong 级证据）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_face_verify_record")
public class FeeFaceVerifyRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 关联报销单
     */
    private Long billId;

    /**
     * 核身用户
     */
    private Long userId;

    /**
     * 触发金额(默认阈值≥2000)
     */
    private BigDecimal amount;

    /**
     * 触发类型 AMOUNT_THRESHOLD/SENSITIVE_CATEGORY
     */
    private String triggerType;

    /**
     * 采集人脸图
     */
    private String faceImageUrl;

    /**
     * 入职底照
     */
    private String referenceImageUrl;

    /**
     * 比对结果 MATCH/MISMATCH/PENDING
     */
    private String result;

    /**
     * 比对相似度
     */
    private BigDecimal similarity;

    /**
     * 人脸存疑(进异常池)
     */
    private Integer isSuspicious;

    /**
     * 证据等级(strong)
     */
    private String evidenceLevel;

    /**
     * 核身时间
     */
    private Date verifyTime;
}
