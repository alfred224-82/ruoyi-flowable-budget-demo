package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 计费套餐业务对象 sys_billing_plan（T14.1，平台级维护）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysBillingPlanBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 套餐编码：trial/first_month/monthly/yearly */
    @NotBlank(message = "套餐编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String planCode;

    /** 套餐名称 */
    @NotBlank(message = "套餐名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 价格(元) */
    @NotNull(message = "价格不能为空", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal price;

    /** 有效天数 */
    @NotNull(message = "有效天数不能为空", groups = {AddGroup.class, EditGroup.class})
    private Integer days;

    /** 授权类型：TRIAL/FORMAL */
    private String licenseType;

    /** 默认授予版本：BASIC/ADVANCED/PROFESSIONAL */
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
