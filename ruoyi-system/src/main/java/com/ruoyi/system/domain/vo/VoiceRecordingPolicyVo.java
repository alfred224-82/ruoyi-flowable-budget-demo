package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 录音留存策略视图对象 voice_recording_policy（T18.1/T18.3，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class VoiceRecordingPolicyVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Integer maxRetentionDays;
    private String storagePath;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
