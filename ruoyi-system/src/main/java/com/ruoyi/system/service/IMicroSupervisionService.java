package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.ComplaintRecordVo;

import java.util.List;
import java.util.Map;

/**
 * 平台监管服务接口（T25.4，小微版 M25）
 * <p>platform_admin 专属：查看全部投诉 / 停止活动 / 停止租户 / 发放 license / 活动留痕。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
public interface IMicroSupervisionService {

    /**
     * 全部投诉列表（平台 Admin 跨租户查看）
     *
     * @param channel    渠道筛选（可空）
     * @param status     状态筛选（可空）
     * @param tenantId   租户筛选（可空）
     */
    List<ComplaintRecordVo> listAllComplaints(String channel, String status, Long tenantId);

    /**
     * 停止活动（T25.4）：将活动状态置为已停止
     *
     * @param activityId 活动ID
     */
    void stopActivity(Long activityId);

    /**
     * 停止租户（T25.6）：将租户状态置为 SUSPENDED
     *
     * @param tenantId 租户ID
     */
    void stopTenant(Long tenantId);

    /**
     * 恢复租户（将租户状态恢复为 ACTIVE）
     *
     * @param tenantId 租户ID
     */
    void resumeTenant(Long tenantId);

    /**
     * 发放 license（T25.4）
     *
     * @param params 发放参数（tenantId/licenseType/expireDate 等）
     * @return 结果描述
     */
    String issueLicense(Map<String, Object> params);

    /**
     * 活动留痕查询（T25.6 平台 Admin 查看租户操作日志，租户不可删除）
     *
     * @param tenantId 租户ID（可空=全部）
     * @param limit    返回条数限制（默认100）
     */
    List<Map<String, Object>> activityTrace(Long tenantId, Integer limit);
}
