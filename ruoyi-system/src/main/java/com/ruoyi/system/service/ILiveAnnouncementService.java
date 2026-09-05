package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.LiveAnnouncementBo;
import com.ruoyi.system.domain.vo.LiveAnnouncementVo;
import com.ruoyi.system.domain.vo.LiveBoardVo;

import java.util.Collection;
import java.util.List;

/**
 * 直播公告服务接口（T12.6 状态机）
 * <p>业务状态 status（PENDING/LIVING/OFFLINE）由后台运营维护；
 * 展示态 displayState 由服务端按 now 与 start_time/end_time 实时派生，不落库：
 * <ul>
 *   <li>LIVING → LIVING（可见，排第一）</li>
 *   <li>PENDING 且 start_time − now ≤ 30min → PENDING_SOON（可见，临近提醒）；否则 PENDING_HIDE（隐藏）</li>
 *   <li>OFFLINE 且 now − end_time ≤ 10min → OFFLINE_REPLAY（可见，可进回放）；否则 OFFLINE_HIDE（隐藏）</li>
 * </ul>
 * canNavigate：platform=video_account → true；douyin → false（仅引流不跳转）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ILiveAnnouncementService {

    /** 展示态：直播中（可见） */
    String DISPLAY_LIVING = "LIVING";
    /** 展示态：临近开播（≤30min，可见） */
    String DISPLAY_PENDING_SOON = "PENDING_SOON";
    /** 展示态：预告隐藏（>30min，不可见） */
    String DISPLAY_PENDING_HIDE = "PENDING_HIDE";
    /** 展示态：可进回放（下播≤10min，可见） */
    String DISPLAY_OFFLINE_REPLAY = "OFFLINE_REPLAY";
    /** 展示态：已结束隐藏（下播>10min，不可见） */
    String DISPLAY_OFFLINE_HIDE = "OFFLINE_HIDE";

    /** 业务状态：预告 */
    String STATUS_PENDING = "PENDING";
    /** 业务状态：直播中 */
    String STATUS_LIVING = "LIVING";
    /** 业务状态：已结束 */
    String STATUS_OFFLINE = "OFFLINE";

    /** 平台：视频号（可跳转直播间） */
    String PLATFORM_VIDEO_ACCOUNT = "video_account";
    /** 平台：抖音（仅引流不跳转） */
    String PLATFORM_DOUYIN = "douyin";

    /**
     * 后台分页查询（含派生展示态，供运营管理）
     */
    TableDataInfo<LiveAnnouncementVo> queryPageList(LiveAnnouncementBo bo, PageQuery pageQuery);

    /**
     * 详情（含派生展示态与 canNavigate）
     */
    LiveAnnouncementVo queryById(Long id);

    /**
     * 新建公告（默认 status=PENDING）
     *
     * @return 公告ID
     */
    Long insert(LiveAnnouncementBo bo);

    /**
     * 编辑公告
     */
    int update(LiveAnnouncementBo bo);

    /**
     * 开播（PENDING→LIVING）
     */
    int startLiving(Long id);

    /**
     * 下播（LIVING→OFFLINE，回写 end_time=now）
     */
    int offline(Long id);

    /**
     * 逻辑删除（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * C 端看板：仅返回派生展示态为可见的公告（LIVING/PENDING_SOON/OFFLINE_REPLAY），
     * LIVING 排第一；无可见公告时 hasAnnouncement=false。
     *
     * @param projectId 活动容器ID（可空，空则取租户全部）
     */
    LiveBoardVo currentBoard(Long projectId);

    /**
     * 按业务状态查询列表（含派生展示态），供内部/看板复用
     */
    List<LiveAnnouncementVo> listByProject(Long projectId);
}
