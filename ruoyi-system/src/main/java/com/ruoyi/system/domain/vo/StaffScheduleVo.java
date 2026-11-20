package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

/**
 * 员工排班视图对象（T22.2，小微版 M22）
 *
 * @author ruoyi
 * @date 2026-10-10
 */
@Data
public class StaffScheduleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 员工 user_id */
    private Long staffId;

    /** 员工昵称（回填，非表字段） */
    private String staffName;

    /** 员工头像（回填，非表字段） */
    private String staffAvatar;

    /** 排班日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate scheduleDate;

    /** 班次类型：morning=早班 / evening=晚班 */
    private String shiftType;

    /** ★班次开始时间 */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime startTime;

    /** ★班次结束时间 */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime endTime;

    /** 状态：draft=草稿 / published=已发布 */
    private String status;

    /** 周期类型：week=一周 / month=一月 */
    private String periodType;

    /** 周期键 */
    private String periodKey;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
