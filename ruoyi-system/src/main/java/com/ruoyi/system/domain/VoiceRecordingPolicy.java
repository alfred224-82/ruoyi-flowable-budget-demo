package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 录音留存策略对象 voice_recording_policy（T18.1，小微版 M18）
 * <p>租户级录音策略：留存天数 + 存储路径（≤ Admin 全局上限）。
 * <p>UNIQUE(tenant_id)：每租户一条策略。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("voice_recording_policy")
public class VoiceRecordingPolicy extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 最大留存天数（≤ Admin 全局上限） */
    private Integer maxRetentionDays;

    /** 录音存储路径前缀 */
    private String storagePath;

    private String remark;
}
