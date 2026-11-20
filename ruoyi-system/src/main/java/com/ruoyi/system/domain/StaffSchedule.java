package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 员工排班 staff_schedule（T22.1，小微版 M22）
 * <p>按天×班次×员工一条记录，Boss 按周/月批量编排；状态 draft→published，
 * 发布后仍可修正，修正写 {@link ScheduleChangeLog} 留痕。
 * <p>★班次分早班 morning / 晚班 evening；起止时间由 Boss 自定（禁止硬编码）。
 * <p>★员工/顾客仅可见 published 记录；Boss 可见 draft + published。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 * <p>UNIQUE(tenant_id, staff_id, schedule_date, shift_type) 防同一员工同一天同一班次重复排班。
 *
 * @author ruoyi
 * @date 2026-10-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("staff_schedule")
public class StaffSchedule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 员工 user_id（FK→sys_user.user_id，salesman 角色）
     */
    private Long staffId;

    /**
     * 排班日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /**
     * 班次类型：morning=早班 / evening=晚班
     */
    private String shiftType;

    /**
     * ★班次开始时间（Boss 自定，禁止硬编码）
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /**
     * ★班次结束时间（Boss 自定，禁止硬编码）
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    /**
     * 状态：draft=草稿 / published=已发布（发布后仍可修正）
     */
    private String status;

    /**
     * 周期类型：week=一周 / month=一月
     */
    private String periodType;

    /**
     * 周期键：week=2026-W41（ISO 周）/ month=2026-10
     */
    private String periodKey;

    /**
     * 备注
     */
    private String remark;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;
}
