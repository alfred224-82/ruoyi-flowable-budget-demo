package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 合同变更业务对象 fee_project_change（T5.5）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectChangeBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（查询用） */
    private Long id;

    /** 项目ID */
    @NotNull(message = "变更必须关联项目", groups = {AddGroup.class})
    private Long projectId;

    /** 变更类型(CONTRACT_INCREASE/BUDGET_INCREASE/COMPREHENSIVE) */
    private String changeType;

    /** 变更原因 */
    private String changeReason;

    /** 合同额变更量（增量，正数=增加） */
    private BigDecimal contractDelta;

    /** 按大类预算变更 {"差旅招待":2000} */
    private Map<String, BigDecimal> budgetDeltas;

    /** 依据附件 */
    private String basisUrls;

    /** 变更状态（查询用） */
    private String changeStatus;
}
