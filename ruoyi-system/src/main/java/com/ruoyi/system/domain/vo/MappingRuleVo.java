package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 映射规则视图对象 fee_mapping_rule
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@ExcelIgnoreUnannotated
public class MappingRuleVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 规则编码
     */
    @ExcelProperty(value = "规则编码")
    private String ruleCode;

    /**
     * 物资小类编码
     */
    @ExcelProperty(value = "物资小类编码")
    private String itemCategory;

    /**
     * 用途标签编码
     */
    @ExcelProperty(value = "用途标签编码")
    private String purposeTag;

    /**
     * 适用责任中心编码(*=全部)
     */
    @ExcelProperty(value = "适用责任中心编码")
    private String costCenterGroup;

    /**
     * 目标预算科目编码
     */
    @ExcelProperty(value = "目标预算科目编码")
    private String budgetSubject;

    /**
     * 归属会计大类
     */
    @ExcelProperty(value = "归属会计大类")
    private String accountingClass;

    /**
     * 是否固定资产(0否 1是)
     */
    @ExcelProperty(value = "是否固定资产")
    private Integer isFixedAsset;

    /**
     * 优先级(数值越大优先级越高)
     */
    @ExcelProperty(value = "优先级")
    private Integer priority;

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
