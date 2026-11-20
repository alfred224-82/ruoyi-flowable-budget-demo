package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 房间座位布局模板视图对象（T26.5 / T26.6，小微版 M26）
 * <p>★Vo 约定：仅用 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class RoomSeatLayoutTemplateVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID（平台默认模板=1） */
    private Long tenantId;

    /** 模板名称 */
    private String name;

    /** 模板默认背景图 OSS URL */
    private String bgImageUrl;

    /** 是否行业默认模板：0=Boss 私有 1=平台配置 */
    private Integer isIndustryDefault;

    /** 行业编码 */
    private String industryCode;

    /** 行业名称（回填，非表字段） */
    private String industryName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
