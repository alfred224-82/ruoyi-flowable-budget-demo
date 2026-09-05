package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目分成视图对象 fee_project_sharing（T5.9）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeProjectSharingVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 项目ID */
    private Long projectId;
    /** 项目名称(关联展示) */
    private String projectName;
    /** 分成对象用户ID */
    private Long shareToUserId;
    /** 分成对象姓名 */
    private String shareToUserName;
    /** 角色(INTRODUCER/PARTNER/CHANNEL) */
    private String role;
    /** 分成基数类型(CONTRACT/INCOME/PROFIT/FIXED) */
    private String baseType;
    /** 分成基数 */
    private BigDecimal baseAmount;
    /** 分成比例 */
    private BigDecimal ratio;
    /** 分成金额 */
    private BigDecimal shareAmount;
    /** 状态(PENDING/ACTIVE/PAID) */
    private String status;
    /** 依据附件 */
    private String basisUrl;
    /** 创建时间 */
    private Date createTime;
}
