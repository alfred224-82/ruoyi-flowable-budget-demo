package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 服务项目视图对象 service_item（T15.4/T15.11）
 * <p>{@code categoryName} 为服务层回填的分类名（来自 service_category.name），便于列表直接展示，非表字段。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class ServiceItemVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 服务分类ID */
    private Long categoryId;

    /** 服务分类名（回填，非表字段） */
    private String categoryName;

    /** 服务项目名称 */
    private String name;

    /** 单价 */
    private BigDecimal price;

    /** 单位（小时/斤/节/人/次/份） */
    private String unit;

    /** 计价方式：duration(计时)/package(套餐)/times(计量)/fixed(一口价)/weight/person/portion */
    private String pricingType;

    /** ★M22 业态差异配置JSON（quota/tableCount等）；★计价模式参数也存此列（includedDuration/overtimePrice等） */
    private String extraConfig;

    /** 封面图URL */
    private String coverUrl;

    /** 项目描述 */
    private String description;

    /** ★v1.40 可容纳人数（房间/空间用，项目为 NULL） */
    private Integer capacity;

    /** ★T26.2 是否有座位布局：0=无位置 1=有位置（房间/空间用） */
    private Integer hasSeatLayout;

    /** ★T26.2 可排座位数（has_seat_layout=1 时有效，房间/空间用） */
    private Integer seatCapacity;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0上架 1下架） */
    private String status;

    /** ★T21.5 资源状态：idle=空闲 / reserved=预约中 / in_use=使用中 */
    private String resourceStatus;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
