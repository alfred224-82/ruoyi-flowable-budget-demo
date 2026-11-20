package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 合规命中记录对象 voice_compliance_hit（T18.1，小微版 M18）
 * <p>租户级合规命中记录：记录语音内容触发合规规则的明细。
 * <p>老板只读查看（T18.4），Admin/租户管理员可查看各自范围。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice_compliance_hit")
public class VoiceComplianceHit extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 命中时间 */
    private Date hitTime;

    /** 命中类型：SENSITIVE_WORD/NO_TRANSCRIPTION/POLICY_VIOLATION */
    private String hitType;

    /** 合规结果：PASS(通过)/FAIL(违规) */
    private String result;

    /** 内容摘要（脱敏后的语音转写片段） */
    private String contentSummary;

    /** 关联规则ID（voice_compliance_rule.id） */
    private Long ruleId;

    /** 关联活动ID */
    private Long activityId;

    /** 处理状态：PENDING/ACKNOWLEDGED/DISMISSED */
    private String status;

    private String remark;
}
