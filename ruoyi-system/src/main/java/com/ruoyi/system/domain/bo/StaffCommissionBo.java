package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工提成比例配置业务对象 staff_commission（REQ-03）
 * <p>platform_admin PC 端新增/编辑员工提成配置；查询过滤：staffName/status。
 *
 * @author ruoyi
 * @date 2026-10-16
 */
@Data
public class StaffCommissionBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "提成配置ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 员工用户ID */
    @NotNull(message = "员工不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long staffUserId;

    /** 员工姓名（可空，服务端按 staffUserId 回填快照） */
    private String staffName;

    /** 提成比例（0~1 小数，如 0.1000=10%） */
    @NotNull(message = "提成比例不能为空", groups = {AddGroup.class, EditGroup.class})
    @DecimalMin(value = "0", message = "提成比例不能小于 0", groups = {AddGroup.class, EditGroup.class})
    @DecimalMax(value = "1", message = "提成比例不能大于 1（100%）", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal commissionRate;

    /** 基础工资（元/月） */
    @DecimalMin(value = "0", message = "基础工资不能小于 0", groups = {AddGroup.class, EditGroup.class})
    private BigDecimal baseSalary;

    /** 生效日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date effectiveDate;

    /** 状态（0-正常；1-停用） */
    private String status;

    /** 备注 */
    private String remark;
}
