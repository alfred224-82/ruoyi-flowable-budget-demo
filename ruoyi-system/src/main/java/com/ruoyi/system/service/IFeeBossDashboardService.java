package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.FeeDashboardVo;
import com.ruoyi.system.domain.vo.FeeLightOverviewVo;
import com.ruoyi.system.domain.vo.FeeMemberLoadVo;
import com.ruoyi.system.domain.vo.FeeProjectVo;

import java.util.List;

/**
 * 老板看板Service接口（T5.10）
 * 总览 + 大类卡片 + 四层下钻 + 项目卡片 + 风险预警区 + 固定成本卡片 + 未归类池
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeBossDashboardService {

    /**
     * 看板总数据（总览/大类/风险/固定成本/未归类池）
     */
    FeeDashboardVo getDashboard();

    /**
     * 项目卡片列表（含利润快照，按实际利润升序突出风险项目）
     */
    List<FeeProjectVo> listProjectCards();

    /**
     * 四层下钻：公司→大类→项目→明细（单据）
     *
     * @param level      层级 1公司 2大类 3项目 4明细
     * @param category   会计大类（2层及以下传入）
     * @param projectId  项目ID（3层及以下传入）
     */
    List<java.util.Map<String, Object>> drillDown(Integer level, String category, Long projectId);

    /**
     * 团队人力分布卡片（T7.5 / FEAT_MEMBER_LOAD）
     * 进行中项目成员按用户聚合：投入比例合计>100% 标记负载超限（仅提示不拦截）
     */
    List<FeeMemberLoadVo> memberLoad();

    /**
     * 轻量收支概览（T5.19④，基础版首页标配，不设功能开关）
     * 本月收入（已核验）/ 本月支出（已审批报销+付款）/ 本月结余，不含大类下钻
     */
    FeeLightOverviewVo getLightOverview();
}
