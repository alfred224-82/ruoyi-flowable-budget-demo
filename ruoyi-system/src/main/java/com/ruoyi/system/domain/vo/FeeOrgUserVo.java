package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户-组织关系视图对象（T7.1 / FEAT_MULTI_ORG）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class FeeOrgUserVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 组织ID */
    private Long orgId;

    /** 组织名称（关联填充） */
    private String orgName;

    /** 用户ID */
    private Long userId;

    /** 用户姓名 */
    private String userName;

    /** 是否主组织 */
    private Integer isPrimary;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
