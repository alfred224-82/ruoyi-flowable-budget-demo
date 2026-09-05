package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeExportTemplate;
import com.ruoyi.system.domain.FeePayment;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.vo.FeeExportTemplateVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeExportTemplateMapper;
import com.ruoyi.system.mapper.FeePaymentMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.service.IFeeVoucherExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 凭证导出Service实现（T5.11 / ADR-017/020）
 * 选月 + 选模板（内置7套，服务层手工租户隔离）+ 选范围 → 借贷两行 → Excel
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeVoucherExportServiceImpl implements IFeeVoucherExportService {

    private final FeeExportTemplateMapper templateMapper;
    private final ExpenseBillMapper billMapper;
    private final FeePaymentMapper paymentMapper;
    private final FeeProjectMapper projectMapper;

    @Override
    public List<FeeExportTemplateVo> listTemplates() {
        Long tenantId = LoginHelper.getTenantId();
        // 内置模板 tenant_id=0 跨租户可见，自定义模板仅本租户可见
        return templateMapper.selectVoList(Wrappers.<FeeExportTemplate>lambdaQuery()
            .and(w -> w.eq(FeeExportTemplate::getTenantId, 0L)
                .or().eq(tenantId != null, FeeExportTemplate::getTenantId, tenantId))
            .orderByAsc(FeeExportTemplate::getSortOrder));
    }

    @Override
    public byte[] exportVouchers(String month, Long templateId, Long projectId) {
        FeeExportTemplate template = loadTemplate(templateId);
        ExportData exportData = collectExportData(month, projectId);
        List<List<Object>> data = buildVoucherRows(exportData);
        if (data.isEmpty()) {
            throw new ServiceException("当月无可导出的已审批支出单据");
        }

        // EasyExcel 导出（表头按模板名称标注）
        List<List<String>> head = buildHead();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        EasyExcel.write(out)
            .head(head)
            .sheet(month + "-凭证-" + template.getTemplateName())
            .doWrite(data);
        return out.toByteArray();
    }

    @Override
    public byte[] exportVouchers2d(String month, Long templateId, Long projectId) {
        FeeExportTemplate template = loadTemplate(templateId);
        ExportData exportData = collectExportData(month, projectId);
        List<List<Object>> data = buildVoucherRows(exportData);
        if (data.isEmpty()) {
            throw new ServiceException("当月无可导出的已审批支出单据");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out).build();
        try {
            // Sheet1：凭证借贷两行（与基础版导出一致）
            WriteSheet sheet1 = EasyExcel.writerSheet(0, month + "-凭证-" + template.getTemplateName())
                .head(buildHead()).build();
            writer.write(data, sheet1);
            // Sheet2：项目×科目矩阵（含行列合计，合计与明细一致）
            MatrixData matrix = buildMatrix(exportData);
            WriteSheet sheet2 = EasyExcel.writerSheet(1, "项目×科目矩阵")
                .head(matrix.head).build();
            writer.write(matrix.rows, sheet2);
        } finally {
            writer.finish();
        }
        return out.toByteArray();
    }

    /**
     * 加载并校验导出模板（租户隔离：仅内置(0)+本租户模板可用，T6.6）
     */
    private FeeExportTemplate loadTemplate(Long templateId) {
        if (templateId == null) {
            throw new ServiceException("请选择导出模板");
        }
        FeeExportTemplate template = templateMapper.selectById(templateId);
        if (template == null) {
            throw new ServiceException("导出模板不存在");
        }
        Long tenantId = LoginHelper.getTenantId();
        if (template.getTenantId() != null && template.getTenantId() != 0L
            && !template.getTenantId().equals(tenantId)) {
            throw new ServiceException("导出模板不存在");
        }
        return template;
    }

    /**
     * 收集导出范围内已审批支出单据（报销单 + 付款单）
     */
    private ExportData collectExportData(String month, Long projectId) {
        if (StrUtil.isBlank(month)) {
            throw new ServiceException("请选择导出月份");
        }
        List<ExpenseBill> bills = billMapper.selectList(Wrappers.<ExpenseBill>lambdaQuery()
            .eq(ExpenseBill::getStatus, "APPROVED")
            .eq(ExpenseBill::getExpensePeriod, month)
            .eq(projectId != null, ExpenseBill::getProjectId, projectId));
        Date monthStart = DateUtil.parse(month + "-01", "yyyy-MM-dd");
        Date monthEnd = DateUtil.endOfMonth(monthStart);
        List<FeePayment> payments = paymentMapper.selectList(Wrappers.<FeePayment>lambdaQuery()
            .eq(FeePayment::getStatus, "APPROVED")
            .ge(FeePayment::getPaymentDate, monthStart)
            .le(FeePayment::getPaymentDate, monthEnd)
            .eq(projectId != null, FeePayment::getProjectId, projectId));
        ExportData exportData = new ExportData();
        exportData.bills = bills;
        exportData.payments = payments;
        exportData.projectNameMap = loadProjectNames(bills, payments);
        return exportData;
    }

    /**
     * 组装借贷两行数据
     */
    private List<List<Object>> buildVoucherRows(ExportData exportData) {
        List<List<Object>> data = new ArrayList<>();
        for (ExpenseBill bill : exportData.bills) {
            String subject = StrUtil.blankToDefault(bill.getCalcBudgetSubject(),
                StrUtil.blankToDefault(bill.getCalcAccountingClass(), "费用"));
            String projectName = exportData.projectNameMap.getOrDefault(bill.getProjectId(), "公司级");
            // 借方行
            data.add(row(bill.getBillNo(), bill.getExpenseDate(), "报销-" + StrUtil.nullToEmpty(bill.getBillNo()),
                subject, zero(bill.getAmount()), null, projectName));
            // 贷方行
            data.add(row(bill.getBillNo(), bill.getExpenseDate(), "报销-" + StrUtil.nullToEmpty(bill.getBillNo()),
                "银行存款", null, zero(bill.getAmount()), projectName));
        }
        for (FeePayment payment : exportData.payments) {
            String projectName = exportData.projectNameMap.getOrDefault(payment.getProjectId(), "公司级");
            data.add(row(payment.getPaymentNo(), payment.getPaymentDate(), "对外付款-" + StrUtil.nullToEmpty(payment.getPayee()),
                "项目支出-" + projectName, zero(payment.getAmount()), null, projectName));
            data.add(row(payment.getPaymentNo(), payment.getPaymentDate(), "对外付款-" + StrUtil.nullToEmpty(payment.getPayee()),
                "银行存款", null, zero(payment.getAmount()), projectName));
        }
        return data;
    }

    /**
     * 构建项目×科目矩阵（行=项目，列=科目，末行/末列为合计，合计与明细一致）
     */
    private MatrixData buildMatrix(ExportData exportData) {
        // 科目列：报销单取预算科目（兑底会计大类），付款单归入"对外付款"
        List<String> subjects = new ArrayList<>();
        Map<Long, Map<String, BigDecimal>> cell = new java.util.LinkedHashMap<>();
        for (ExpenseBill bill : exportData.bills) {
            String subject = StrUtil.blankToDefault(bill.getCalcBudgetSubject(),
                StrUtil.blankToDefault(bill.getCalcAccountingClass(), "费用"));
            if (!subjects.contains(subject)) {
                subjects.add(subject);
            }
            cell.computeIfAbsent(zeroProjectId(bill.getProjectId()), k -> new java.util.LinkedHashMap<>())
                .merge(subject, zero(bill.getAmount()), BigDecimal::add);
        }
        for (FeePayment payment : exportData.payments) {
            String subject = "对外付款";
            if (!subjects.contains(subject)) {
                subjects.add(subject);
            }
            cell.computeIfAbsent(zeroProjectId(payment.getProjectId()), k -> new java.util.LinkedHashMap<>())
                .merge(subject, zero(payment.getAmount()), BigDecimal::add);
        }

        // 表头：项目 + 各科目 + 合计
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("项目"));
        for (String subject : subjects) {
            head.add(Collections.singletonList(subject));
        }
        head.add(Collections.singletonList("合计"));

        // 数据行：每个项目一行 + 末行合计行（空单元格以 -- 占位，与设计文档 4.2.3 示例一致）
        List<List<Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> columnTotal = new java.util.LinkedHashMap<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Map<String, BigDecimal>> entry : cell.entrySet()) {
            List<Object> row = new ArrayList<>();
            row.add(exportData.projectNameMap.getOrDefault(entry.getKey(), "公司级"));
            BigDecimal rowTotal = BigDecimal.ZERO;
            for (String subject : subjects) {
                BigDecimal value = entry.getValue().get(subject);
                row.add(value == null ? "--" : value);
                if (value != null) {
                    rowTotal = rowTotal.add(value);
                    columnTotal.merge(subject, value, BigDecimal::add);
                }
            }
            row.add(rowTotal);
            grandTotal = grandTotal.add(rowTotal);
            rows.add(row);
        }
        List<Object> totalRow = new ArrayList<>();
        totalRow.add("合计");
        for (String subject : subjects) {
            BigDecimal value = columnTotal.get(subject);
            totalRow.add(value == null ? "--" : value);
        }
        totalRow.add(grandTotal);
        rows.add(totalRow);

        MatrixData matrix = new MatrixData();
        matrix.head = head;
        matrix.rows = rows;
        return matrix;
    }

    /**
     * 导出数据集合（报销单 + 付款单 + 项目名称映射）
     */
    private static class ExportData {
        List<ExpenseBill> bills;
        List<FeePayment> payments;
        Map<Long, String> projectNameMap;
    }

    /**
     * 矩阵数据（表头 + 数据行）
     */
    private static class MatrixData {
        List<List<String>> head;
        List<List<Object>> rows;
    }

    private Long zeroProjectId(Long projectId) {
        return projectId == null ? 0L : projectId;
    }

    /**
     * 标准凭证表头（各模板差异体现在文件名与备注，借贷结构统一）
     */
    private List<List<String>> buildHead() {
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("凭证号"));
        head.add(Collections.singletonList("日期"));
        head.add(Collections.singletonList("摘要"));
        head.add(Collections.singletonList("会计科目"));
        head.add(Collections.singletonList("借方金额"));
        head.add(Collections.singletonList("贷方金额"));
        head.add(Collections.singletonList("项目"));
        return head;
    }

    private List<Object> row(String no, Date date, String summary, String subject,
                             BigDecimal debit, BigDecimal credit, String projectName) {
        List<Object> row = new ArrayList<>();
        row.add(no);
        row.add(date == null ? "" : DateUtil.formatDate(date));
        row.add(summary);
        row.add(subject);
        row.add(debit == null ? "" : debit);
        row.add(credit == null ? "" : credit);
        row.add(projectName);
        return row;
    }

    /**
     * 汇总导出范围内的项目名称
     */
    private Map<Long, String> loadProjectNames(List<ExpenseBill> bills, List<FeePayment> payments) {
        List<Long> projectIds = new ArrayList<>();
        bills.forEach(b -> {
            if (b.getProjectId() != null && b.getProjectId() > 0) {
                projectIds.add(b.getProjectId());
            }
        });
        payments.forEach(p -> {
            if (p.getProjectId() != null && p.getProjectId() > 0) {
                projectIds.add(p.getProjectId());
            }
        });
        if (projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return projectMapper.selectBatchIds(projectIds.stream().distinct().collect(Collectors.toList())).stream()
            .collect(Collectors.toMap(FeeProject::getId, FeeProject::getProjectName, (a, b) -> a));
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
