package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 直播公告看板视图对象（T12.6）
 * <p>小程序/PC 首页直播位数据源：无可见公告时 hasAnnouncement=false（前端不展示直播位）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class LiveBoardVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否有可展示的直播/公告 */
    private boolean hasAnnouncement;

    /** 可见公告列表（living 优先，仅含派生展示态为可见的项） */
    private List<LiveAnnouncementVo> items = new ArrayList<>();
}
