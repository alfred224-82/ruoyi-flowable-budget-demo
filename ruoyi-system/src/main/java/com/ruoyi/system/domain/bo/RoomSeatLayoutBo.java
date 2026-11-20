package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 房间座位布局保存业务对象（T26.1 / T26.4，小微版 M26）
 * <p>Boss PC 拖拽定位保存：布局主表信息 + 座位点批量列表；一次事务写入 room_seat_layout + room_seat。
 * <p>★roomId + tenantId 命中 UNIQUE(tenant_id, room_id)：同房间只有一份布局，重复保存走 UPDATE + 座位全量替换。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class RoomSeatLayoutBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     * <p>注意：saveLayout 接口通过 roomId 判定新增/编辑（UNIQUE(tenant_id, room_id)），
     * 不依赖此字段，故不加 @NotNull；Service 层已做 roomId 的 fail-fast 校验。
     */
    private Long id;

    /** 房间ID（FK→service_item.id, category_type=space） */
    @NotNull(message = "房间ID不能为空", groups = {AddGroup.class, EditGroup.class})
    private Long roomId;

    /** 背景图 OSS URL（T26.3 上传后回填） */
    @NotBlank(message = "背景图不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 500, message = "背景图URL长度不能超过500")
    private String bgImageUrl;

    /** 背景图来源：UPLOAD=Boss 自传 / PRESET=平台默认模板；缺省 UPLOAD */
    private String bgImageSource;

    /** 布局基准宽度（px），缺省 1920 */
    private Integer layoutWidth;

    /** 布局基准高度（px），缺省 1080 */
    private Integer layoutHeight;

    /** 来源模板ID（若从模板应用而来，留痕；复制/应用后脱钩，可空） */
    private Long templateId;

    /** 布局名称（存为模板时使用；本 Bo 保存布局时可为空） */
    @Size(max = 100, message = "布局名称长度不能超过100")
    private String templateName;

    /** 座位点批量列表（拖拽定位落位后一次性提交；空数组=清空所有座位） */
    @Valid
    private List<RoomSeatBo> seats;
}
