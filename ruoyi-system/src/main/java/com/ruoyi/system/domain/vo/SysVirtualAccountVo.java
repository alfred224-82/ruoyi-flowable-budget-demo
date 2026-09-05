package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 租户虚拟账户视图对象 sys_virtual_account（T14.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysVirtualAccountVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 可用余额(元) */
    private BigDecimal balance;

    /** 冻结余额(提现审核中) */
    private BigDecimal frozenBalance;

    /** 累计入账 */
    private BigDecimal totalIncome;

    /** 累计提现 */
    private BigDecimal totalWithdrawn;

    /** 状态（0正常 1冻结） */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
