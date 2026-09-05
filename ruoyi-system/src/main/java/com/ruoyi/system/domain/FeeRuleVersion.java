package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 规则引擎版本快照对象 fee_rule_version（T7.4 / FEAT_RULE_GRAYSCALE）
 * 规则灰度发布 + 锁定预算使用当时规则快照，规则迭代不腐蚀历史
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_rule_version")
public class FeeRuleVersion extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 规则版本号
     */
    private Integer versionNo;

    /**
     * 规则快照内容JSON
     */
    private String ruleSnapshot;

    /**
     * 灰度百分比(新规则默认10%)
     */
    private BigDecimal grayscalePct;

    /**
     * 灰度状态 GRAYSCALE灰度中/FULL全量/ROLLBACK回滚
     */
    private String grayscaleStatus;

    /**
     * 灰度期命中率(比对用)
     */
    private BigDecimal hitRate;

    /**
     * 发布人
     */
    private Long publishBy;

    /**
     * 发布时间
     */
    private Date publishAt;
}
