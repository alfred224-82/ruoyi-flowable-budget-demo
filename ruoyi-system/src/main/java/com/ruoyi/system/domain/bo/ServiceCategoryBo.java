package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 服务分类业务对象 service_category（T15.4/T15.11）
 * <p>平台管理员维护：分类 CRUD + 停用/启用；查询按名称/行业/状态过滤。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class ServiceCategoryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "分类ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 所属行业（industry_template.industry_code，可空=通用） */
    private String industryType;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;
}
