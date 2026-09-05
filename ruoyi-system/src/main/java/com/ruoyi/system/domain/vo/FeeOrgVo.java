package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 组织树视图对象（T7.1 / FEAT_MULTI_ORG）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeOrgVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 组织编码 */
    private String orgCode;

    /** 组织名称 */
    private String orgName;

    /** 父组织ID(0=集团总部) */
    private Long parentId;

    /** 祖级列表 */
    private String ancestors;

    /** 负责人用户ID */
    private Long leaderUserId;

    /** 负责人姓名（关联填充） */
    private String leaderUserName;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 组织成员数（关联统计） */
    private Integer userCount;

    /** 子组织 */
    private List<FeeOrgVo> children = new ArrayList<>();
}
