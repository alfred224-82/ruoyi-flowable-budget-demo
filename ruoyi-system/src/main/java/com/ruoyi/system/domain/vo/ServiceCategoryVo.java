package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 服务分类视图对象 service_category（T15.4/T15.11）
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class ServiceCategoryVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 分类名称（房间/助教/蔬菜/肉菜/课程...） */
    private String name;

    /** 所属行业（industry_template.industry_code） */
    private String industryType;

    /** ★分类类型（V3.7 原子资源）：service=服务分类 / space=房间·空间分类 */
    private String categoryType;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
