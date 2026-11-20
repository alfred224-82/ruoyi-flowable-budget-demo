package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 打卡配置视图对象（T24.1，小微版 M24）
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class CheckInConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 是否启用打卡：0=否 1=是 */
    private Integer enabled;

    /** 打卡是否必须拍照：0=否 1=是 */
    private Integer photoRequired;

    /** 迟到判定阈值（分钟） */
    private Integer lateThresholdMinutes;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
