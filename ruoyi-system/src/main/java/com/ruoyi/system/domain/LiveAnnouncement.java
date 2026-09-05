package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 直播公告对象 biz_live_announcement（T12.6 状态机）
 * <p>业务状态 status：PENDING(预告)/LIVING(直播中)/OFFLINE(已结束)；
 * 展示态由服务端按 now 与 start_time/end_time 派生（living 排第一 / pending≤30min 临近提醒 /
 * pending&gt;30min 隐藏 / offline≤10min 可进回放 / offline&gt;10min 隐藏）；无直播时接口返回 has_announcement=false。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_live_announcement")
public class LiveAnnouncement extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联活动容器ID（project.id, type=activity）
     */
    private Long projectId;

    /**
     * 平台：video_account(视频号,可跳转)/douyin(抖音,仅引流不跳转)
     */
    private String platform;

    /**
     * 直播/公告标题
     */
    private String title;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 直播间外链（抖音引流/回放）
     */
    private String liveUrl;

    /**
     * 视频号直播间ID（navigateToChannelsLive 跳转用）
     */
    private String roomId;

    /**
     * 主播/发起人
     */
    private String anchor;

    /**
     * 开播时间
     */
    private Date startTime;

    /**
     * 下播时间（实际）
     */
    private Date endTime;

    /**
     * 业务状态：PENDING/LIVING/OFFLINE
     */
    private String status;

    /**
     * 排序（living 优先）
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
