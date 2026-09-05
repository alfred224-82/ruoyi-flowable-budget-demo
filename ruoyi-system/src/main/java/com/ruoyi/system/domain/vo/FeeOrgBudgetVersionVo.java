package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 组织级预算版本快照视图对象（T7.1 / FEAT_ORG_SNAPSHOT）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeOrgBudgetVersionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 组织ID */
    private Long orgId;

    /** 组织名称（关联填充） */
    private String orgName;

    /** 预算期间 yyyy-MM */
    private String period;

    /** 版本号 */
    private Integer versionNo;

    /** 状态 DRAFT/PENDING/LOCKED */
    private String status;

    /** 预算快照JSON */
    private String snapshotData;

    /** 锁定人 */
    private Long lockedBy;

    /** 锁定时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lockedAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
