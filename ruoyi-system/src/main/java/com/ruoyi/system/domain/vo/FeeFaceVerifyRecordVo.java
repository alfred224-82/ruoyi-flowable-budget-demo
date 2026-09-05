package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 人脸核身记录视图对象（T7.2 / FEAT_FACE_RECOGNITION）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeFaceVerifyRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联报销单 */
    private Long billId;

    /** 报销单号（关联填充） */
    private String billNo;

    /** 核身用户 */
    private Long userId;

    /** 核身用户姓名（关联填充） */
    private String userName;

    /** 触发金额 */
    private BigDecimal amount;

    /** 触发类型 AMOUNT_THRESHOLD/SENSITIVE_CATEGORY */
    private String triggerType;

    /** 采集人脸图 */
    private String faceImageUrl;

    /** 入职底照 */
    private String referenceImageUrl;

    /** 比对结果 MATCH/MISMATCH/PENDING */
    private String result;

    /** 比对相似度 */
    private BigDecimal similarity;

    /** 人脸存疑(进异常池) */
    private Integer isSuspicious;

    /** 证据等级 */
    private String evidenceLevel;

    /** 核身时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date verifyTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
