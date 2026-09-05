package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 提现审核业务对象（T14.3 人工审核）
 * <p>approved=true → APPROVED 并扣减虚拟账户余额；approved=false → REJECTED 并解冻。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class WithdrawalAuditBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 提现申请ID */
    @NotNull(message = "提现申请ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 是否通过 */
    @NotNull(message = "审核结论不能为空", groups = {EditGroup.class})
    private Boolean approved;

    /** 审核意见 */
    private String auditRemark;
}
