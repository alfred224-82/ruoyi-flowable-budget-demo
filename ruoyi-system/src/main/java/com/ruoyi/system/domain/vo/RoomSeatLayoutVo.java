package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 房间座位布局视图对象（T26.1 / T26.4，小微版 M26）
 * <p>查询房间布局时返回：布局主表 + 座位点批量列表；供 PC 拖拽画布 / MP 座位图渲染。
 * <p>★Vo 约定：仅用 @Data + Serializable，禁用 @AutoMapper。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class RoomSeatLayoutVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 房间ID（FK→service_item.id） */
    private Long roomId;

    /** 房间名称（回填，非表字段） */
    private String roomName;

    /** 背景图 OSS URL */
    private String bgImageUrl;

    /** 背景图来源：UPLOAD / PRESET */
    private String bgImageSource;

    /** 布局基准宽度（px） */
    private Integer layoutWidth;

    /** 布局基准高度（px） */
    private Integer layoutHeight;

    /** 来源模板ID（复制/应用后脱钩，仅留痕） */
    private Long templateId;

    /** 是否作为该房间默认布局：0=否 1=是 */
    private Integer isDefaultTemplate;

    /** 布局名称（存为模板时使用） */
    private String templateName;

    /** 座位点批量列表（联表回填） */
    private List<RoomSeatVo> seats;

    /** 座位总数（回填，非表字段） */
    private Integer seatCount;

    /** 预置座位数（回填，非表字段） */
    private Integer presetCount;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
