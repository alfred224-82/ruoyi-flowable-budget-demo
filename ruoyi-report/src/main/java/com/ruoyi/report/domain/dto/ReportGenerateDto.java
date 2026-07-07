package com.ruoyi.report.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * AI报表生成请求DTO
 *
 * @author ruoyi
 */
@Data
public class ReportGenerateDto {

    /**
     * 报表类型
     * MONTHLY_DETAIL - 部门月度预算明细报表
     * APPROVAL_SUMMARY - 审批汇总分析报表
     * YEARLY_FORECAST - 年度预算滚动预测报表
     * AUDIT_ARCHIVE - 审计归档标准报表
     */
    @NotBlank(message = "报表类型不能为空")
    private String reportType;

    /**
     * 预算单ID列表（指定生成哪些预算单的报表）
     */
    private List<Long> sheetIds;

    /**
     * 报表参数
     * 可选参数：
     * - scenario: 场景（BALANCED/OPTIMISTIC/PESSIMISTIC）
     * - includeHistory: 是否包含历史数据（true/false）
     * - exportFormat: 导出格式（PDF/EXCEL/WORD/HTML）
     * - year: 年度（YEARLY_FORECAST 时使用）
     * - months: 预测月数（YEARLY_FORECAST 时使用，默认3）
     */
    private Map<String, Object> parameters;
}
