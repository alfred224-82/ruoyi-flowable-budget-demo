package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 预算驳回历史记录业务对象 budget_reject_history
 *
 * @author ruoyi
 * @date 2026-07-02
 */
@Data
public class BudgetRejectHistoryBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 预算单ID
     */
    @NotNull(message = "预算单ID不能为空", groups = { AddGroup.class })
    private Long sheetId;

    /**
     * 预算单号
     */
    private String sheetNo;

    /**
     * 驳回方层级(HQ/Branch)
     */
    @NotBlank(message = "驳回方层级不能为空", groups = { AddGroup.class })
    private String rejectFromLevel;

    /**
     * 驳回人工号
     */
    private String rejectFromUser;

    /**
     * 驳回人姓名
     */
    private String rejectFromName;

    /**
     * 被驳回方层级(Branch/Dept)
     */
    @NotBlank(message = "被驳回方层级不能为空", groups = { AddGroup.class })
    private String rejectToLevel;

    /**
     * 被驳回人工号
     */
    private String rejectToUser;

    /**
     * 被驳回部门ID
     */
    private Long rejectToDeptId;

    /**
     * 被驳回部门名称
     */
    private String rejectToDeptName;

    /**
     * 驳回理由
     */
    @NotBlank(message = "驳回理由不能为空", groups = { AddGroup.class })
    private String rejectReason;

    /**
     * 截止时间
     */
    private Date deadlineTime;

    /**
     * 驳回时间
     */
    private Date rejectTime;

    /**
     * 处理时间
     */
    private Date handleTime;

    /**
     * 处理耗时(小时)
     */
    private BigDecimal handleDurationHours;

    /**
     * 是否超时(0否1是)
     */
    private Integer isTimeout;
}
