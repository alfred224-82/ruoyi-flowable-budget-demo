package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 存为模板业务对象（T26.5，小微版 M26）
 * <p>Boss 将当前房间的布局存为本租户私有模板（is_industry_default=0）；
 * 平台 Admin 通过 {@link RoomSeatLayoutTemplateBo} 建行业默认模板（is_industry_default=1）。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class SaveAsTemplateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 源布局ID（FK→room_seat_layout.id） */
    @NotNull(message = "布局ID不能为空", groups = {AddGroup.class})
    private Long layoutId;

    /** 模板名称（UNIQUE(tenant_id, name)） */
    @NotBlank(message = "模板名称不能为空", groups = {AddGroup.class})
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String templateName;
}
