package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 行业模板业务对象 industry_template（T15.10 admin CRUD）
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class IndustryTemplateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @NotNull(message = "行业模板ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 行业编码（board_game/market/gym/spa...） */
    @NotBlank(message = "行业编码不能为空", groups = {AddGroup.class})
    private String industryCode;

    /** 行业名称 */
    @NotBlank(message = "行业名称不能为空", groups = {AddGroup.class})
    private String industryName;

    /** 行业图标 */
    private String icon;

    /** 预置数据 JSON（服务分类示例/结算规则/单位库/默认页面配置） */
    private String presetConfig;

    /** 排序 */
    private Integer sortOrder;

    /** 状态（0正常 1停用） */
    private String status;

    /** 是否可语音（0否 1是，三级控制总开关：admin→platform_admin→boss，T1.7/T18.1） */
    private Integer voiceEnabled;

    /** 备注 */
    private String remark;
}
