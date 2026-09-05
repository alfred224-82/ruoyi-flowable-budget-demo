package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 账户数据操作业务对象（T14.4 数据导出/账号注销）
 * <p>apply 用 actionType（DATA_EXPORT/ACCOUNT_DELETE）；handle 用 id + status(DONE/REJECTED) + resultRef。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class AccountActionBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID（处理时必填） */
    private Long id;

    /** 目标用户ID（申请时必填） */
    private Long userId;

    /** 操作类型：DATA_EXPORT/ACCOUNT_DELETE（申请时必填） */
    @NotBlank(message = "操作类型不能为空", groups = {AddGroup.class})
    private String actionType;

    /** 处理结果状态：DONE/REJECTED（处理时必填） */
    private String status;

    /** 结果引用(导出文件URL/注销凭证) */
    private String resultRef;

    /** 备注 */
    private String remark;
}
