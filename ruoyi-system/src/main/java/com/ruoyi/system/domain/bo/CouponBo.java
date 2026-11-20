package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券业务对象 coupon（T16.3，小微版 M16）
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class CouponBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 优惠券名称 */
    @NotBlank(message = "优惠券名称不能为空", groups = {AddGroup.class})
    private String name;

    /** 券类型：discount/gift/cash */
    @NotBlank(message = "券类型不能为空", groups = {AddGroup.class})
    private String type;

    /** 券值 */
    private BigDecimal value;

    /** 领取后有效天数(默认30) */
    private Integer validDays;

    /** 发行总量(NULL=不限量) */
    private Integer total;

    /** 状态：0启用 1停用 */
    private String status;

    private String remark;
}
