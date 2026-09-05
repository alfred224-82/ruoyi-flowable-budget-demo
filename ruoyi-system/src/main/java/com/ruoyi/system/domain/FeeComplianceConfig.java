package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发票合规级别配置对象 fee_compliance_config
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_compliance_config")
public class FeeComplianceConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /**
     * 预算科目编码
     */
    private String budgetSubjectCode;

    /**
     * 预算科目名称
     */
    private String budgetSubjectName;

    /**
     * 合规级别（SIMPLE/NORMAL/STRICT）
     */
    private String complianceLevel;

    /**
     * 行业编码（*=通用）
     */
    private String industryCode;

    /**
     * 严格模式是否需财务盖章确认（0否1是）
     */
    private Integer requireSeal;

    /**
     * 状态
     */
    private String status;
}
