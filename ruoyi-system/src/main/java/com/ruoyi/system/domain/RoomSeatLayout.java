package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 房间座位布局 room_seat_layout（T26.1，小微版 M26）
 * <p>一个房间一份布局，UNIQUE(tenant_id, room_id) 兜底。坐标以背景图百分比存储，
 * 前端按容器实际尺寸换算像素，保证 PC / 手机 / 平板三端渲染一致。
 * <p>★bg_image_source 值域：
 * <ul>
 *   <li>UPLOAD=Boss 自传（原图展示，不叠加主题滤镜，M27 T27.3）</li>
 *   <li>PRESET=平台默认模板（透明底 SVG，跟随主题色渲染）</li>
 * </ul>
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("room_seat_layout")
public class RoomSeatLayout extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /** 背景图来源：Boss 自传 */
    public static final String SOURCE_UPLOAD = "UPLOAD";
    /** 背景图来源：平台默认模板 */
    public static final String SOURCE_PRESET = "PRESET";

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 房间ID（FK→service_item.id, category_type=space） */
    private Long roomId;

    /** 背景图 OSS URL */
    private String bgImageUrl;

    /** 背景图来源：UPLOAD=Boss 自传 / PRESET=平台默认模板 */
    private String bgImageSource;

    /** 布局基准宽度（px，坐标百分比换算基准） */
    private Integer layoutWidth;

    /** 布局基准高度（px，坐标百分比换算基准） */
    private Integer layoutHeight;

    /** 来源模板ID（复制/应用模板后脱钩，仅留痕） */
    private Long templateId;

    /** 是否作为该房间默认布局：0=否 1=是 */
    private Integer isDefaultTemplate;

    /** 布局名称（存为模板时使用） */
    private String templateName;
}
