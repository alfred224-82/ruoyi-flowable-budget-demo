package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 行业配置主数据对象 fee_industry_config
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_industry_config")
public class FeeIndustryConfig extends TenantEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 行业编码（INTERNET/REALESTATE/MANUFACTURING/FINANCE） */
    private String industryCode;

    /** 行业名称 */
    private String industryName;

    /** 行业开关组JSON */
    private String switches;

    /** 折旧默认参数JSON */
    private String depreciationDefaults;

    /** 预算管控阈值JSON */
    private String budgetThresholds;

    /** 行业合规拦截规则JSON */
    private String complianceRules;

    /** AI引导话术包JSON */
    private String aiPromptPack;

    /** 盈利提示文案包JSON */
    private String profitTipPack;

    /** 计提规则参数JSON */
    private String accrualParams;

    /** 状态（ENABLED/DISABLED） */
    private String status;
}
