package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.TenantOverviewVo;

/**
 * 租户管理员企业概览首页服务接口（T18.6，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface ITenantOverviewService {

    /**
     * 获取当前租户概览数据
     * <p>包含：企业基本信息 / 配置健康度自检 / 系统公告 / 快捷入口
     */
    TenantOverviewVo getOverview();
}
