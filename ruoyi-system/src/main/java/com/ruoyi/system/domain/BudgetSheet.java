package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("budget_sheet")
public class BudgetSheet extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    private String sheetNo;

    private Long orgId;

    private String orgName;

    private String budgetMonth;

    private String status;

    private String rejectLevel;

    private String rejectReason;

    private Date deadlineTime;

    private String currentHandler;

    private BigDecimal totalBudget;

    private BigDecimal totalActual;

    private BigDecimal varianceRate;

    private String processInstanceId;

    private String remark;
}

