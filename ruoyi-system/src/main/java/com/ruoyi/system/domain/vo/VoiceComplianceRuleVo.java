package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 语音合规规则视图对象 voice_compliance_rule（T18.1/T18.3，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class VoiceComplianceRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private String activityType;
    private Boolean enabled;
    private String sensitiveWordOverride;
    private Boolean transcriptionEnabled;
    private Integer maxVoiceDuration;
    private String status;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
