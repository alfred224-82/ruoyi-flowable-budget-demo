package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 服务项目对象 service_item（T15.4/T15.11，小微版）
 * <p>名称 + 价格 + 单位 + 计价方式；租户级表（含 tenant_id），多租户插件自动隔离。
 * <p>计价方式 pricing_type：duration(按时长)/weight(按重量)/times(按次)/person(按人)/portion(按份)，
 * 见 {@link com.ruoyi.system.domain.enums.PricingType}。改价自动写 price_change_log（T15.11）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("service_item")
public class ServiceItem extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务分类ID（FK→service_category）
     */
    private Long categoryId;

    /**
     * 服务项目名称（小房间/初级助教/有机蔬菜...）
     */
    private String name;

    /**
     * 单价
     */
    private BigDecimal price;

    /**
     * 单位（小时/斤/节/人/次/份）
     */
    private String unit;

    /**
     * 计价方式：duration/weight/times/person/portion
     */
    private String pricingType;

    /**
     * 封面图URL
     */
    private String coverUrl;

    /**
     * 项目描述
     */
    private String description;

    /**
     * ★M22 业态差异配置JSON（quota/tableCount/perTable 等，T11 报名成团用）
     */
    private String extraConfig;

    /**
     * ★v1.40 可容纳人数（房间/空间用，项目为 NULL；从 preset_config rooms[].capacity 解析）
     */
    private Integer capacity;

    /**
     * ★T26.2 是否有座位布局：0=无位置 1=有位置（房间/空间用，项目保持 0）
     * <p>新建房间默认值由行业模板 preset_config.rooms[].has_seat_layout 决定（IndustryTemplateServiceImpl 回填）。
     */
    private Integer hasSeatLayout;

    /**
     * ★T26.2 可排座位数（has_seat_layout=1 时有效，房间/空间用）
     */
    private Integer seatCapacity;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态（0上架 1下架）
     */
    private String status;

    /**
     * ★T21.5 资源状态：idle=空闲 / reserved=预约中 / in_use=使用中
     * （仅 space 分类和助教有实际意义，默认 idle）
     */
    private String resourceStatus;

    /**
     * 删除标志（0存在 2删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 备注
     */
    private String remark;
}
