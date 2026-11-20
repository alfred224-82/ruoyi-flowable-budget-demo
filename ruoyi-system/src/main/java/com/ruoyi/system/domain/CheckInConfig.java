package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 打卡配置 check_in_config（T24.1，小微版 M24）
 * <p>租户级打卡开关+拍照要求+迟到阈值。
 * <p>UNIQUE(tenant_id) 每租户一条配置。
 * <p>三级开关链路：industry_template.check_in_enabled → sys_tenant.check_in_enabled → check_in_config.enabled
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("check_in_config")
public class CheckInConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 是否启用打卡：0=否 1=是（三级开关之 Boss 活动级）
     */
    private Integer enabled;

    /**
     * 打卡是否必须拍照：0=否 1=是
     */
    private Integer photoRequired;

    /**
     * 迟到判定阈值（分钟）：打卡时间超过活动开始时间+N分钟视为迟到
     */
    private Integer lateThresholdMinutes;
}
