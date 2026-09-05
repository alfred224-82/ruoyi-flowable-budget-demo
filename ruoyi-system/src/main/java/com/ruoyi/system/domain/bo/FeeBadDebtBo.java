package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 烂账记录业务对象 fee_bad_debt（T5.6）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeBadDebtBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（跟进操作用） */
    private Long id;

    /** 项目ID */
    @NotNull(message = "烂账标记必须关联项目", groups = {AddGroup.class})
    private Long projectId;

    /** 原因说明 */
    @NotBlank(message = "烂账原因不能为空", groups = {AddGroup.class})
    private String reason;

    /** 烂账类型(LOST_CONTACT/QUALITY_DISPUTE/PRICE_ISSUE/INCOMPLETE/OTHER) */
    private String type;

    /** 处置建议(BAD_DEBT/FOLLOW_UP/LEGAL) */
    private String proposal;

    /** 计提坏账金额 */
    private BigDecimal provisionAmount;

    /** 状态（查询/跟进用） */
    private String status;

    /** 跟进记录 */
    private String followUpNotes;
}
