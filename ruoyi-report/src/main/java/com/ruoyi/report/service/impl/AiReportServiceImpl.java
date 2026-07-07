package com.ruoyi.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.report.config.ReportProperties;
import com.ruoyi.report.domain.ReportRecord;
import com.ruoyi.report.domain.dto.ReportGenerateDto;
import com.ruoyi.report.domain.vo.ReportRecordVo;
import com.ruoyi.report.mapper.ReportRecordMapper;
import com.ruoyi.report.service.IAiReportService;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
/**
 * AI报表服务实现
 * <p>
 * 支持4类报表生成：
 * 1. MONTHLY_DETAIL  - 部门月度预算明细报表
 * 2. APPROVAL_SUMMARY - 审批汇总分析报表
 * 3. YEARLY_FORECAST  - 年度预算滚动预测报表
 * 4. AUDIT_ARCHIVE    - 审计归档标准报表
 * </p>
 *
 * @author ruoyi
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class AiReportServiceImpl implements IAiReportService {

    private final ReportRecordMapper reportRecordMapper;
    private final BudgetPreparationMapper budgetPreparationMapper;
    private final BudgetPreparationDetailMapper budgetPreparationDetailMapper;
    private final ReportProperties reportProperties;
    private final OkHttpClient reportOkHttpClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key:}")
    private String apiKey;

    public AiReportServiceImpl(
            ReportRecordMapper reportRecordMapper,
            BudgetPreparationMapper budgetPreparationMapper,
            BudgetPreparationDetailMapper budgetPreparationDetailMapper,
            ReportProperties reportProperties,
            @Qualifier("reportOkHttpClient") OkHttpClient reportOkHttpClient,
            ObjectMapper objectMapper) {
        this.reportRecordMapper = reportRecordMapper;
        this.budgetPreparationMapper = budgetPreparationMapper;
        this.budgetPreparationDetailMapper = budgetPreparationDetailMapper;
        this.reportProperties = reportProperties;
        this.reportOkHttpClient = reportOkHttpClient;
        this.objectMapper = objectMapper;
    }

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    // ==================== 报表类型常量 ====================
    private static final String REPORT_MONTHLY_DETAIL = "MONTHLY_DETAIL";
    private static final String REPORT_APPROVAL_SUMMARY = "APPROVAL_SUMMARY";
    private static final String REPORT_YEARLY_FORECAST = "YEARLY_FORECAST";
    private static final String REPORT_AUDIT_ARCHIVE = "AUDIT_ARCHIVE";

    @Override
    public ReportRecordVo generateReport(ReportGenerateDto dto) {
        String reportType = dto.getReportType();
        validateReportType(reportType);

        // 获取预算数据
        List<BudgetPreparation> sheets = loadBudgetSheets(dto.getSheetIds());
        if (sheets.isEmpty()) {
            throw new ServiceException("未找到指定的预算单数据");
        }

        List<Long> sheetIds = sheets.stream().map(BudgetPreparation::getId).collect(Collectors.toList());
        List<BudgetPreparationDetail> allDetails = loadBudgetDetails(sheetIds);

        // 构建报表名称
        String reportName = buildReportName(reportType, sheets);

        // 创建报表记录
        ReportRecord record = new ReportRecord();
        record.setReportType(reportType);
        record.setReportName(reportName);
        record.setSheetIds(sheetIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        record.setExportFormat(getParam(dto.getParameters(), "exportFormat", "HTML"));
        record.setStatus("GENERATING");
        record.setGenerateTime(new Date());
        reportRecordMapper.insert(record);

        long startTime = System.currentTimeMillis();

        try {
            // 构建 System Prompt
            String systemPrompt = buildSystemPrompt(reportType, sheets, allDetails, dto.getParameters());

            // 构建用户消息
            String userMessage = buildUserMessage(reportType, sheets, dto.getParameters());

            // 调用 AI API 生成报表
            String reportContent = callAiApi(systemPrompt, userMessage);

            long costTime = System.currentTimeMillis() - startTime;

            // 更新记录
            record.setReportContent(reportContent);
            record.setStatus("COMPLETED");
            record.setCostTimeMs(costTime);
            reportRecordMapper.updateById(record);

            return reportRecordMapper.selectVoById(record.getId());

        } catch (Exception e) {
            log.error("AI报表生成失败, reportType={}", reportType, e);
            record.setStatus("FAILED");
            record.setErrorMessage(e.getMessage());
            record.setCostTimeMs(System.currentTimeMillis() - startTime);
            reportRecordMapper.updateById(record);
            throw new ServiceException("报表生成失败：" + e.getMessage());
        }
    }

    @Override
    public TableDataInfo<ReportRecordVo> queryReportHistory(String reportType, PageQuery pageQuery) {
        LambdaQueryWrapper<ReportRecord> lqw = Wrappers.<ReportRecord>lambdaQuery()
            .eq(StrUtil.isNotBlank(reportType), ReportRecord::getReportType, reportType)
            .orderByDesc(ReportRecord::getCreateTime);
        Page<ReportRecordVo> result = reportRecordMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public ReportRecordVo getReportDetail(Long id) {
        return reportRecordMapper.selectVoById(id);
    }

    @Override
    public Boolean deleteReport(Long id) {
        return reportRecordMapper.deleteById(id) > 0;
    }

    @Override
    public List<Map<String, String>> getReportTypes() {
        List<Map<String, String>> types = new ArrayList<>();
        types.add(buildTypeMap(REPORT_MONTHLY_DETAIL, "部门月度预算明细报表", "完成编制时"));
        types.add(buildTypeMap(REPORT_APPROVAL_SUMMARY, "审批汇总分析报表", "批量提交时"));
        types.add(buildTypeMap(REPORT_YEARLY_FORECAST, "年度预算滚动预测报表", "手动/季度末"));
        types.add(buildTypeMap(REPORT_AUDIT_ARCHIVE, "审计归档标准报表", "审批通过后"));
        return types;
    }

    // ==================== 核心：AI API 调用 ====================

    /**
     * 调用 DashScope Responses API 生成报表内容
     */
    private String callAiApi(String systemPrompt, String userMessage) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", "qwen-plus");
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_output_tokens", 8192);
            requestBody.put("instructions", systemPrompt);

            ArrayNode inputArray = requestBody.putArray("input");
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            inputArray.add(userMsg);

            String url = "https://dashscope.aliyuncs.com/compatible-mode/v1/responses";
            // 从 Spring 环境获取 API Key
            String apiKey = getApiKey();
            if (StrUtil.isBlank(apiKey)) {
                throw new ServiceException("AI API Key 未配置，请检查 ai.api-key 配置项");
            }

            Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON_MEDIA_TYPE, requestBody.toString()))
                .build();

            try (Response response = reportOkHttpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    log.error("DashScope API调用失败, code={}, body={}", response.code(), errorBody);
                    throw new ServiceException("AI服务调用失败（HTTP " + response.code() + "）");
                }

                String responseBody = response.body().string();
                JsonNode responseJson = objectMapper.readTree(responseBody);
                return extractTextFromOutput(responseJson);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用AI API异常", e);
            throw new ServiceException("AI服务调用异常：" + e.getMessage());
        }
    }

    private String extractTextFromOutput(JsonNode responseJson) {
        JsonNode outputArray = responseJson.path("output");
        if (outputArray.isArray()) {
            StringBuilder text = new StringBuilder();
            for (JsonNode item : outputArray) {
                if ("message".equals(item.path("type").asText())) {
                    JsonNode contentArray = item.path("content");
                    if (contentArray.isArray()) {
                        for (JsonNode content : contentArray) {
                            if ("output_text".equals(content.path("type").asText())) {
                                text.append(content.path("text").asText(""));
                            }
                        }
                    }
                }
            }
            if (text.length() > 0) return text.toString();
        }
        return "报表生成失败：AI未返回有效内容";
    }

    // ==================== System Prompt 构建 ====================

    /**
     * 根据报表类型构建 System Prompt
     */
    private String buildSystemPrompt(String reportType, List<BudgetPreparation> sheets,
                                     List<BudgetPreparationDetail> allDetails, Map<String, Object> parameters) {
        StringBuilder prompt = new StringBuilder();

        switch (reportType) {
            case REPORT_MONTHLY_DETAIL:
                prompt.append(getMonthlyDetailPrompt());
                break;
            case REPORT_APPROVAL_SUMMARY:
                prompt.append(getApprovalSummaryPrompt());
                break;
            case REPORT_YEARLY_FORECAST:
                prompt.append(getYearlyForecastPrompt());
                break;
            case REPORT_AUDIT_ARCHIVE:
                prompt.append(getAuditArchivePrompt());
                break;
            default:
                prompt.append("你是一个企业全面预算管理系统的报表生成专家。请根据提供的数据生成专业的报表。");
        }

        // 附加数据上下文
        prompt.append("\n\n【报表数据】\n");
        prompt.append(buildDataSection(sheets, allDetails));

        return prompt.toString();
    }

    /**
     * MONTHLY_DETAIL: 部门月度预算明细报表 Prompt
     */
    private String getMonthlyDetailPrompt() {
        return "你是一个企业全面预算管理系统的报表生成专家。\n"
            + "请根据提供的预算编制数据，生成一份【部门月度预算明细报表】。\n\n"
            + "报表要求：\n"
            + "1. 报表标题包含预算期间和部门名称\n"
            + "2. 按科目类型分组（收入类、成本类、费用类、资产类、负债类、权益类）\n"
            + "3. 每个科目列出：科目编码、科目名称、预算金额、实际金额、差异金额、差异率\n"
            + "4. 每类科目有小计行\n"
            + "5. 底部有合计行\n"
            + "6. 金额使用千位符格式化，保留2位小数\n"
            + "7. 差异率 = (预算金额 - 实际金额) / 实际金额 × 100%\n"
            + "8. 使用Markdown表格格式输出\n"
            + "9. 报表末尾添加编制日期和编制人信息\n";
    }

    /**
     * APPROVAL_SUMMARY: 审批汇总分析报表 Prompt
     */
    private String getApprovalSummaryPrompt() {
        return "你是一个企业全面预算管理系统的报表生成专家。\n"
            + "请根据提供的多份预算编制数据，生成一份【审批汇总分析报表】。\n\n"
            + "报表要求：\n"
            + "1. 报表标题为\u201c审批汇总分析报表\u201d\n"
            + "2. 汇总统计：预算单总数、预算总额、实际总额、平均差异率\n"
            + "3. 按部门分组汇总：每个部门的预算单数、预算总额、实际总额、差异率\n"
            + "4. 按科目类型汇总：各类型科目的预算合计、实际合计\n"
            + "5. 异常分析：标注差异率超过20%的科目，给出风险提示\n"
            + "6. 审批状态分布：各状态的预算单数量\n"
            + "7. 使用Markdown表格格式输出\n"
            + "8. 金额使用千位符格式化，保留2位小数\n"
            + "9. 报表末尾添加生成日期\n";
    }

    /**
     * YEARLY_FORECAST: 年度预算滚动预测报表 Prompt
     */
    private String getYearlyForecastPrompt() {
        return "你是一个企业全面预算管理系统的报表生成专家。\n"
            + "请根据提供的预算数据，生成一份【年度预算滚动预测报表】。\n\n"
            + "报表要求：\n"
            + "1. 报表标题包含年度信息\n"
            + "2. 基于已有预算数据，分析各类科目的月度趋势\n"
            + "3. 对每个科目类型，计算月均预算、月均实际\n"
            + "4. 基于历史趋势，预测未来3个月的预算建议值\n"
            + "5. 预测方法说明：使用简单移动平均法（取最近3个月的实际值平均）\n"
            + "6. 如果历史数据不足，则基于当前预算金额按比例推算\n"
            + "7. 年度预算执行率分析：已执行月份的实际/预算比率\n"
            + "8. 使用Markdown表格格式输出\n"
            + "9. 金额使用千位符格式化，保留2位小数\n"
            + "10. 报表末尾添加预测说明和生成日期\n";
    }

    /**
     * AUDIT_ARCHIVE: 审计归档标准报表 Prompt
     */
    private String getAuditArchivePrompt() {
        return "你是一个企业全面预算管理系统的报表生成专家。\n"
            + "请根据提供的预算数据，生成一份【审计归档标准报表】。\n\n"
            + "报表要求：\n"
            + "1. 报表标题为\u201c审计归档标准报表\u201d\n"
            + "2. 报表包含以下章节：\n"
            + "   一、预算编制概况（预算单号、编制部门、预算期间、编制人、审批状态）\n"
            + "   二、预算执行情况汇总（按科目类型分组的预算/实际/差异汇总）\n"
            + "   三、重大差异说明（差异率超过15%的科目逐一说明）\n"
            + "   四、审批流程记录（审批阶段、各级审批意见）\n"
            + "   五、合规性检查（预算金额是否为负、科目编码是否有效等）\n"
            + "3. 每个章节使用Markdown标题分隔\n"
            + "4. 数据部分使用Markdown表格\n"
            + "5. 金额使用千位符格式化，保留2位小数\n"
            + "6. 报表末尾添加归档日期、审计备注栏\n"
            + "7. 语言正式、规范，符合审计文档要求\n";
    }

    // ==================== 数据构建 ====================

    /**
     * 构建数据区域（供 Prompt 使用）
     */
    private String buildDataSection(List<BudgetPreparation> sheets, List<BudgetPreparationDetail> allDetails) {
        StringBuilder sb = new StringBuilder();

        // 预算单信息
        sb.append("预算单列表（共").append(sheets.size()).append("份）：\n");
        for (BudgetPreparation sheet : sheets) {
            sb.append("  - 单号：").append(sheet.getSheetNo());
            sb.append("，部门：").append(sheet.getOrgName());
            sb.append("，期间：").append(sheet.getBudgetYear()).append("年").append(sheet.getBudgetMonth()).append("月");
            sb.append("，状态：").append(translateStatus(sheet.getStatus()));
            sb.append("，预算总额：").append(formatAmount(sheet.getTotalBudget()));
            if (sheet.getTotalActual() != null) {
                sb.append("，实际总额：").append(formatAmount(sheet.getTotalActual()));
            }
            sb.append("\n");
        }

        // 明细数据按科目类型分组
        if (!allDetails.isEmpty()) {
            sb.append("\n明细数据（共").append(allDetails.size()).append("条）：\n");
            Map<String, List<BudgetPreparationDetail>> grouped = allDetails.stream()
                .filter(d -> StrUtil.isNotBlank(d.getSubjectType()))
                .collect(Collectors.groupingBy(BudgetPreparationDetail::getSubjectType));

            for (Map.Entry<String, List<BudgetPreparationDetail>> entry : grouped.entrySet()) {
                sb.append("\n[").append(translateSubjectType(entry.getKey())).append("]\n");
                BigDecimal typeTotalBudget = BigDecimal.ZERO;
                BigDecimal typeTotalActual = BigDecimal.ZERO;
                for (BudgetPreparationDetail d : entry.getValue()) {
                    sb.append("  ").append(d.getSubjectCode()).append(" | ").append(d.getSubjectName());
                    sb.append(" | 预算：").append(formatAmount(d.getBudgetAmount()));
                    sb.append(" | 实际：").append(formatAmount(d.getActualAmount()));
                    if (d.getDeptName() != null) {
                        sb.append(" | 部门：").append(d.getDeptName());
                    }
                    sb.append("\n");
                    typeTotalBudget = typeTotalBudget.add(d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO);
                    typeTotalActual = typeTotalActual.add(d.getActualAmount() != null ? d.getActualAmount() : BigDecimal.ZERO);
                }
                sb.append("  小计：预算 ").append(formatAmount(typeTotalBudget))
                    .append("，实际 ").append(formatAmount(typeTotalActual)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 构建用户消息
     */
    private String buildUserMessage(String reportType, List<BudgetPreparation> sheets, Map<String, Object> parameters) {
        StringBuilder msg = new StringBuilder();
        String period = sheets.get(0).getBudgetYear() + "年" + sheets.get(0).getBudgetMonth() + "月";

        switch (reportType) {
            case REPORT_MONTHLY_DETAIL:
                msg.append("请根据以上数据生成").append(period).append("的部门月度预算明细报表。");
                if (sheets.size() > 1) {
                    msg.append("共涉及").append(sheets.size()).append("个部门。");
                }
                break;
            case REPORT_APPROVAL_SUMMARY:
                msg.append("请根据以上").append(sheets.size()).append("份预算单数据生成审批汇总分析报表。");
                break;
            case REPORT_YEARLY_FORECAST:
                int year = sheets.get(0).getBudgetYear();
                int forecastMonths = getParamInt(parameters, "months", 3);
                msg.append("请根据以上数据生成").append(year).append("年度预算滚动预测报表，预测未来").append(forecastMonths).append("个月。");
                break;
            case REPORT_AUDIT_ARCHIVE:
                msg.append("请根据以上数据生成审计归档标准报表，包含所有").append(sheets.size()).append("份预算单的完整审计信息。");
                break;
            default:
                msg.append("请根据以上数据生成报表。");
        }

        return msg.toString();
    }

    // ==================== 辅助方法 ====================

    private List<BudgetPreparation> loadBudgetSheets(List<Long> sheetIds) {
        if (sheetIds != null && !sheetIds.isEmpty()) {
            return budgetPreparationMapper.selectBatchIds(sheetIds);
        }
        // 未指定ID时，取最近50条有数据的预算单
        LambdaQueryWrapper<BudgetPreparation> lqw = Wrappers.<BudgetPreparation>lambdaQuery()
            .in(BudgetPreparation::getStatus, "Completed", "Approved", "Pending_Review")
            .orderByDesc(BudgetPreparation::getCreateTime)
            .last("LIMIT 50");
        return budgetPreparationMapper.selectList(lqw);
    }

    private List<BudgetPreparationDetail> loadBudgetDetails(List<Long> sheetIds) {
        if (sheetIds == null || sheetIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<BudgetPreparationDetail> lqw = Wrappers.<BudgetPreparationDetail>lambdaQuery()
            .in(BudgetPreparationDetail::getSheetId, sheetIds)
            .orderByAsc(BudgetPreparationDetail::getSubjectType)
            .orderByAsc(BudgetPreparationDetail::getSortOrder);
        return budgetPreparationDetailMapper.selectList(lqw);
    }

    private void validateReportType(String reportType) {
        Set<String> validTypes = new HashSet<>(Arrays.asList(
            REPORT_MONTHLY_DETAIL, REPORT_APPROVAL_SUMMARY, REPORT_YEARLY_FORECAST, REPORT_AUDIT_ARCHIVE
        ));
        if (!validTypes.contains(reportType)) {
            throw new ServiceException("不支持的报表类型：" + reportType
                + "，支持的类型：MONTHLY_DETAIL, APPROVAL_SUMMARY, YEARLY_FORECAST, AUDIT_ARCHIVE");
        }
    }

    private String buildReportName(String reportType, List<BudgetPreparation> sheets) {
        String typeName;
        switch (reportType) {
            case REPORT_MONTHLY_DETAIL: typeName = "部门月度预算明细报表"; break;
            case REPORT_APPROVAL_SUMMARY: typeName = "审批汇总分析报表"; break;
            case REPORT_YEARLY_FORECAST: typeName = "年度预算滚动预测报表"; break;
            case REPORT_AUDIT_ARCHIVE: typeName = "审计归档标准报表"; break;
            default: typeName = "报表";
        }
        BudgetPreparation first = sheets.get(0);
        return first.getBudgetYear() + "年" + first.getBudgetMonth() + "月 " + typeName;
    }

    private String getApiKey() {
        return apiKey;
    }

    private String translateStatus(String status) {
        if (status == null) return "未知";
        switch (status) {
            case "Draft": return "草稿";
            case "Completed": return "完成编制";
            case "Pending_Review": return "待审核";
            case "Pending_Revision": return "待修订";
            case "Approved": return "已批准";
            case "Rejected": return "已驳回";
            default: return status;
        }
    }

    private String translateSubjectType(String type) {
        if (type == null) return "未知";
        switch (type) {
            case "INCOME": return "收入类";
            case "COST": return "成本类";
            case "EXPENSE": return "费用类";
            case "ASSET": return "资产类";
            case "LIABILITY": return "负债类";
            case "EQUITY": return "权益类";
            default: return type;
        }
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }

    @SuppressWarnings("unchecked")
    private <T> T getParam(Map<String, Object> params, String key, T defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        try {
            return (T) params.get(key);
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    private int getParamInt(Map<String, Object> params, String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object val = params.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Map<String, String> buildTypeMap(String code, String name, String trigger) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("code", code);
        map.put("name", name);
        map.put("trigger", trigger);
        return map;
    }
}
