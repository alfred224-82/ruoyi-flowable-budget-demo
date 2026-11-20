package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 合规命中记录视图对象 voice_compliance_hit（T18.1/T18.4，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class VoiceComplianceHitVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date hitTime;

    private String hitType;
    private String result;
    private String contentSummary;
    private Long ruleId;
    private Long activityId;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
