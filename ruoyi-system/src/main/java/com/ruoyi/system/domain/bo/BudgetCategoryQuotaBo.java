package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

import com.ruoyi.system.domain.vo.BudgetCategoryQuotaItemVo;

/**
 * 老板大类预算额度保存业务对象（T5.14 ①，粗粒度）
 * 老板按期间录入各费用大类额度，落库到 budget_preparation.category_quotas
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class BudgetCategoryQuotaBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预算年度
     */
    @NotNull(message = "预算年度不能为空")
    private Integer budgetYear;

    /**
     * 预算月份（1-12）
     */
    @NotNull(message = "预算月份不能为空")
    @Min(value = 1, message = "预算月份必须在1-12之间")
    @Max(value = 12, message = "预算月份必须在1-12之间")
    private Integer budgetMonth;

    /**
     * 部门ID（年月+部门维度，T5.13/T5.14）
     * 基础版仅总公司（用户所属部门），高级版起支持多部门分别编制
     * 为空时默认使用当前用户所属部门
     */
    private Long departmentId;

    /**
     * 大类额度列表
     */
    @NotEmpty(message = "大类额度列表不能为空")
    @Valid
    private List<BudgetCategoryQuotaItemVo> quotas;

    /**
     * 备注
     */
    private String remark;
}
