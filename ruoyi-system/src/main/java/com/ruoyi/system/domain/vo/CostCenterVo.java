package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 责任中心视图对象 fee_cost_center
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Data
@ExcelIgnoreUnannotated
public class CostCenterVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    private Long id;

    /**
     * 责任中心编码
     */
    @ExcelProperty(value = "责任中心编码")
    private String centerCode;

    /**
     * 责任中心名称
     */
    @ExcelProperty(value = "责任中心名称")
    private String centerName;

    /**
     * 默认会计大类
     */
    @ExcelProperty(value = "默认会计大类")
    private String defaultAccountingClass;

    /**
     * 是否待分摊中心(0否 1是)
     */
    @ExcelProperty(value = "是否待分摊中心")
    private Integer isPending;

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
