package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用途标签视图对象 fee_purpose_tag
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@ExcelIgnoreUnannotated
public class PurposeTagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 标签编码
     */
    @ExcelProperty(value = "标签编码")
    private String tagCode;

    /**
     * 标签名称
     */
    @ExcelProperty(value = "标签名称")
    private String tagName;

    /**
     * 标签图标
     */
    @ExcelProperty(value = "标签图标")
    private String tagIcon;

    /**
     * 默认会计大类
     */
    @ExcelProperty(value = "默认会计大类")
    private String defaultAccountingClass;

    /**
     * 是否要求项目编号(0否 1是)
     */
    @ExcelProperty(value = "是否要求项目编号")
    private Integer requireProject;

    /**
     * 排序号
     */
    @ExcelProperty(value = "排序号")
    private Integer sortOrder;

    /**
     * 状态(ENABLED/DISABLED)
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 创建者
     */
    @ExcelProperty(value = "创建者")
    private String createBy;

    /**
     * 创建时间
     */
    @ExcelProperty(value = "创建时间")
    private Date createTime;

    /**
     * 更新者
     */
    @ExcelProperty(value = "更新者")
    private String updateBy;

    /**
     * 更新时间
     */
    @ExcelProperty(value = "更新时间")
    private Date updateTime;
}
