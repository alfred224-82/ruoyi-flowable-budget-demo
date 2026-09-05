package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 多账套对象 fee_account_set（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 * <p>专业版租户内支持多个独立账套，每账套独立科目表/预算版本/凭证导出；
 * org_id 关联 fee_org（多组织为账套的上级维度）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_account_set")
public class FeeAccountSet extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 账套ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 所属组织ID(fee_org，多组织为账套上级维度，空=租户级账套)
     */
    private Long orgId;

    /**
     * 账套名称
     */
    private String name;

    /**
     * 账套编码(租户内唯一)
     */
    private String code;

    /**
     * 财年起始(格式 MM-DD，如 01-01 自然年)
     */
    private String fiscalYearStart;

    /**
     * 是否默认账套(每租户至多一个，存量单据归属)
     */
    private Integer isDefault;

    /**
     * 状态(ACTIVE启用/FROZEN冻结/CLOSED已结账)
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
