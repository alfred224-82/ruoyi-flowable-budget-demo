package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.LiveAnnouncement;
import com.ruoyi.system.domain.bo.LiveAnnouncementBo;
import com.ruoyi.system.domain.vo.LiveAnnouncementVo;
import com.ruoyi.system.domain.vo.LiveBoardVo;
import com.ruoyi.system.mapper.LiveAnnouncementMapper;
import com.ruoyi.system.service.ILiveAnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 直播公告服务实现（T12.6 状态机）
 * <p>业务状态由运营维护，展示态由服务端实时派生（不落库）；租户隔离由多租户插件自动处理。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiveAnnouncementServiceImpl implements ILiveAnnouncementService {

    private final LiveAnnouncementMapper announcementMapper;

    /** 临近开播提醒阈值：30 分钟 */
    private static final long PENDING_SOON_MILLIS = 30L * 60 * 1000;
    /** 下播可进回放阈值：10 分钟 */
    private static final long OFFLINE_REPLAY_MILLIS = 10L * 60 * 1000;

    @Override
    public TableDataInfo<LiveAnnouncementVo> queryPageList(LiveAnnouncementBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<LiveAnnouncement> wrapper = Wrappers.<LiveAnnouncement>lambdaQuery()
            .eq(bo.getProjectId() != null, LiveAnnouncement::getProjectId, bo.getProjectId())
            .eq(StrUtil.isNotBlank(bo.getPlatform()), LiveAnnouncement::getPlatform, bo.getPlatform())
            .eq(StrUtil.isNotBlank(bo.getStatus()), LiveAnnouncement::getStatus, bo.getStatus())
            .like(StrUtil.isNotBlank(bo.getTitle()), LiveAnnouncement::getTitle, bo.getTitle())
            .orderByAsc(LiveAnnouncement::getSort)
            .orderByDesc(LiveAnnouncement::getStartTime);
        Page<LiveAnnouncementVo> page = announcementMapper.selectVoPage(pageQuery.build(), wrapper);
        page.getRecords().forEach(this::fillDerived);
        return TableDataInfo.build(page);
    }

    @Override
    public LiveAnnouncementVo queryById(Long id) {
        LiveAnnouncementVo vo = announcementMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("直播公告不存在");
        }
        fillDerived(vo);
        return vo;
    }

    @Override
    public Long insert(LiveAnnouncementBo bo) {
        LiveAnnouncement entity = BeanCopyUtils.copy(bo, LiveAnnouncement.class);
        entity.setId(null);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus(STATUS_PENDING);
        }
        if (StrUtil.isBlank(entity.getPlatform())) {
            entity.setPlatform(PLATFORM_VIDEO_ACCOUNT);
        }
        announcementMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int update(LiveAnnouncementBo bo) {
        LiveAnnouncement exist = announcementMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("直播公告不存在");
        }
        LiveAnnouncement entity = BeanCopyUtils.copy(bo, LiveAnnouncement.class);
        return announcementMapper.updateById(entity);
    }

    @Override
    public int startLiving(Long id) {
        LiveAnnouncement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("直播公告不存在");
        }
        if (STATUS_OFFLINE.equals(exist.getStatus())) {
            throw new ServiceException("已结束的公告不可再次开播");
        }
        LiveAnnouncement update = new LiveAnnouncement();
        update.setId(id);
        update.setStatus(STATUS_LIVING);
        return announcementMapper.updateById(update);
    }

    @Override
    public int offline(Long id) {
        LiveAnnouncement exist = announcementMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("直播公告不存在");
        }
        if (!STATUS_LIVING.equals(exist.getStatus())) {
            throw new ServiceException("仅直播中的公告可下播");
        }
        LiveAnnouncement update = new LiveAnnouncement();
        update.setId(id);
        update.setStatus(STATUS_OFFLINE);
        update.setEndTime(new Date());
        return announcementMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return announcementMapper.deleteBatchIds(ids);
    }

    @Override
    public LiveBoardVo currentBoard(Long projectId) {
        LiveBoardVo board = new LiveBoardVo();
        List<LiveAnnouncementVo> all = listByProject(projectId);
        List<LiveAnnouncementVo> visible = new ArrayList<>();
        for (LiveAnnouncementVo vo : all) {
            if (isVisible(vo.getDisplayState())) {
                visible.add(vo);
            }
        }
        // LIVING 排第一，其余按 sort 升序、开播时间降序
        visible.sort(Comparator
            .comparing((LiveAnnouncementVo v) -> DISPLAY_LIVING.equals(v.getDisplayState()) ? 0 : 1)
            .thenComparing(v -> v.getSort() == null ? Integer.MAX_VALUE : v.getSort())
            .thenComparing(v -> v.getStartTime() == null ? new Date(0) : v.getStartTime(), Comparator.reverseOrder()));
        board.setItems(visible);
        board.setHasAnnouncement(!visible.isEmpty());
        return board;
    }

    @Override
    public List<LiveAnnouncementVo> listByProject(Long projectId) {
        List<LiveAnnouncementVo> list = announcementMapper.selectVoList(
            Wrappers.<LiveAnnouncement>lambdaQuery()
                .eq(projectId != null, LiveAnnouncement::getProjectId, projectId)
                .orderByAsc(LiveAnnouncement::getSort)
                .orderByDesc(LiveAnnouncement::getStartTime));
        list.forEach(this::fillDerived);
        return list;
    }

    /**
     * 填充服务端派生字段：displayState 与 canNavigate（不落库）
     */
    private void fillDerived(LiveAnnouncementVo vo) {
        vo.setDisplayState(deriveDisplayState(vo));
        vo.setCanNavigate(PLATFORM_VIDEO_ACCOUNT.equals(vo.getPlatform()));
    }

    /**
     * 派生展示态（T12.6 状态机核心）
     */
    private String deriveDisplayState(LiveAnnouncementVo vo) {
        long now = System.currentTimeMillis();
        String status = vo.getStatus();
        if (STATUS_LIVING.equals(status)) {
            return DISPLAY_LIVING;
        }
        if (STATUS_PENDING.equals(status)) {
            Date start = vo.getStartTime();
            if (start != null && start.getTime() - now <= PENDING_SOON_MILLIS) {
                return DISPLAY_PENDING_SOON;
            }
            return DISPLAY_PENDING_HIDE;
        }
        if (STATUS_OFFLINE.equals(status)) {
            Date end = vo.getEndTime();
            if (end != null && now - end.getTime() <= OFFLINE_REPLAY_MILLIS) {
                return DISPLAY_OFFLINE_REPLAY;
            }
            return DISPLAY_OFFLINE_HIDE;
        }
        // 未知状态一律隐藏
        return DISPLAY_PENDING_HIDE;
    }

    /**
     * 展示态是否对 C 端可见
     */
    private boolean isVisible(String displayState) {
        return DISPLAY_LIVING.equals(displayState)
            || DISPLAY_PENDING_SOON.equals(displayState)
            || DISPLAY_OFFLINE_REPLAY.equals(displayState);
    }
}
