package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 责任中心业务对象 fee_cost_center
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CostCenterBo extends BaseEntity {

    /**
     * 主键ID
     */
    @NotNull(message = "主键ID不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 责任中心编码
     */
    @NotBlank(message = "责任中心编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String centerCode;

    /**
     * 责任中心名称
     */
    @NotBlank(message = "责任中心名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String centerName;

    /**
     * 默认会计大类
     */
    private String defaultAccountingClass;

    /**
     * 是否待分摊中心(0否 1是)
     */
    private Integer isPending;

    /**
     * 状态(ENABLED/DISABLED)
     */
    private String status;
}
