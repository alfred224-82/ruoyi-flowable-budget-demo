package com.ruoyi.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * AI报表生成记录实体
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_report_record")
public class ReportRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 报表类型
     */
    private String reportType;

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 关联预算单ID（逗号分隔，支持多预算单）
     */
    private String sheetIds;

    /**
     * 报表内容（AI生成的Markdown/HTML文本）
     */
    private String reportContent;

    /**
     * 导出格式（PDF/EXCEL/WORD/HTML）
     */
    private String exportFormat;

    /**
     * 文件路径（已生成文件时的存储路径）
     */
    private String filePath;

    /**
     * 生成状态（GENERATING-生成中/COMPLETED-已完成/FAILED-失败）
     */
    private String status;

    /**
     * 错误信息（生成失败时记录）
     */
    private String errorMessage;

    /**
     * 生成耗时（毫秒）
     */
    private Long costTimeMs;

    /**
     * 生成时间
     */
    private Date generateTime;
}
