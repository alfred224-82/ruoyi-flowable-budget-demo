package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.VisitorProfileBo;
import com.ruoyi.system.domain.vo.VisitorProfileVo;

import java.util.List;

/**
 * 游客信息补全服务接口（M31 T31.6/T31.7/T31.8）
 * <p>Boss 补全游客信息 + 游客自助完善 + 升级正式顾客 + 批量转正。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
public interface IVisitorProfileService {

    /**
     * Boss 查询临时游客列表（status=TEMPORARY + is_complete=0）
     *
     * @param pageNum  页码
     * @param pageSize 每页数量
     * @return 临时游客列表
     */
    List<VisitorProfileVo> listTempVisitors(int pageNum, int pageSize);

    /**
     * Boss 补全游客信息
     *
     * @param bo 游客信息
     */
    void updateVisitorProfile(VisitorProfileBo bo);

    /**
     * 游客查询自己的补全状态
     *
     * @param userId 用户ID
     * @return 游客信息
     */
    VisitorProfileVo getMyProfile(Long userId);

    /**
     * 游客自助完善信息
     *
     * @param userId 用户ID
     * @param bo     游客信息
     */
    void updateMyProfile(Long userId, VisitorProfileBo bo);

    /**
     * 游客主动升级为正式顾客（T31.7）
     *
     * @param userId 用户ID
     * @param bo     完整信息（含 realName/idCard/birthday/gender/address/wechatOpenid/alipayUserId）
     */
    void upgradeToFormal(Long userId, VisitorProfileBo bo);

    /**
     * Boss 批量转正临时游客（T31.8）
     *
     * @param userIds 用户ID列表
     * @return {successCount, failCount, failReasons}
     */
    java.util.Map<String, Object> batchUpgradeToFormal(List<Long> userIds);
}
