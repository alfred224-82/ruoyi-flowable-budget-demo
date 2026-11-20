package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 打卡记录视图对象（T24.1，小微版 M24）
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
@ExcelIgnoreUnannotated
public class CheckInRecordVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 活动ID */
    private Long activityId;

    /** 活动名称（回填，非表字段） */
    private String activityTitle;

    /** 打卡员工 user_id */
    private Long staffUserId;

    /** 员工昵称（回填，非表字段） */
    @ExcelProperty(value = "员工姓名")
    private String staffName;

    /** 打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(value = "打卡时间")
    private Date checkInTime;

    /** 打卡地点名称 */
    @ExcelProperty(value = "打卡地点")
    private String locationName;

    /** 纬度 */
    private BigDecimal locationLat;

    /** 经度 */
    private BigDecimal locationLng;

    /** 打卡照片URL */
    private String photoUrl;

    /** 是否迟到：0=否 1=是 */
    @ExcelProperty(value = "是否迟到")
    private String isLateStr;

    /** 水印文字 */
    private String watermarkText;

    /** 来源：NORMAL=正常打卡 / SUPPLEMENT=Boss 补录 */
    @ExcelProperty(value = "来源")
    private String source;

    /** 补录原因 */
    @ExcelProperty(value = "补录原因")
    private String supplementReason;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
