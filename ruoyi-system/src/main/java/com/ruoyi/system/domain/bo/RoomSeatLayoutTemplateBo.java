package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 房间座位布局模板业务对象（T26.6，小微版 M26）
 * <p>平台 Admin 建行业默认模板：模板名称 + 默认背景图（透明底 SVG/PNG）+ 行业编码。
 * <p>★is_industry_default=1 时 industry_code 必填（Service 层校验）。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class RoomSeatLayoutTemplateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID（编辑时必填） */
    @NotNull(message = "模板ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 模板名称（UNIQUE(tenant_id, name)） */
    @NotBlank(message = "模板名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 100, message = "模板名称长度不能超过100")
    private String name;

    /** 模板默认背景图 OSS URL（透明底 PNG/SVG，M27 T27.2） */
    @Size(max = 500, message = "背景图URL长度不能超过500")
    private String bgImageUrl;

    /** 是否行业默认模板：0=否（Boss 私有）1=是（平台配置）；缺省 1 */
    private Integer isIndustryDefault;

    /** 行业编码（is_industry_default=1 时必填，FK→industry_template.industry_code） */
    @Size(max = 50, message = "行业编码长度不能超过50")
    private String industryCode;
}
