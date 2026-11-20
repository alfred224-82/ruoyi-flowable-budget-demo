package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 打卡记录 check_in_record（T24.1，小微版 M24）
 * <p>员工到岗拍照+时间+地点+水印；迟到判定；Boss 可补录。
 * <p>★打卡不受活动锁定影响，锁定后仍可打卡。
 * <p>★source 字段区分 NORMAL=正常打卡 / SUPPLEMENT=Boss 补录。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 * <p>UNIQUE(tenant_id, activity_id, staff_user_id, check_in_time) 防同一员工同一时间重复打卡。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_in_record")
public class CheckInRecord extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 活动ID（FK→promotion_activity.id）
     */
    private Long activityId;

    /**
     * 打卡员工 user_id（FK→sys_user.user_id，salesman 角色）
     */
    private Long staffUserId;

    /**
     * 打卡时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkInTime;

    /**
     * 打卡地点名称
     */
    private String locationName;

    /**
     * 纬度
     */
    private BigDecimal locationLat;

    /**
     * 经度
     */
    private BigDecimal locationLng;

    /**
     * 打卡照片URL（OSS）
     */
    private String photoUrl;

    /**
     * 是否迟到：0=否 1=是
     */
    private Integer isLate;

    /**
     * 水印文字（时间/位置/用户名）
     */
    private String watermarkText;

    /**
     * 来源：NORMAL=正常打卡 / SUPPLEMENT=Boss 补录
     */
    private String source;

    /**
     * 补录原因（Boss 补录时填写）
     */
    private String supplementReason;

    /**
     * 备注
     */
    private String remark;
}
