package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 多账套视图对象 fee_account_set（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class FeeAccountSetVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 账套ID */
    private Long id;

    /** 所属组织ID */
    private Long orgId;

    /** 所属组织名称（关联填充） */
    private String orgName;

    /** 账套名称 */
    private String name;

    /** 账套编码 */
    private String code;

    /** 财年起始(MM-DD) */
    private String fiscalYearStart;

    /** 是否默认账套(0否 1是) */
    private Integer isDefault;

    /** 状态(ACTIVE/FROZEN/CLOSED) */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
