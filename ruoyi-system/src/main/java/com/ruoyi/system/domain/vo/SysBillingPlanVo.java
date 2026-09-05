package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 计费套餐视图对象 sys_billing_plan（T14.1）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysBillingPlanVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 套餐编码 */
    private String planCode;

    /** 套餐名称 */
    private String name;

    /** 价格(元) */
    private BigDecimal price;

    /** 有效天数 */
    private Integer days;

    /** 授权类型：TRIAL/FORMAL */
    private String licenseType;

    /** 默认授予版本 */
    private String edition;

    /** 是否仅首次可用(0否 1是) */
    private Integer firstOnly;

    /** 是否需支付(0否 1是) */
    private Integer needPay;

    /** 排序 */
    private Integer sort;

    /** 状态（0上架 1下架） */
    private String status;

    /** 备注 */
    private String remark;
}
