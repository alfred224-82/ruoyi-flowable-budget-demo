package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 员工排班业务对象（T22.2，小微版 M22）
 * <p>用于批量创建（{@link #items}）与单条修正（{@link #id}）两种场景。
 *
 * @author ruoyi
 * @date 2026-10-10
 */
@Data
public class StaffScheduleBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（修正时必填） */
    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 员工 user_id（单条修正时必填） */
    private Long staffId;

    /** 排班日期（单条修正时必填） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** 班次类型：morning=早班 / evening=晚班 */
    private String shiftType;

    /** ★班次开始时间（Boss 自定） */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /** ★班次结束时间（Boss 自定） */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    /** 周期类型：week=一周 / month=一月 */
    private String periodType;

    /** 周期键：week=2026-W41（ISO 周）/ month=2026-10 */
    private String periodKey;

    /** 备注 */
    private String remark;

    /** 变更原因（修正时必填，写入 schedule_change_log） */
    private String changeReason;

    /**
     * ★批量创建条目列表（仅 batchCreate 使用）
     * <p>每条目 = 员工×日期×班次×起止时间
     */
    private List<ScheduleItemBo> items;

    /**
     * 批量创建的单条排班项
     */
    @Data
    public static class ScheduleItemBo implements Serializable {

        private static final long serialVersionUID = 1L;

        /** 员工 user_id */
        @NotNull(message = "员工不能为空", groups = {AddGroup.class})
        private Long staffId;

        /** 排班日期 */
        @NotNull(message = "排班日期不能为空", groups = {AddGroup.class})
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate scheduleDate;

        /** 班次类型：morning=早班 / evening=晚班 */
        @NotNull(message = "班次类型不能为空", groups = {AddGroup.class})
        private String shiftType;

        /** ★班次开始时间 */
        @NotNull(message = "班次开始时间不能为空", groups = {AddGroup.class})
        @JsonFormat(pattern = "HH:mm:ss")
        private LocalTime startTime;

        /** ★班次结束时间 */
        @NotNull(message = "班次结束时间不能为空", groups = {AddGroup.class})
        @JsonFormat(pattern = "HH:mm:ss")
        private LocalTime endTime;

        /** 备注 */
        private String remark;
    }
}
