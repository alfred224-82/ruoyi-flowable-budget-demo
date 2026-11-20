package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 语音合规规则业务对象 voice_compliance_rule（T18.1/T18.3，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class VoiceComplianceRuleBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 活动类型（★由后端从 sys_tenant.industry 自动填充，前端不传） */
    private String activityType;

    /** 语音合规开关 */
    private Boolean enabled;

    /** 敏感词策略：INHERIT/OVERRIDE/MERGE */
    private String sensitiveWordOverride;

    /** 转写留痕开关 */
    private Boolean transcriptionEnabled;

    /** 最大语音时长（秒；默认180=3分钟；0=不限） */
    private Integer maxVoiceDuration;

    /** 状态：NORMAL/DISABLED */
    private String status;

    private String remark;
}
