package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 烂账记录视图对象 fee_bad_debt（T5.6）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeBadDebtVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 项目名称(关联展示) */
    private String projectName;
    /** 标记人 */
    private Long markedBy;
    /** 标记时间 */
    private Date markedAt;
    /** 原因说明 */
    private String reason;
    /** 烂账类型 */
    private String type;
    /** 处置建议(BAD_DEBT/FOLLOW_UP/LEGAL) */
    private String proposal;
    /** 计提坏账金额 */
    private BigDecimal provisionAmount;
    /** 状态(OPEN/FOLLOWING/CLOSED) */
    private String status;
    /** 跟进记录 */
    private String followUpNotes;
    /** 创建时间 */
    private Date createTime;
}
