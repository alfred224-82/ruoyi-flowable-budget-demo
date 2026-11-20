package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.AdminDashboardVo;

/**
 * Admin 平台驾驶舱首页服务接口（T18.5，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface IAdminDashboardService {

    /**
     * 获取 Admin 驾驶舱聚合数据
     * <p>包含：租户大盘 / 系统健康度 / 全局合规风险监控
     */
    AdminDashboardVo getDashboard();
}
