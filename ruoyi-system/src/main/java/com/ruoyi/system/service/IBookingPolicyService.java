package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.BookingPolicyVo;
import com.ruoyi.system.domain.bo.BookingPolicyBo;

/**
 * 预约策略配置服务接口（T18.10，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
public interface IBookingPolicyService {

    /** 查询当前租户预约策略 */
    BookingPolicyVo queryPolicy();

    /** 保存/更新预约策略（upsert） */
    Long savePolicy(BookingPolicyBo bo);
}
