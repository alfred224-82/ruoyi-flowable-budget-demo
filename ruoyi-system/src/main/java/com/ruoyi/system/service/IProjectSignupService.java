package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ProjectSignupBo;
import com.ruoyi.system.domain.vo.ProjectSignupVo;
import com.ruoyi.system.service.strategy.TeamResult;

/**
 * 项目活动报名服务接口（T11.3 / T11.4）
 * <p>报名采集 + 满员候补 + 成团判定（委派 ITeamStrategy 策略，引擎不感知业态）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IProjectSignupService {

    /** 分页查询报名（按容器/签到状态/候补过滤） */
    TableDataInfo<ProjectSignupVo> queryPageList(ProjectSignupBo bo, PageQuery pageQuery);

    /**
     * 报名登记：满员进候补，登记后触发成团策略判定
     *
     * @return 成团判定结果
     */
    TeamResult signup(ProjectSignupBo bo);

    /** 取消报名 */
    int cancel(Long id);

    /** 签到（NOT_SIGNED→SIGNED/ABSENT） */
    int checkin(Long id, String signInStatus);

    /**
     * 查询容器当前成团状态（不写库，仅按策略实时判定）
     */
    TeamResult teamStatus(Long projectId);
}
