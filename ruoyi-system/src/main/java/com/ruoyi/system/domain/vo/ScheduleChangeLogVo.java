package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * 排班修正日志视图对象（T22.1，小微版 M22）
 *
 * @author ruoyi
 * @date 2026-10-10
 */
@Data
public class ScheduleChangeLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 排班记录ID */
    private Long scheduleId;

    /** 员工 user_id */
    private Long staffId;

    /** 员工昵称（回填，非表字段） */
    private String staffName;

    /** 排班日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** 班次类型 */
    private String shiftType;

    /** 变更类型：TIME/SHIFT/STATUS/DELETE */
    private String changeType;

    /** 变更前时间 */
    private String beforeTime;

    /** 变更后时间 */
    private String afterTime;

    /** 变更前状态 */
    private String beforeStatus;

    /** 变更后状态 */
    private String afterStatus;

    /** 变更原因 */
    private String changeReason;

    /** 操作人 user_id */
    private Long operatorId;

    /** 操作人昵称 */
    private String operatorName;

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date operateTime;
}
