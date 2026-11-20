package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class PeriodRuleVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long tenantId;
    private String ruleName;
    private BigDecimal periodValue;
    private String periodUnit;
    private Integer totalDays;
    private Long tagId;
    /** 关联标签名称（回填，非表字段） */
    private String tagName;
    private String status;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
