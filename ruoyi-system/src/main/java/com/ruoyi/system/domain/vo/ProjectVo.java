package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 项目统一容器视图对象 project（T11.1）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class ProjectVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 业态包编码 */
    private String bizCode;

    /** 容器类型：activity/team_buy/general */
    private String type;

    /** 项目/活动名称 */
    private String name;

    /** 状态：DRAFT/PUBLISHED/ONGOING/FINISHED/CLOSED/CANCELLED */
    private String status;

    /** 负责人用户ID */
    private Long ownerUserId;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 业态差异字段 JSON */
    private String extraConfig;

    /** 备注 */
    private String remark;

    private Date createTime;

    private Date updateTime;
}
