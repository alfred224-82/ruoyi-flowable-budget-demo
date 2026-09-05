package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 公司福利政策配置视图对象
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
@ExcelIgnoreUnannotated
public class FeeWelfareConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 福利编码
     */
    @ExcelProperty(value = "福利编码")
    private String welfareCode;

    /**
     * 福利名称
     */
    @ExcelProperty(value = "福利名称")
    private String welfareName;

    /**
     * 福利大类
     */
    @ExcelProperty(value = "福利大类")
    private String category;

    /**
     * 福利子类
     */
    @ExcelProperty(value = "福利子类")
    private String subCategory;

    /**
     * 适用区域
     */
    @ExcelProperty(value = "适用区域")
    private String region;

    /**
     * 出差时长类型
     */
    @ExcelProperty(value = "出差时长")
    private String tripDuration;

    /**
     * 城市等级
     */
    @ExcelProperty(value = "城市等级")
    private String cityLevel;

    /**
     * 国家分组
     */
    @ExcelProperty(value = "国家分组")
    private String countryGroup;

    /**
     * 补助标准金额
     */
    @ExcelProperty(value = "补助标准")
    private BigDecimal standardAmount;

    /**
     * 计量单位
     */
    @ExcelProperty(value = "计量单位")
    private String unit;

    /**
     * 最大享受天数/次数
     */
    @ExcelProperty(value = "最大限制")
    private Integer maxLimit;

    /**
     * 适用职级
     */
    @ExcelProperty(value = "适用职级")
    private String applicableRank;

    /**
     * 生效日期
     */
    @ExcelProperty(value = "生效日期")
    private Date effectiveDate;

    /**
     * 失效日期
     */
    @ExcelProperty(value = "失效日期")
    private Date expiryDate;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态")
    private String status;

    /**
     * 备注
     */
    private String remark;

    private Date createTime;

    private Date updateTime;
}
