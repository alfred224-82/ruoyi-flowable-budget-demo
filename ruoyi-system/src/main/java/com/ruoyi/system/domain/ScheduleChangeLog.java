package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.util.Date;

/**
 * 排班修正日志 schedule_change_log（T22.1，小微版 M22）
 * <p>发布后每次修正（起止时间/班次/状态发布/删除）留痕；员工/顾客端展示最新版本。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_change_log")
public class ScheduleChangeLog extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 排班记录ID（FK→staff_schedule.id）
     */
    private Long scheduleId;

    /**
     * 员工 user_id（冗余快照）
     */
    private Long staffId;

    /**
     * 排班日期（冗余快照）
     */
    private LocalDate scheduleDate;

    /**
     * 班次类型（冗余快照）
     */
    private String shiftType;

    /**
     * 变更类型：TIME=起止时间 / SHIFT=班次 / STATUS=状态发布 / DELETE=删除
     */
    private String changeType;

    /**
     * 变更前时间（如 09:00-18:00）
     */
    private String beforeTime;

    /**
     * 变更后时间（如 10:00-19:00）
     */
    private String afterTime;

    /**
     * 变更前状态
     */
    private String beforeStatus;

    /**
     * 变更后状态
     */
    private String afterStatus;

    /**
     * 变更原因（Boss 修正时填写）
     */
    private String changeReason;

    /**
     * 操作人 user_id
     */
    private Long operatorId;

    /**
     * 操作人昵称（冗余快照）
     */
    private String operatorName;

    /**
     * 操作时间
     */
    private Date operateTime;
}
