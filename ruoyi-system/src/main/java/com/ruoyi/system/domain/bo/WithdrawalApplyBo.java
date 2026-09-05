package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 提现申请业务对象（T14.3）
 * <p>租户发起提现：金额从虚拟账户可用余额冻结；人工审核通过后扣减、驳回则解冻。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class WithdrawalApplyBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 提现金额 */
    @NotNull(message = "提现金额不能为空", groups = {AddGroup.class})
    @DecimalMin(value = "0.01", message = "提现金额必须大于0", groups = {AddGroup.class})
    private BigDecimal amount;

    /** 收款账户信息(脱敏存储) */
    private String accountInfo;
}
