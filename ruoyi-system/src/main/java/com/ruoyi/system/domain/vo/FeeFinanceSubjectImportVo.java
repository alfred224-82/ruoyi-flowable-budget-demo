package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 财务科目Excel导入模型（T5.16，支持金蝶/用友等7套模板通用格式）
 *
 * @author ruoyi
 * @date 2026-08-30
 */
@Data
@NoArgsConstructor
public class FeeFinanceSubjectImportVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 财务科目编码(客户自有)
     */
    @ExcelProperty(value = "科目编码")
    @NotBlank(message = "科目编码不能为空")
    private String subjectCode;

    /**
     * 财务科目名称
     */
    @ExcelProperty(value = "科目名称")
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;

    /**
     * 父级科目编码
     */
    @ExcelProperty(value = "父级科目编码")
    private String parentCode;

    /**
     * 科目层级
     */
    @ExcelProperty(value = "科目层级")
    private Integer subjectLevel;
}
