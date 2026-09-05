package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 规则引擎版本快照视图对象（T7.4 / FEAT_RULE_GRAYSCALE）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeRuleVersionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 规则版本号 */
    private Integer versionNo;

    /** 规则快照内容JSON */
    private String ruleSnapshot;

    /** 快照内规则条数（解析填充） */
    private Integer ruleCount;

    /** 灰度百分比 */
    private BigDecimal grayscalePct;

    /** 灰度状态 GRAYSCALE灰度中/FULL全量/ROLLBACK回滚 */
    private String grayscaleStatus;

    /** 灰度期命中率 */
    private BigDecimal hitRate;

    /** 发布人 */
    private Long publishBy;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date publishAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
