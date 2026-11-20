package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.BossDashboardVo;

/**
 * 老板经营驾驶舱首页服务接口（T18.7，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface IBossDashboardService {

    /**
     * 获取老板经营驾驶舱聚合数据
     * <p>包含：核心经营数据 / 成本构成 / 业务待办
     */
    BossDashboardVo getDashboard();
}
