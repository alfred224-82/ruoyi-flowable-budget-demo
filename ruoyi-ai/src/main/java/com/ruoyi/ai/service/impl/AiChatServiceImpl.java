package com.ruoyi.ai.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.ai.config.AiProperties;
import com.ruoyi.ai.domain.dto.AiChatRequest;
import com.ruoyi.ai.domain.dto.AiChatResponse;
import com.ruoyi.ai.service.AiChatService;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI对话服务实现（使用OkHttp直接调用DashScope Responses API）
 * <p>
 * 绕过LangChain4j，直接对接DashScope Responses API以支持qwen3.7-max等新模型。
 * 使用 previous_response_id 实现多轮对话上下文管理。
 *
 * @author ruoyi
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai", name = "enabled", havingValue = "true")
public class AiChatServiceImpl implements AiChatService {

    private final OkHttpClient aiOkHttpClient;
    private final AiProperties aiProperties;
    private final BudgetPreparationMapper budgetPreparationMapper;
    private final BudgetPreparationDetailMapper budgetPreparationDetailMapper;
    private final ObjectMapper objectMapper;

    /**
     * 会话记忆：sessionId -> 消息列表（role, content）
     */
    private final ConcurrentHashMap<String, List<Map<String, String>>> sessionMemory = new ConcurrentHashMap<>();

    /**
     * DashScope响应ID映射：sessionId -> responseId（用于previous_response_id多轮对话）
     */
    private final ConcurrentHashMap<String, String> sessionResponseIds = new ConcurrentHashMap<>();

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String sessionId = getOrCreateSessionId(request.getSessionId());

        try {
            ObjectNode requestBody = buildRequestBody(sessionId, request, false);
            String url = aiProperties.getBaseUrl() + "/responses";

            Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + aiProperties.getApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON_MEDIA_TYPE, requestBody.toString()))
                .build();

            try (Response response = aiOkHttpClient.newCall(httpRequest).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    log.error("DashScope API调用失败, code={}, body={}", response.code(), errorBody);
                    return AiChatResponse.of(sessionId, "抱歉，AI服务返回异常（HTTP " + response.code() + "），请检查API Key、模型名称或账户余额。");
                }

                String responseBody = response.body().string();
                JsonNode responseJson = objectMapper.readTree(responseBody);

                // 保存responseId用于后续多轮对话
                String responseId = responseJson.path("id").asText(null);
                if (StrUtil.isNotBlank(responseId)) {
                    sessionResponseIds.put(sessionId, responseId);
                }

                String replyText = extractTextFromOutput(responseJson);

                // 保存到会话记忆
                List<Map<String, String>> messages = sessionMemory.computeIfAbsent(sessionId, k -> new ArrayList<>());
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", request.getMessage());
                messages.add(userMsg);
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", replyText);
                messages.add(assistantMsg);
                trimMemory(sessionId);

                return AiChatResponse.of(sessionId, replyText);
            }
        } catch (Exception e) {
            log.error("AI对话失败, sessionId={}", sessionId, e);
            return AiChatResponse.of(sessionId, "抱歉，AI服务暂时不可用，请稍后再试。错误信息：" + e.getMessage());
        }
    }

    @Override
    public SseEmitter chatStream(AiChatRequest request) {
        String sessionId = getOrCreateSessionId(request.getSessionId());
        SseEmitter emitter = new SseEmitter(120000L);

        ObjectNode requestBody;
        try {
            requestBody = buildRequestBody(sessionId, request, true);
        } catch (Exception e) {
            log.error("构建请求失败, sessionId={}", sessionId, e);
            try {
                emitter.send(SseEmitter.event().name("error").data("构建请求失败：" + e.getMessage()));
                emitter.complete();
            } catch (IOException ex) {
                emitter.complete();
            }
            return emitter;
        }

        String url = aiProperties.getBaseUrl() + "/responses";
        Request httpRequest = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + aiProperties.getApiKey())
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(RequestBody.create(JSON_MEDIA_TYPE, requestBody.toString()))
            .build();

        StringBuilder fullReply = new StringBuilder();

        aiOkHttpClient.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("AI流式对话网络失败, sessionId={}", sessionId, e);
                try {
                    emitter.send(SseEmitter.event().name("error")
                        .data("AI服务连接失败：" + e.getMessage()));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.complete();
                }
            }

            @Override
            public void onResponse(Call call, Response response) {
                if (!response.isSuccessful()) {
                    try {
                        String errorBody = response.body() != null ? response.body().string() : "未知错误";
                        log.error("DashScope API返回错误, code={}, body={}", response.code(), errorBody);
                        emitter.send(SseEmitter.event().name("error")
                            .data("AI服务返回异常（HTTP " + response.code() + "），请检查API Key、模型名称或账户余额。"));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.complete();
                    }
                    return;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String jsonData = line.substring(6);
                            if ("[DONE]".equals(jsonData)) {
                                break;
                            }
                            try {
                                JsonNode event = objectMapper.readTree(jsonData);
                                String eventType = event.path("type").asText("");

                                if ("response.output_text.delta".equals(eventType)) {
                                    String delta = event.path("delta").asText("");
                                    if (StrUtil.isNotBlank(delta)) {
                                        emitter.send(SseEmitter.event().data(delta));
                                        fullReply.append(delta);
                                    }
                                } else if ("response.completed".equals(eventType)) {
                                    JsonNode respNode = event.path("response");
                                    String responseId = respNode.path("id").asText(null);
                                    if (StrUtil.isNotBlank(responseId)) {
                                        sessionResponseIds.put(sessionId, responseId);
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("解析SSE事件失败: {}", jsonData, e);
                            }
                        }
                    }

                    // 保存AI回复到会话记忆
                    List<Map<String, String>> messages = sessionMemory.computeIfAbsent(sessionId, k -> new ArrayList<>());
                    Map<String, String> userMsg2 = new HashMap<>();
                    userMsg2.put("role", "user");
                    userMsg2.put("content", request.getMessage());
                    messages.add(userMsg2);
                    Map<String, String> assistantMsg2 = new HashMap<>();
                    assistantMsg2.put("role", "assistant");
                    assistantMsg2.put("content", fullReply.toString());
                    messages.add(assistantMsg2);
                    trimMemory(sessionId);

                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (IOException e) {
                    log.warn("SSE流读取异常, sessionId={}", sessionId, e);
                    try {
                        emitter.send(SseEmitter.event().name("error").data("AI响应中断：" + e.getMessage()));
                    } catch (IOException ex) {
                        // ignore
                    }
                    emitter.complete();
                }
            }
        });

        return emitter;
    }

    @Override
    public void clearSession(String sessionId) {
        sessionMemory.remove(sessionId);
        sessionResponseIds.remove(sessionId);
        log.info("已清除AI会话: {}", sessionId);
    }

    // ======================== DashScope API 请求构建 ========================

    /**
     * 构建DashScope Responses API请求体
     */
    private ObjectNode buildRequestBody(String sessionId, AiChatRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", aiProperties.getModelName());
        body.put("stream", stream);
        body.put("temperature", aiProperties.getTemperature());
        body.put("max_output_tokens", aiProperties.getMaxTokens());

        // 系统提示词 + 预算数据上下文
        String systemPrompt = aiProperties.getSystemPrompt();
        if (request.getSheetId() != null) {
            String budgetContext = buildBudgetContext(request.getSheetId());
            if (StrUtil.isNotBlank(budgetContext)) {
                systemPrompt += "\n\n【当前用户正在查看的预算单数据】\n" + budgetContext;
            }
        } else if (isBudgetRelatedQuestion(request.getMessage())) {
            String globalContext = buildGlobalBudgetContext(request.getMessage());
            if (StrUtil.isNotBlank(globalContext)) {
                systemPrompt += "\n\n【系统中的预算数据】\n" + globalContext;
            }
        }
        body.put("instructions", systemPrompt);

        // 使用 previous_response_id 进行服务端多轮对话管理
        String previousResponseId = sessionResponseIds.get(sessionId);
        if (StrUtil.isNotBlank(previousResponseId)) {
            body.put("previous_response_id", previousResponseId);
        }

        // 构建 input 消息数组（对话历史 + 当前消息）
        ArrayNode inputArray = body.putArray("input");
        List<Map<String, String>> history = sessionMemory.getOrDefault(sessionId, Collections.emptyList());
        for (Map<String, String> msg : history) {
            ObjectNode msgNode = objectMapper.createObjectNode();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
            inputArray.add(msgNode);
        }

        ObjectNode currentMsg = objectMapper.createObjectNode();
        currentMsg.put("role", "user");
        currentMsg.put("content", request.getMessage());
        inputArray.add(currentMsg);

        return body;
    }

    // ======================== 响应解析 ========================

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
            return text.toString();
        }
        return "AI未返回有效内容";
    }

    // ======================== 会话管理 ========================

    private String getOrCreateSessionId(String sessionId) {
        return StrUtil.isNotBlank(sessionId) ? sessionId : IdUtil.fastSimpleUUID();
    }

    private void trimMemory(String sessionId) {
        List<Map<String, String>> messages = sessionMemory.get(sessionId);
        if (messages != null && messages.size() > aiProperties.getMemoryWindowSize() * 2) {
            int keepSize = aiProperties.getMemoryWindowSize() * 2;
            List<Map<String, String>> trimmed = new ArrayList<>(messages.subList(messages.size() - keepSize, messages.size()));
            sessionMemory.put(sessionId, trimmed);
        }
    }

    // ======================== 预算数据上下文构建 ========================

    private String buildBudgetContext(Long sheetId) {
        try {
            BudgetPreparation preparation = budgetPreparationMapper.selectById(sheetId);
            if (preparation == null) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("预算单号：").append(preparation.getSheetNo()).append("\n");
            sb.append("组织：").append(preparation.getOrgName()).append("\n");
            sb.append("预算期间：").append(preparation.getBudgetYear()).append("年")
                .append(preparation.getBudgetMonth()).append("月\n");
            sb.append("状态：").append(translateStatus(preparation.getStatus())).append("\n");
            sb.append("预算总额：").append(formatAmount(preparation.getTotalBudget())).append("\n");
            if (preparation.getTotalActual() != null) {
                sb.append("实际总额：").append(formatAmount(preparation.getTotalActual())).append("\n");
            }
            if (preparation.getVarianceRate() != null) {
                sb.append("差异率：").append(preparation.getVarianceRate()).append("%\n");
            }

            List<BudgetPreparationDetail> details = budgetPreparationDetailMapper.selectList(
                new LambdaQueryWrapper<BudgetPreparationDetail>()
                    .eq(BudgetPreparationDetail::getSheetId, sheetId)
                    .orderByAsc(BudgetPreparationDetail::getSubjectType)
                    .orderByAsc(BudgetPreparationDetail::getSortOrder)
            );

            if (!details.isEmpty()) {
                sb.append("\n【明细数据】\n");
                Map<String, List<BudgetPreparationDetail>> grouped = details.stream()
                    .collect(Collectors.groupingBy(BudgetPreparationDetail::getSubjectType));
                for (Map.Entry<String, List<BudgetPreparationDetail>> entry : grouped.entrySet()) {
                    String type = translateSubjectType(entry.getKey());
                    sb.append("\n").append(type).append("：\n");
                    BigDecimal typeTotal = BigDecimal.ZERO;
                    for (BudgetPreparationDetail d : entry.getValue()) {
                        sb.append("  - ").append(d.getSubjectName())
                            .append("(").append(d.getSubjectCode()).append(")")
                            .append("：预算 ").append(formatAmount(d.getBudgetAmount()));
                        if (d.getActualAmount() != null) {
                            sb.append("，实际 ").append(formatAmount(d.getActualAmount()));
                        }
                        sb.append("\n");
                        typeTotal = typeTotal.add(d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO);
                    }
                    sb.append("  小计：").append(formatAmount(typeTotal)).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建预算上下文失败, sheetId={}", sheetId, e);
            return "";
        }
    }

    private boolean isBudgetRelatedQuestion(String message) {
        if (StrUtil.isBlank(message)) return false;
        String[] keywords = {"预算", "科目", "编制", "审批", "审核", "部门预算", "执行", "差异",
            "超支", "结余", "收入", "费用", "成本", "资产", "负债", "权益"};
        for (String kw : keywords) {
            if (message.contains(kw)) return true;
        }
        return false;
    }

    private Integer extractYear(String message) {
        Matcher m = Pattern.compile("(\\d{4})\\s*年").matcher(message);
        if (m.find()) {
            int year = Integer.parseInt(m.group(1));
            if (year >= 2020 && year <= 2099) return year;
        }
        return null;
    }

    private Integer extractMonth(String message) {
        Matcher m = Pattern.compile("(\\d{1,2})\\s*月").matcher(message);
        if (m.find()) {
            int month = Integer.parseInt(m.group(1));
            if (month >= 1 && month <= 12) return month;
        }
        if (message.contains("本月") || message.contains("这个月")) return Calendar.getInstance().get(Calendar.MONTH) + 1;
        if (message.contains("上月") || message.contains("上个月")) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, -1);
            return cal.get(Calendar.MONTH) + 1;
        }
        return null;
    }

    private String buildGlobalBudgetContext(String message) {
        try {
            Integer year = extractYear(message);
            Integer month = extractMonth(message);

            LambdaQueryWrapper<BudgetPreparation> sheetQuery = new LambdaQueryWrapper<>();
            if (year != null) sheetQuery.eq(BudgetPreparation::getBudgetYear, year);
            if (month != null) sheetQuery.eq(BudgetPreparation::getBudgetMonth, month);
            sheetQuery.in(BudgetPreparation::getStatus, "Completed", "Approved", "Pending_Review");
            sheetQuery.last("LIMIT 50");

            List<BudgetPreparation> sheets = budgetPreparationMapper.selectList(sheetQuery);
            if (sheets.isEmpty()) {
                sheetQuery = new LambdaQueryWrapper<>();
                if (year != null) sheetQuery.eq(BudgetPreparation::getBudgetYear, year);
                if (month != null) sheetQuery.eq(BudgetPreparation::getBudgetMonth, month);
                sheetQuery.last("LIMIT 50");
                sheets = budgetPreparationMapper.selectList(sheetQuery);
            }
            if (sheets.isEmpty()) return buildDataSummaryHint(year, month);

            List<Long> sheetIds = sheets.stream().map(BudgetPreparation::getId).collect(Collectors.toList());
            LambdaQueryWrapper<BudgetPreparationDetail> detailQuery = new LambdaQueryWrapper<>();
            detailQuery.in(BudgetPreparationDetail::getSheetId, sheetIds);
            detailQuery.orderByDesc(BudgetPreparationDetail::getBudgetAmount);
            List<BudgetPreparationDetail> allDetails = budgetPreparationDetailMapper.selectList(detailQuery);
            if (allDetails.isEmpty()) return buildDataSummaryHint(year, month);

            StringBuilder sb = new StringBuilder();
            String periodDesc = (year != null ? year + "年" : "") + (month != null ? month + "月" : "");
            if (StrUtil.isBlank(periodDesc)) periodDesc = "所有期间";
            sb.append("查询范围：").append(periodDesc).append("\n");
            sb.append("预算单数量：").append(sheets.size()).append("个\n");

            Map<String, List<BudgetPreparationDetail>> bySubject = allDetails.stream()
                .filter(d -> StrUtil.isNotBlank(d.getSubjectCode()))
                .collect(Collectors.groupingBy(BudgetPreparationDetail::getSubjectCode));

            List<Map<String, Object>> subjectSummary = new ArrayList<>();
            for (Map.Entry<String, List<BudgetPreparationDetail>> entry : bySubject.entrySet()) {
                List<BudgetPreparationDetail> ds = entry.getValue();
                BigDecimal totalBudget = ds.stream()
                    .map(d -> d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal totalActual = ds.stream()
                    .map(d -> d.getActualAmount() != null ? d.getActualAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", entry.getKey());
                item.put("name", ds.get(0).getSubjectName());
                item.put("type", ds.get(0).getSubjectType());
                item.put("totalBudget", totalBudget);
                item.put("totalActual", totalActual);
                subjectSummary.add(item);
            }
            subjectSummary.sort((a, b) -> ((BigDecimal) b.get("totalBudget")).compareTo((BigDecimal) a.get("totalBudget")));

            sb.append("\n【按科目汇总（预算金额降序）】\n");
            int rank = 0;
            for (Map<String, Object> item : subjectSummary) {
                rank++;
                sb.append(rank).append(". ").append(item.get("name")).append("(").append(item.get("code")).append(")")
                    .append(" [").append(translateSubjectType((String) item.get("type"))).append("]")
                    .append(" 预算合计：").append(formatAmount((BigDecimal) item.get("totalBudget")));
                BigDecimal actual = (BigDecimal) item.get("totalActual");
                if (actual.compareTo(BigDecimal.ZERO) > 0) sb.append("，实际合计：").append(formatAmount(actual));
                sb.append("\n");
                if (rank >= 30) { sb.append("... 共").append(subjectSummary.size()).append("个科目，仅展示前30\n"); break; }
            }

            Map<String, BigDecimal> deptBudget = allDetails.stream()
                .filter(d -> StrUtil.isNotBlank(d.getDeptName()))
                .collect(Collectors.groupingBy(BudgetPreparationDetail::getDeptName,
                    Collectors.reducing(BigDecimal.ZERO, d -> d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO, BigDecimal::add)));
            if (!deptBudget.isEmpty()) {
                sb.append("\n【按部门汇总】\n");
                deptBudget.entrySet().stream().sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                    .forEach(e -> sb.append("  - ").append(e.getKey()).append("：").append(formatAmount(e.getValue())).append("\n"));
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建全局预算上下文失败", e);
            return "";
        }
    }

    private String buildDataSummaryHint(Integer year, Integer month) {
        try {
            long totalSheets = budgetPreparationMapper.selectCount(null);
            if (totalSheets == 0) return "当前系统中暂无预算数据。";
            String period = (year != null ? year + "年" : "") + (month != null ? month + "月" : "");
            return "系统中已有 " + totalSheets + " 条预算单，" + (StrUtil.isNotBlank(period) ? "但未找到" + period + "的数据。" : "");
        } catch (Exception e) { return ""; }
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
}
