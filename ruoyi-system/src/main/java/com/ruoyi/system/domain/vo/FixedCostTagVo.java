package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class FixedCostTagVo implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long tenantId;
    private String tagName;
    private String tagCode;
    private String color;
    private Integer sortOrder;
    private String status;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
