package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.SelfBookingSubmitBo;
import com.ruoyi.system.domain.vo.ServiceItemVo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 自助预约服务接口（T21.1~T21.4，小微版）
 * <p>顾客小程序「自助预约」页面数据源 + 提交逻辑。
 * <p>★复用 booking_order 表写入，不新增业务表；V1 零支付（pay_status=PENDING_PAY）。
 * <p>资源状态三态：idle→reserved（预约提交）→in_use（活动开始）→idle（结束/取消）。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
public interface IMicroSelfBookingService {

    /**
     * 查询可选项目列表（排除 space 分类，仅上架状态）
     */
    List<ServiceItemVo> listAvailableItems();

    /**
     * 查询空闲房间列表（排除该时段已有预约 + 排除 in_use 房间）
     *
     * @param bookingDate 预约日期
     * @param timeSlot    预约时段（如 09:00-11:00，可空）
     */
    List<ServiceItemVo> listAvailableRooms(LocalDate bookingDate, String timeSlot);

    /**
     * 查询当天有排班的助教列表（返回 avatar/nickName/description）
     *
     * @param bookingDate 预约日期
     */
    List<Map<String, Object>> listAvailableStaff(LocalDate bookingDate);

    /**
     * 提交自助预约（写 booking_order + 更新资源状态为 reserved + 写 resource_status_log）
     *
     * @return 订单ID
     */
    Long submitSelfBooking(SelfBookingSubmitBo bo);

    /**
     * 校验预约日期（≤ 明天）
     */
    void validateBookingTime(LocalDate bookingDate);

    /**
     * 释放关联资源（reserved/idle → idle + 写日志）
     * <p>取消预约/手动释放时调用。
     *
     * @param bookingId 预约订单ID
     */
    void releaseResources(Long bookingId);

    /**
     * 定时任务：按预约时间窗 + 订单状态推进资源 reserved→in_use→idle（T21.8）
     * <p>由 {@code resourceInUseFlowJob} 每 10 分钟调用一次，幂等（当前状态==目标状态则跳过）。
     *
     * @return 状态变更条数
     */
    int advanceResourceStatusByTime();

    /**
     * Boss PC 查询房间资源状态列表（space 分类 service_item，含 resource_status）
     */
    List<ServiceItemVo> listRoomResourceStatus();

    /**
     * Boss PC 手动释放资源（reserved → idle + 写 resource_status_log）
     *
     * @param resourceType staff=助教 / room=房间
     * @param resourceId   助教=sys_user.user_id / 房间=service_item.id
     * @param reason       释放原因（必填）
     */
    void manualReleaseResource(String resourceType, Long resourceId, String reason);

    /**
     * Boss PC 手动设置资源状态（写 resource_status_log；房间同步更新 service_item.resource_status）
     * <p>助教可选：idle=空闲 / working=工作 / resting=休息；房间可选：idle=空闲 / in_use=使用。
     * <p>仅 idle 状态的资源可被顾客预约。
     *
     * @param resourceType staff=助教 / room=房间
     * @param resourceId   助教=sys_user.user_id / 房间=service_item.id
     * @param targetStatus 目标状态
     * @param reason       变更原因（可空）
     */
    void setResourceStatus(String resourceType, Long resourceId, String targetStatus, String reason);

    /**
     * Boss PC 查询助教资源状态列表（salesman 角色用户 + 最新 resource_status_log 推导状态）
     */
    List<Map<String, Object>> listStaffResourceStatus();

    /**
     * 查询资源状态变更日志（最近 50 条）
     *
     * @param resourceType staff/room（可空=全部）
     * @param resourceId   资源ID（可空=全部）
     */
    List<com.ruoyi.system.domain.vo.ResourceStatusLogVo> listResourceStatusLogs(String resourceType, Long resourceId);

    /**
     * ★释放 Boss 活动关联的房间和助教资源（→ idle + 写日志）
     * <p>活动结束/取消/删除时调用，确保被占用的资源重新变为空闲可预约。
     *
     * @param activityIds 需释放资源的活动ID列表
     * @return 实际释放的资源条数（房间 + 助教）
     */
    int releaseActivityResources(List<Long> activityIds);

    /**
     * ★BUG-02 占用活动关联的房间/员工资源（→ in_use/working + 写日志）
     * <p>活动发布（DRAFT→PUBLISHED 或直接以 PUBLISHED 创建）时调用，
     * 让关联的房间/大巴/导游/司机立即翻为忙碌状态，避免重复预约。
     * <p>非 idle 资源视为已被其他预约/活动占用，fail-soft 保留原状不覆盖，不阻断活动发布。
     *
     * @param activityId 需占用资源的活动ID
     * @return 实际占用的资源条数（房间 + 员工）
     */
    int occupyActivityResources(Long activityId);
}
