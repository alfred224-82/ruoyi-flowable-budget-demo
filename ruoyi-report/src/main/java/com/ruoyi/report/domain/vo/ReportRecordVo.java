package com.ruoyi.report.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * AI报表记录VO
 *
 * @author ruoyi
 */
@Data
@ExcelIgnoreUnannotated
public class ReportRecordVo {

    @TableId
    @ExcelProperty("报表ID")
    private Long id;

    @ExcelProperty("报表类型")
    private String reportType;

    @ExcelProperty("报表名称")
    private String reportName;

    @ExcelProperty("关联预算单ID")
    private String sheetIds;

    @ExcelProperty("报表内容")
    private String reportContent;

    @ExcelProperty("导出格式")
    private String exportFormat;

    @ExcelProperty("文件路径")
    private String filePath;

    @ExcelProperty("生成状态")
    private String status;

    @ExcelProperty("错误信息")
    private String errorMessage;

    @ExcelProperty("生成耗时(ms)")
    private Long costTimeMs;

    @ExcelProperty("生成时间")
    private Date generateTime;

    @ExcelProperty("创建时间")
    private Date createTime;
}
