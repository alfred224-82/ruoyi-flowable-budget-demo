package com.ruoyi.system.service;

import com.ruoyi.system.domain.bo.StaffScheduleBo;
import com.ruoyi.system.domain.vo.ScheduleChangeLogVo;
import com.ruoyi.system.domain.vo.StaffScheduleVo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 员工排班服务接口（T22.2，小微版 M22）
 * <p>Boss PC 按周/月批量创建草稿 → 发布 → 修正（留痕）；员工/顾客仅可见 published。
 * <p>★班次分早班 morning / 晚班 evening，起止时间由 Boss 自定（禁止硬编码）。
 * <p>★与 M21 自助预约联动：{@link #listScheduledStaffIds(LocalDate)} 供
 * {@code MicroSelfBookingServiceImpl.listAvailableStaff} 作为「当天有排班的助教」数据源。
 *
 * @author ruoyi
 * @date 2026-10-10
 */
public interface IMicroStaffScheduleService {

    // ===== Boss PC 管理端 =====

    /**
     * 按周期批量创建草稿排班（T22.2 ①）
     * <p>入参 items 中每条目 = 员工×日期×班次×起止时间；
     * 同一员工同一天同一班次已存在则跳过（幂等）。
     *
     * @param bo 含 periodType/periodKey/items
     * @return 实际新增条数
     */
    int batchCreate(StaffScheduleBo bo);

    /**
     * 查询排班列表（T22.2 ②）
     * <p>Boss 可见 draft + published；员工/顾客仅可见 published（由 Controller 传 statusOnlyPublished 控制）。
     *
     * @param periodType         week/month（可空）
     * @param periodKey          2026-W41 / 2026-10（可空）
     * @param status             draft/published（可空=全部）
     * @param staffId            员工 user_id（可空=全部）
     * @param startDate          开始日期（可空，与 endDate 组合按日期区间查询）
     * @param endDate            结束日期（可空）
     * @param statusOnlyPublished 是否强制仅返回 published（员工/顾客端调用为 true）
     */
    List<StaffScheduleVo> listSchedules(String periodType, String periodKey, String status,
                                        Long staffId, LocalDate startDate, LocalDate endDate,
                                        boolean statusOnlyPublished);

    /**
     * 发布排班（T22.2 ③）：将指定周期 draft 批量置为 published，写 schedule_change_log（changeType=STATUS）
     *
     * @param periodType week/month
     * @param periodKey  2026-W41 / 2026-10
     * @return 实际发布条数
     */
    int publish(String periodType, String periodKey);

    /**
     * 修正排班（T22.2 ④）：published 记录编辑起止时间/班次，写 schedule_change_log
     *
     * @param bo 含 id + 变更字段 + changeReason
     * @return 记录ID
     */
    Long modify(StaffScheduleBo bo);

    /**
     * 设定班次时间（T22.2 ⑤）：Boss 自定义早班/晚班起止时间，批量更新指定周期内所有对应班次记录
     *
     * @param periodType week/month
     * @param periodKey  2026-W41 / 2026-10
     * @param shiftType  morning/evening
     * @param startTime  HH:mm:ss
     * @param endTime    HH:mm:ss
     * @return 实际更新条数
     */
    int setShiftTime(String periodType, String periodKey, String shiftType,
                     java.time.LocalTime startTime, java.time.LocalTime endTime);

    /**
     * 删除草稿排班（仅 draft 可删；published 不可删，只能通过修正调整）
     *
     * @param id 排班记录ID
     */
    void deleteDraft(Long id);

    /**
     * 查询修正日志（Boss PC 排班页展示）
     *
     * @param scheduleId 排班记录ID（可空=按周期查询）
     * @param periodType 可空
     * @param periodKey  可空
     */
    List<ScheduleChangeLogVo> listChangeLogs(Long scheduleId, String periodType, String periodKey);

    // ===== 员工小程序 =====

    /**
     * 员工查看本人排班（T22.4）：仅返回本人 published 记录
     *
     * @param periodType week/month
     * @param periodKey  2026-W41 / 2026-10
     */
    List<StaffScheduleVo> mySchedules(String periodType, String periodKey);

    // ===== 顾客小程序 =====

    /**
     * 顾客查看门店最新排班（T22.5）：仅返回 published 记录，按日期分组
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 按日期分组的排班列表 Map&lt;date, List&lt;StaffScheduleVo&gt;&gt;
     */
    Map<String, List<StaffScheduleVo>> storeSchedules(LocalDate startDate, LocalDate endDate);

    // ===== M21 联动 =====

    /**
     * ★T22.5 与 M21 自助预约联动：查询指定日期已发布排班的员工 user_id 集合
     * <p>替代 {@code MicroSelfBookingServiceImpl.listAvailableStaff} 原按 booking_order 过滤逻辑。
     *
     * @param date 排班日期
     * @return 当天有 published 排班的员工 user_id 集合
     */
    java.util.Set<Long> listScheduledStaffIds(LocalDate date);

    /**
     * ★T22.6 查询指定日期已发布排班的员工班次时间（供自助预约校验助教是否在岗）
     * <p>返回 Map&lt;staffId, List&lt;int[2]&gt;&gt;，int[2] = {startTimeMinutes, endTimeMinutes}（距午夜的分钟数）。
     * <p>一个员工同一天可能有多个班次（早班+晚班），故值为 List。
     *
     * @param date 排班日期
     * @return staffId → 班次时间列表
     */
    Map<Long, List<int[]>> getStaffShiftTimes(LocalDate date);

    // ===== 员工下拉选项（Boss PC 排班页使用） =====

    /**
     * 查询本租户 salesman 角色员工作为排班下拉选项（含昵称/头像）
     */
    List<Map<String, Object>> listStaffOptions();
}
