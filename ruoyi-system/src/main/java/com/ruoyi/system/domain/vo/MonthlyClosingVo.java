package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 月度经营月结视图对象（M38 T38.4）
 * <p>月报页税费/月结锁定区展示 + 保存回显。
 *
 * @author ruoyi
 * @date 2026-10-20
 */
@Data
public class MonthlyClosingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;

    /** 月份（yyyy-MM） */
    private String month;

    /** 税费金额 */
    private BigDecimal taxAmount;

    /** 税率（%） */
    private BigDecimal taxRate;

    /** 计税方式：MANUAL/RATE */
    private String taxMode;

    /** 月结状态：OPEN/LOCKED */
    private String status;

    /** 是否已锁定（前端只读判定） */
    private Boolean locked;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockTime;

    private Long lockBy;

    /** 锁定操作人姓名 */
    private String lockByName;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
