package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 直播公告视图对象 biz_live_announcement（T12.6）
 * <p>额外携带服务端派生的展示态 displayState 与是否可跳转 canNavigate。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class LiveAnnouncementVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联活动容器ID */
    private Long projectId;

    /** 平台：video_account/douyin */
    private String platform;

    /** 标题 */
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

    /** 派生展示态：LIVING/PENDING_SOON/PENDING_HIDE/OFFLINE_REPLAY/OFFLINE_HIDE（服务端计算，非表字段） */
    private String displayState;

    /** 是否可跳转直播间（视频号 true / 抖音 false，非表字段） */
    private Boolean canNavigate;

    private Date createTime;
}
