package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 责任中心-部门关联对象 fee_cost_center_dept
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_cost_center_dept")
public class CostCenterDept extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 责任中心编码
     */
    private String centerCode;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 部门名称(冗余)
     */
    private String deptName;
}
