package com.ruoyi.system.domain.bo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 公司福利政策配置业务对象
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Data
@ExcelIgnoreUnannotated
public class FeeWelfareConfigBo {

    /**
     * 主键（编辑时传入）
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 福利编码（租户内唯一）
     */
    @NotBlank(message = "福利编码不能为空")
    @ExcelProperty(value = "福利编码")
    private String welfareCode;

    /**
     * 福利名称
     */
    @NotBlank(message = "福利名称不能为空")
    @ExcelProperty(value = "福利名称")
    private String welfareName;

    /**
     * 福利大类：MEAL/TRAVEL/TRANSPORT/OTHER
     */
    @NotBlank(message = "福利大类不能为空")
    @ExcelProperty(value = "福利大类")
    private String category;

    /**
     * 福利子类
     */
    @ExcelProperty(value = "福利子类")
    private String subCategory;

    /**
     * 适用区域：DOMESTIC/INTERNATIONAL/ALL
     */
    @ExcelProperty(value = "适用区域")
    private String region;

    /**
     * 出差时长类型：SHORT/LONG/ANY
     */
    @ExcelProperty(value = "出差时长")
    private String tripDuration;

    /**
     * 城市等级：TIER1/TIER2/TIER3/ANY
     */
    @ExcelProperty(value = "城市等级")
    private String cityLevel;

    /**
     * 国家分组：HK_MO_TW/ASIA/EUROPE_AMERICA/OTHER/ANY
     */
    @ExcelProperty(value = "国家分组")
    private String countryGroup;

    /**
     * 补助标准金额
     */
    @NotNull(message = "补助标准金额不能为空")
    @ExcelProperty(value = "补助标准")
    private BigDecimal standardAmount;

    /**
     * 计量单位：DAY/MONTH/TIME/MEAL
     */
    @NotBlank(message = "计量单位不能为空")
    @ExcelProperty(value = "计量单位")
    private String unit;

    /**
     * 最大享受天数/次数（0=不限）
     */
    @ExcelProperty(value = "最大限制")
    private Integer maxLimit;

    /**
     * 适用职级：ALL/指定职级编码
     */
    @ExcelProperty(value = "适用职级")
    private String applicableRank;

    /**
     * 生效日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ExcelProperty(value = "生效日期")
    private Date effectiveDate;

    /**
     * 失效日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ExcelProperty(value = "失效日期")
    private Date expiryDate;

    /**
     * 状态：ENABLED/DISABLED
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
