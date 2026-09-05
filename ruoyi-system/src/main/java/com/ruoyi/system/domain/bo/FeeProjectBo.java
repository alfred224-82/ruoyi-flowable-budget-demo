package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 项目信息业务对象 fee_project（T5.1）
 * 立项：老板建 / 财务代建 / 项目经理申请
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "项目ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 项目名称 */
    @NotBlank(message = "项目名称不能为空", groups = {AddGroup.class})
    private String projectName;

    /** 客户名称 */
    private String clientName;

    /** 初始合同额 */
    @NotNull(message = "合同额不能为空", groups = {AddGroup.class})
    private BigDecimal contractAmount;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /** 项目经理ID */
    private Long projectManagerId;

    /** 项目经理姓名 */
    private String projectManagerName;

    /** 项目总预算（按大类分配合计） */
    private BigDecimal totalBudget;

    /** 备注 */
    private String remark;

    /** 项目预算科目（按大类分配，审批通过生成） */
    private List<FeeProjectSubjectBo> subjects;

    /** 项目成员 */
    private List<FeeProjectMemberBo> members;

    /**
     * 项目预算科目项
     */
    @Data
    public static class FeeProjectSubjectBo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 主键ID（编辑时） */
        private Long id;
        /** 费用大类ID */
        private Long categoryId;
        /** 费用大类名称 */
        private String categoryName;
        /** 预算科目ID */
        private Long budgetSubjectId;
        /** 科目名称 */
        private String subjectName;
        /** 项目级预算金额 */
        private BigDecimal budgetAmount;
        /** 是否启用 */
        private Integer isActive;
    }

    /**
     * 项目成员项
     */
    @Data
    public static class FeeProjectMemberBo implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 用户ID */
        @NotNull(message = "成员用户不能为空", groups = {AddGroup.class})
        private Long userId;
        /** 用户姓名 */
        private String userName;
        /** 成员角色(MANAGER/MEMBER/VIEWER) */
        private String role;
        /** 投入比例% */
        private Integer investRatio;
    }
}
