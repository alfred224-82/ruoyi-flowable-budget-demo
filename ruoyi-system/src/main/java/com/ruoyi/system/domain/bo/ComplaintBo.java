package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 投诉提交业务对象（T25.1，小微版 M25）
 * <p>双渠道投诉：BOSS=渠道一（无额外门槛）/ PLATFORM=渠道二（拍照取证+文字≥20字）。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class ComplaintBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联活动ID（可空=不关联活动） */
    private Long activityId;

    /** 被投诉员工 user_id（可空=不指定员工） */
    private Long staffId;

    /** 被投诉员工姓名（冗余快照） */
    private String staffName;

    /** 投诉人手机号 */
    private String complainantPhone;

    /** 投诉人姓名 */
    private String complainantName;

    /** 投诉渠道：BOSS / PLATFORM */
    @NotNull(message = "投诉渠道不能为空", groups = {AddGroup.class})
    private String complaintChannel;

    /** 投诉类别（渠道二必填） */
    private String category;

    /** 投诉文字描述（渠道二要求≥20字） */
    @NotNull(message = "投诉内容不能为空", groups = {AddGroup.class})
    private String contentText;

    /** 附加图片URL列表（JSON 数组，渠道一可选≤3张） */
    private String photoUrls;

    /** 取证照片URL列表（JSON 数组，渠道二必填≥1张，仅摄像头拍摄） */
    private String evidencePhotos;
}
