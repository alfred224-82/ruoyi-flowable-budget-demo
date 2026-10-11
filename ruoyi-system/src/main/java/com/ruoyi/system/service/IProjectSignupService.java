package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ProjectSignupBo;
import com.ruoyi.system.domain.vo.ProjectSignupVo;

import java.util.Map;

/**
 * 项目活动报名服务接口（T11.3）
 * <p>报名采集 + 满员候补；成团判定已移除（M22 简化），前端根据 quota/remaining 控制按钮灰显。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IProjectSignupService {

    /** 分页查询报名（按容器/签到状态/候补过滤） */
    TableDataInfo<ProjectSignupVo> queryPageList(ProjectSignupBo bo, PageQuery pageQuery);

    /**
     * 报名登记：满员进候补
     *
     * @return {signupId, waitlist, quota, message}
     */
    Map<String, Object> signup(ProjectSignupBo bo);

    /** 取消报名（后台管理用，无时间限制） */
    int cancel(Long id);

    /**
     * 顾客端取消报名（活动开始前 1 小时可取消，校验归属 + 时间窗口）
     *
     * @param signupId   报名记录ID
     * @param activityId 活动ID（用于获取活动开始时间）
     */
    void cancelByActivityId(Long signupId, Long activityId);

    /** 签到（NOT_SIGNED→SIGNED/ABSENT） */
    int checkin(Long id, String signInStatus);

    /**
     * 查询容器当前成团状态（策略实时判定，不写库）
     *
     * @param projectId 服务项目ID
     * @return 成团状态信息
     */
    Map<String, Object> teamStatus(Long projectId);
}
