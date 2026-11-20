package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 敏感词库业务对象 sensitive_word_library（T18.1/T18.2/T18.3，小微版 M18）
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class SensitiveWordLibraryBo implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空", groups = {EditGroup.class})
    private Long id;

    /** 敏感词内容 */
    @NotBlank(message = "敏感词不能为空", groups = {AddGroup.class})
    private String word;

    /** 敏感词分类 */
    private String category;

    /** 级别：WARN/BLOCK */
    private String level;

    /** 状态：ENABLED/DISABLED */
    private String status;

    private String remark;
}
