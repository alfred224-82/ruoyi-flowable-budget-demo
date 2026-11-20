package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 服务项目业务对象 service_item（T15.4/T15.11）
 * <p>平台管理员/老板维护：项目 CRUD（名称+价格+单位+计价方式）+ 停用/启用；
 * 改价（price 变更）由服务层自动写入 price_change_log（T15.11）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class ServiceItemBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "服务项目ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 服务分类ID */
    private Long categoryId;

    /** 服务项目名称 */
    @NotBlank(message = "服务项目名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String name;

    /** 单价 */
    @NotNull(message = "单价不能为空", groups = {AddGroup.class})
    private BigDecimal price;

    /** 单位（小时/斤/节/人/次/份） */
    private String unit;

    /** 计价方式：duration(计时)/package(套餐)/times(计量)/fixed(一口价)/weight/person/portion（默认 times） */
    private String pricingType;

    /** ★M22 业态差异配置JSON；★计价模式参数也存此列（includedDuration/overtimePrice等） */
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

    /** 备注 */
    private String remark;
}
