package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 多账套业务对象 fee_account_set（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class FeeAccountSetBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账套ID */
    @NotNull(message = "账套ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 所属组织ID(空=租户级账套) */
    private Long orgId;

    /** 账套名称 */
    @NotBlank(message = "账套名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 账套编码(租户内唯一) */
    @NotBlank(message = "账套编码不能为空", groups = {AddGroup.class})
    private String code;

    /** 财年起始(MM-DD) */
    private String fiscalYearStart;

    /** 是否默认账套(0否 1是) */
    private Integer isDefault;

    /** 状态(ACTIVE/FROZEN/CLOSED) */
    private String status;

    /** 备注 */
    private String remark;
}
