package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;

/**
 * 直播公告业务对象 biz_live_announcement（T12.6）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class LiveAnnouncementBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "公告ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 关联活动容器ID */
    private Long projectId;

    /** 平台：video_account/douyin */
    private String platform;

    /** 标题 */
    @NotBlank(message = "标题不能为空", groups = {AddGroup.class})
    private String title;

    /** 封面图URL */
    private String coverUrl;

    /** 直播间外链 */
    private String liveUrl;

    /** 视频号直播间ID */
    private String roomId;

    /** 主播/发起人 */
    private String anchor;

    /** 开播时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 下播时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 业务状态：PENDING/LIVING/OFFLINE */
    private String status;

    /** 排序 */
    private Integer sort;

    /** 备注 */
    private String remark;
}
