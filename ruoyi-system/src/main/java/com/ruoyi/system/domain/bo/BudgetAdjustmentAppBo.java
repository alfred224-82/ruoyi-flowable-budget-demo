package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 预算调整申请业务对象 fee_budget_adjustment_app
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BudgetAdjustmentAppBo extends BaseEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 申请单号（查询条件）
     */
    private String appNo;

    /**
     * 状态（查询条件）
     */
    private String status;

    /**
     * 关联报销单ID（可选，不再强制绑定）
     */
    private Long billId;

    /**
     * 物资小类编码
     */
    @NotBlank(message = "物资小类不能为空", groups = {AddGroup.class})
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @NotBlank(message = "用途标签不能为空", groups = {AddGroup.class})
    private String purposeTag;

    /**
     * 目标预算科目编码（由后端通过映射规则自动填充）
     */
    private String targetSubject;

    /**
     * 目标预算科目名称
     */
    private String targetSubjectName;

    /**
     * 责任中心编码
     */
    private String costCenterCode;

    /**
     * 申请调整金额
     */
    @NotNull(message = "申请调整金额不能为空", groups = {AddGroup.class})
    private BigDecimal requestAmount;

    /**
     * 预算金额（只读，由后端自动填充）
     */
    private BigDecimal budgetAmount;

    /**
     * 调整原因说明
     */
    @NotBlank(message = "调整原因不能为空", groups = {AddGroup.class})
    private String reason;
}
