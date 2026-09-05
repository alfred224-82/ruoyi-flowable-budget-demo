package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 计费套餐对象 sys_billing_plan（T14.1 四档套餐）
 * <p>系统级共享表，不含 tenant_id（已加入 IGNORE_TENANT_TABLES）；主键为套餐编码 plan_code。
 * <p>trial=0元/7天不绑卡不扣款；first_month=299/首月仅首次；monthly=499/30天；yearly=4790/年。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_billing_plan")
public class SysBillingPlan extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 套餐编码（主键）：trial/first_month/monthly/yearly */
    @TableId(value = "plan_code", type = IdType.INPUT)
    private String planCode;

    /** 套餐名称 */
    private String name;

    /** 价格(元) */
    private BigDecimal price;

    /** 有效天数(trial=7,monthly=30,yearly=365) */
    private Integer days;

    /** 授权类型：TRIAL/FORMAL */
    private String licenseType;

    /** 默认授予版本：BASIC/ADVANCED/PROFESSIONAL */
    private String edition;

    /** 是否仅首次可用(0否 1是；首月特惠同租户仅一次) */
    private Integer firstOnly;

    /** 是否需支付(0否 1是；试用不绑卡不扣款=0) */
    private Integer needPay;

    /** 排序 */
    private Integer sort;

    /** 状态（0上架 1下架） */
    private String status;

    /** 备注 */
    private String remark;
}
