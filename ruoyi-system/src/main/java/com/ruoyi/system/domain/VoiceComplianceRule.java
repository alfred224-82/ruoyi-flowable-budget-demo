package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 语音合规规则对象 voice_compliance_rule（T18.1，小微版 M18）
 * <p>租户级合规规则：按 activity_type 控制语音合规开关 + 敏感词覆盖策略。
 * <p>UNIQUE(tenant_id, activity_type)：每租户每活动类型一条规则。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice_compliance_rule")
public class VoiceComplianceRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 活动类型（继承行业默认） */
    private String activityType;

    /** 语音合规开关（0=关闭 1=开启） */
    private Boolean enabled;

    /** 敏感词策略：INHERIT/OVERRIDE/MERGE */
    private String sensitiveWordOverride;

    /** 转写留痕开关（0=关闭 1=开启） */
    private Boolean transcriptionEnabled;

    /** 最大语音时长（秒；默认180=3分钟；0=不限） */
    private Integer maxVoiceDuration;

    /** 状态：NORMAL/DISABLED */
    private String status;

    private String remark;
}
