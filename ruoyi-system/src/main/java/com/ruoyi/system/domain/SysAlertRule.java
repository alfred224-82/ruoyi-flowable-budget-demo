package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 告警规则对象 sys_alert_rule（T18.15，小微版 M18）
 * <p>★平台级表（无 tenant_id），仅 Admin 管理。
 * <p>规则类型：LICENSE_EXPIRE/COMPLIANCE_HIT/TENANT_DISABLED/SERVICE_DOWN。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_alert_rule")
public class SysAlertRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 规则类型：LICENSE_EXPIRE/COMPLIANCE_HIT/TENANT_DISABLED/SERVICE_DOWN */
    private String ruleType;

    /** 触发阈值 */
    private Integer thresholdValue;

    /** 严重程度：INFO/WARNING/CRITICAL */
    private String severity;

    /** 通知渠道：SYSTEM/EMAIL/SMS */
    private String notifyChannel;

    /** 是否启用：1=启用 0=停用 */
    private Integer enabled;

    /** 最近触发时间 */
    private Date lastTriggerTime;

    /** 累计触发次数 */
    private Integer triggerCount;

    /** 删除标志（0存在 2删除） */
    @TableLogic
    private String delFlag;

    private String remark;
}
