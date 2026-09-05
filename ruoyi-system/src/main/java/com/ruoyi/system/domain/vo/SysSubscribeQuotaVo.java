package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 订阅消息授权次数视图对象 sys_subscribe_quota（T14.4）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysSubscribeQuotaVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 微信 openid */
    private String openid;

    /** 订阅消息模板ID */
    private String templateId;

    /** 剩余可发送次数 */
    private Integer remainingCount;

    /** 累计授权次数 */
    private Integer totalGranted;

    /** 累计发送次数 */
    private Integer totalSent;
}
