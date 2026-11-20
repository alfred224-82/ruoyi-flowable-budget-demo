package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 录音留存策略业务对象 voice_recording_policy（T18.1/T18.3，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class VoiceRecordingPolicyBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 最大留存天数（≤ Admin 全局上限） */
    private Integer maxRetentionDays;

    /** 录音存储路径前缀 */
    private String storagePath;

    private String remark;
}
