package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.StaffScheduleBo;
import com.ruoyi.system.domain.vo.ScheduleChangeLogVo;
import com.ruoyi.system.domain.vo.StaffScheduleVo;
import com.ruoyi.system.service.IMicroStaffScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 员工排班管理 Controller（T22.2，小微版 M22）
 * <p>三端接入：Boss PC 管理 / 员工 MP 查看本人 / 顾客 MP 查看门店。
 * <p>类级 {@code @RequireFeature("FEAT_STAFF_SCHEDULE")} 版本开关控制；
 * micro_lite 免费版不启用，micro_pro 收费版启用。
 * <p>★班次分早班 morning / 晚班 evening；起止时间由 Boss 自定，禁止硬编码。
 * <p>★员工/顾客仅可见 published 记录（由 Service 层 statusOnlyPublished 强制过滤）。
 *
 * @author ruoyi
 * @date 2026-10-10
 */
@Validated
@RequireFeature("FEAT_STAFF_SCHEDULE")
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/staffSchedule")
public class MicroStaffScheduleController extends BaseController {

    private final IMicroStaffScheduleService staffScheduleService;

    // ============================================================
    // Boss PC 管理端
    // ============================================================

    /**
     * 查询员工下拉选项（salesman 角色，本租户）
     */
    @SaCheckPermission("micro:staffSchedule:view")
    @GetMapping("/staffOptions")
    public R<List<Map<String, Object>>> listStaffOptions() {
        return R.ok(staffScheduleService.listStaffOptions());
    }

    /**
     * 查询排班列表（Boss PC 排班日历）
     *
     * @param periodType week/month（可空）
     * @param periodKey  2026-W41 / 2026-10（可空）
     * @param status     draft/published（可空=全部）
     * @param staffId    员工 user_id（可空=全部）
     * @param startDate  开始日期（可空）
     * @param endDate    结束日期（可空）
     */
    @SaCheckPermission("micro:staffSchedule:view")
    @GetMapping("/list")
    public R<List<StaffScheduleVo>> list(
        @RequestParam(required = false) String periodType,
        @RequestParam(required = false) String periodKey,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long staffId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        // Boss 可见 draft + published（statusOnlyPublished=false）
        return R.ok(staffScheduleService.listSchedules(
            periodType, periodKey, status, staffId, startDate, endDate, false));
    }

    /**
     * 批量创建草稿排班（Boss 按周/月编排）
     */
    @SaCheckPermission("micro:staffSchedule:create")
    @Log(title = "员工排班-批量创建", businessType = BusinessType.INSERT)
    @PostMapping("/batchCreate")
    public R<Integer> batchCreate(@Validated(AddGroup.class) @RequestBody StaffScheduleBo bo) {
        return R.ok(staffScheduleService.batchCreate(bo));
    }

    /**
     * 发布排班（将指定周期 draft 批量置为 published）
     *
     * @param periodType week/month
     * @param periodKey  2026-W41 / 2026-10
     */
    @SaCheckPermission("micro:staffSchedule:publish")
    @Log(title = "员工排班-发布", businessType = BusinessType.UPDATE)
    @PostMapping("/publish")
    public R<Integer> publish(@RequestParam String periodType, @RequestParam String periodKey) {
        return R.ok(staffScheduleService.publish(periodType, periodKey));
    }

    /**
     * 修正排班（published 记录编辑起止时间/班次，写留痕日志）
     */
    @SaCheckPermission("micro:staffSchedule:edit")
    @Log(title = "员工排班-修正", businessType = BusinessType.UPDATE)
    @PutMapping("/modify")
    public R<Long> modify(@Validated(EditGroup.class) @RequestBody StaffScheduleBo bo) {
        return R.ok(staffScheduleService.modify(bo));
    }

    /**
     * 设定班次时间（Boss 自定义早班/晚班起止时间，批量更新指定周期内所有对应班次记录）
     *
     * @param periodType week/month
     * @param periodKey  2026-W41 / 2026-10
     * @param shiftType  morning/evening
     * @param startTime  HH:mm:ss
     * @param endTime    HH:mm:ss
     */
    @SaCheckPermission("micro:staffSchedule:edit")
    @Log(title = "员工排班-设定班次时间", businessType = BusinessType.UPDATE)
    @PutMapping("/setShiftTime")
    public R<Integer> setShiftTime(
        @RequestParam String periodType,
        @RequestParam String periodKey,
        @RequestParam String shiftType,
        @RequestParam @DateTimeFormat(pattern = "HH:mm:ss") LocalTime startTime,
        @RequestParam @DateTimeFormat(pattern = "HH:mm:ss") LocalTime endTime) {
        return R.ok(staffScheduleService.setShiftTime(periodType, periodKey, shiftType, startTime, endTime));
    }

    /**
     * 删除草稿排班（仅 draft 可删；published 不可删）
     */
    @SaCheckPermission("micro:staffSchedule:edit")
    @Log(title = "员工排班-删除草稿", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> deleteDraft(@PathVariable Long id) {
        staffScheduleService.deleteDraft(id);
        return R.ok();
    }

    /**
     * 查询修正日志
     *
     * @param scheduleId 排班记录ID（可空=按周期查询）
     * @param periodType 可空
     * @param periodKey  可空
     */
    @SaCheckPermission("micro:staffSchedule:view")
    @GetMapping("/changeLogs")
    public R<List<ScheduleChangeLogVo>> listChangeLogs(
        @RequestParam(required = false) Long scheduleId,
        @RequestParam(required = false) String periodType,
        @RequestParam(required = false) String periodKey) {
        return R.ok(staffScheduleService.listChangeLogs(scheduleId, periodType, periodKey));
    }

    // ============================================================
    // 员工小程序（T22.4）
    // ============================================================

    /**
     * 员工查看本人排班（仅 published）
     *
     * @param periodType week/month
     * @param periodKey  2026-W41 / 2026-10
     */
    @SaCheckPermission("micro:staffSchedule:my")
    @GetMapping("/my")
    public R<List<StaffScheduleVo>> my(
        @RequestParam(required = false) String periodType,
        @RequestParam(required = false) String periodKey) {
        return R.ok(staffScheduleService.mySchedules(periodType, periodKey));
    }

    // ============================================================
    // 顾客小程序（T22.5）
    // ============================================================

    /**
     * 顾客查看门店最新排班（仅 published，按日期分组）
     *
     * @param startDate 开始日期（默认今天）
     * @param endDate   结束日期（默认开始日期+6 天）
     */
    @SaCheckPermission("micro:staffSchedule:store")
    @GetMapping("/store")
    public R<Map<String, List<StaffScheduleVo>>> store(
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return R.ok(staffScheduleService.storeSchedules(startDate, endDate));
    }

    /**
     * ★T22.5 M21 联动：查询指定日期已发布排班的员工 user_id 集合（内部使用，不对外暴露菜单权限）
     * <p>供 MicroSelfBookingServiceImpl.listAvailableStaff 数据源使用；
     * 走 micro:selfBooking:view 权限，避免顾客端额外授权。
     *
     * @param date 排班日期
     */
    @SaCheckPermission("micro:selfBooking:view")
    @GetMapping("/scheduledStaffIds")
    public R<Set<Long>> scheduledStaffIds(
        @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(staffScheduleService.listScheduledStaffIds(date));
    }
}
