package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 固定支出标签业务对象（T18.9，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class FixedCostTagBo implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    @NotBlank(message = "标签名称不能为空", groups = {AddGroup.class})
    private String tagName;

    private String tagCode;
    private String color;
    private Integer sortOrder;
    private String status;
    private String remark;
}
