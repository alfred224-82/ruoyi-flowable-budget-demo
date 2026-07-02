package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import lombok.Data;
import java.util.Date;



/**
 * 预算科目视图对象 budget_subject
 *
 * @author ruoyi
 * @date 2026-05-30
 */
@Data
@ExcelIgnoreUnannotated
public class BudgetSubjectVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 科目编码（唯一，格式：一级1001，二级1001001）
     */
    @ExcelProperty(value = "科目编码", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "唯=一，格式：一级1001，二级1001001")
    private String subjectCode;

    /**
     * 科目名称
     */
    @ExcelProperty(value = "科目名称")
    private String subjectName;

    /**
     * 父级科目ID（顶级为0）
     */
    @ExcelProperty(value = "父级科目ID", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "顶=级为0")
    private Long parentId;

    /**
     * 父级科目编码（冗余字段，便于查询）
     */
    @ExcelProperty(value = "父级科目编码", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "冗=余字段，便于查询")
    private String parentCode;

    /**
     * 科目层级（1-一级科目，2-二级科目，3-三级科目）
     */
    @ExcelProperty(value = "科目层级", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "1=-一级科目，2-二级科目，3-三级科目")
    private Long level;

    /**
     * 祖级列表（如：0/1001/1001001）
     */
    @ExcelProperty(value = "祖级列表", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "如=：0/1001/1001001")
    private String ancestors;

    /**
     * 是否叶子节点（0-否有子科目，1-是无子科目）
     */
    @ExcelProperty(value = "是否叶子节点", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=-否有子科目，1-是无子科目")
    private Integer isLeaf;

    /**
     * 排序号（同级科目内的显示顺序）
     */
    @ExcelProperty(value = "排序号", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "同=级科目内的显示顺序")
    private Long sortOrder;

    /**
     * 是否启用（0-禁用，1-启用）
     */
    @ExcelProperty(value = "是否启用", converter = ExcelDictConvert.class)
    @ExcelDictFormat(readConverterExp = "0=-禁用，1-启用")
    private Integer isActive;

    /**
     * 科目说明
     */
    @ExcelProperty(value = "科目说明")
    private String description;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    private String remark;

    /**
     * 科目类型
     */
    @ExcelProperty(value = "科目类型")
    private String subjectType;


}
