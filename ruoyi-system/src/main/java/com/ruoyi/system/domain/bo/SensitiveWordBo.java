package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 敏感词业务对象 sensitive_word（M18 T18.1，图片合规与全局敏感词）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class SensitiveWordBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 敏感词文本 */
    @NotBlank(message = "敏感词不能为空", groups = {AddGroup.class})
    @Size(max = 100, message = "敏感词长度不能超过100个字符", groups = {AddGroup.class, EditGroup.class})
    private String word;

    /** 分类：POLITICAL/PORN/VIOLENCE/AD/SUPERSTITION/ABUSE/RUMOR/DIALECT/CUSTOM */
    @NotBlank(message = "分类不能为空", groups = {AddGroup.class})
    private String category;

    /** 匹配模式：exact/fuzzy/regex（默认 exact） */
    private String matchMode;

    /** 行业范围（JSON 数组文本，NULL=全行业通用） */
    private String industryScope;

    /** 状态（0启用 1禁用） */
    private String status;

    /** 来源：global/tenant（后端根据登录身份自动填充，前端不传） */
    private String source;

    private String remark;
}
