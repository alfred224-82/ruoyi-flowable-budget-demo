package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 映射规则对象 fee_mapping_rule
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_mapping_rule")
public class MappingRule extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 物资小类编码
     */
    private String itemCategory;

    /**
     * 用途标签编码
     */
    private String purposeTag;

    /**
     * 目标预算科目编码
     */
    private String budgetSubject;

    /**
     * 归属会计大类
     */
    private String accountingClass;

    /**
     * 是否固定资产(0否 1是)
     */
    private Integer isFixedAsset;

    /**
     * 费用确认方式(DIRECT直接费用化/DEPRECIATION折旧/ACCRUAL计提摊销/FEE_RATE费率利息)
     */
    private String costMethod;

    /**
     * 优先级(数值越大优先级越高)
     */
    private Integer priority;

    /**
     * 状态(ENABLED/DISABLED)
     */
    private String status;

    /**
     * 发布状态(DRAFT草稿/PUBLISHED已发布，T6.4 / FEAT_RULE_ENGINE_CUSTOM)
     */
    private String publishStatus;

    /**
     * 发布时间
     */
    private Date publishTime;

    /**
     * 发布人
     */
    private Long publishedBy;
}
