package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeIncomeConfirmation;
import com.ruoyi.system.domain.FeePayment;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeReceipt;
import com.ruoyi.system.domain.vo.FeeProfitTimelineVo;
import com.ruoyi.system.domain.vo.FeeProjectProfitVo;
import com.ruoyi.system.domain.vo.FeeProjectVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeIncomeConfirmationMapper;
import com.ruoyi.system.mapper.FeePaymentMapper;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeReceiptMapper;
import com.ruoyi.system.service.IFeeProfitReportService;
import com.ruoyi.system.service.IFeeProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 项目利润报表Service实现（T6.5 / FEAT_PROFIT_REPORT）
 * 纸面/实际分离、利润拆解、收支时间线、进度条、按项目/月导出
 * 数据范围复用 {@link IFeeProjectService#scopedProjectIds()}（项目经理仅本人参与项目）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeProfitReportServiceImpl implements IFeeProfitReportService {

    private final FeeProjectMapper projectMapper;
    private final FeeIncomeConfirmationMapper incomeMapper;
    private final FeePaymentMapper paymentMapper;
    private final FeeReceiptMapper receiptMapper;
    private final ExpenseBillMapper billMapper;
    private final IFeeProjectService projectService;

    @Override
    public TableDataInfo<FeeProjectProfitVo> reportList(String projectName, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeProject> wrapper = Wrappers.<FeeProject>lambdaQuery()
            .like(StrUtil.isNotBlank(projectName), FeeProject::getProjectName, projectName)
            .eq(StrUtil.isNotBlank(status), FeeProject::getStatus, status)
            .orderByDesc(FeeProject::getCreateTime);
        List<Long> scoped = projectService.scopedProjectIds();
        if (scoped != null) {
            if (scoped.isEmpty()) {
                return TableDataInfo.build(new Page<>());
            }
            wrapper.in(FeeProject::getId, scoped);
        }
        Page<FeeProjectVo> page = projectMapper.selectVoPage(pageQuery.build(), wrapper);
        List<FeeProjectProfitVo> rows = new ArrayList<>();
        for (FeeProjectVo project : page.getRecords()) {
            rows.add(buildReportRow(project));
        }
        Page<FeeProjectProfitVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(rows);
        return TableDataInfo.build(result);
    }

    @Override
    public FeeProjectProfitVo reportDetail(Long projectId) {
        FeeProjectVo project = projectMapper.selectVoById(projectId);
        if (project == null) {
            throw new ServiceException("项目不存在");
        }
        // getProfitDetail 内部执行数据范围校验（项目经理查他人项目抛异常）
        return buildReportRow(project);
    }

    @Override
    public List<FeeProfitTimelineVo> timeline(Long projectId) {
        FeeProjectVo project = projectService.queryById(projectId);
        List<FeeProfitTimelineVo> events = new ArrayList<>();

        // 合同（含变更叠加）
        FeeProfitTimelineVo contract = new FeeProfitTimelineVo();
        contract.setEventType("CONTRACT");
        contract.setEventLabel("项目合同签订"
            + (project.getChangeCount() != null && project.getChangeCount() > 0
                ? "（含" + project.getChangeCount() + "次变更）" : ""));
        contract.setOccurDate(project.getCreateTime());
        contract.setAmount(zero(project.getCurrentContractAmount()));
        contract.setRefNo(project.getClientName());
        events.add(contract);

        // 收入确认（首付/进度款/验收按依据区分）
        List<FeeIncomeConfirmation> incomes = incomeMapper.selectList(
            Wrappers.<FeeIncomeConfirmation>lambdaQuery()
                .eq(FeeIncomeConfirmation::getProjectId, projectId)
                .orderByAsc(FeeIncomeConfirmation::getId));
        for (FeeIncomeConfirmation income : incomes) {
            FeeProfitTimelineVo event = new FeeProfitTimelineVo();
            event.setEventType("INCOME_CONFIRM");
            event.setEventLabel("收入确认-" + basisLabel(income.getBasisType()));
            event.setOccurDate(income.getVerifyAt() != null ? income.getVerifyAt() : income.getConfirmDate());
            event.setAmount(zero(income.getConfirmedAmount()));
            event.setRefNo(income.getBasisNo());
            event.setStatus(income.getVerifyStatus());
            events.add(event);
        }

        // 对外付款
        List<FeePayment> payments = paymentMapper.selectList(
            Wrappers.<FeePayment>lambdaQuery()
                .eq(FeePayment::getProjectId, projectId)
                .orderByAsc(FeePayment::getId));
        for (FeePayment payment : payments) {
            FeeProfitTimelineVo event = new FeeProfitTimelineVo();
            event.setEventType("PAYMENT");
            event.setEventLabel("对外付款-" + StrUtil.nullToEmpty(payment.getPayee()));
            event.setOccurDate(payment.getPaymentDate());
            event.setAmount(zero(payment.getAmount()));
            event.setRefNo(payment.getPaymentNo());
            event.setStatus(payment.getStatus());
            events.add(event);
        }

        // 收款
        List<FeeReceipt> receipts = receiptMapper.selectList(
            Wrappers.<FeeReceipt>lambdaQuery()
                .eq(FeeReceipt::getProjectId, projectId)
                .orderByAsc(FeeReceipt::getId));
        for (FeeReceipt receipt : receipts) {
            FeeProfitTimelineVo event = new FeeProfitTimelineVo();
            event.setEventType("RECEIPT");
            event.setEventLabel("收款-" + StrUtil.nullToEmpty(receipt.getPayer()));
            event.setOccurDate(receipt.getReceivedDate());
            event.setAmount(zero(receipt.getAmount()));
            event.setRefNo(receipt.getReceiptNo());
            event.setStatus(receipt.getStatus());
            events.add(event);
        }

        events.sort(Comparator.comparing(FeeProfitTimelineVo::getOccurDate,
            Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    @Override
    public byte[] exportReport(Long projectId, String month) {
        List<Long> scoped = projectService.scopedProjectIds();
        LambdaQueryWrapper<FeeProject> wrapper = Wrappers.<FeeProject>lambdaQuery()
            .eq(projectId != null, FeeProject::getId, projectId)
            .orderByDesc(FeeProject::getCreateTime);
        if (scoped != null) {
            if (scoped.isEmpty()) {
                throw new ServiceException("数据范围内无可导出的项目");
            }
            wrapper.in(FeeProject::getId, scoped);
        }
        List<FeeProjectVo> projects = projectMapper.selectVoList(wrapper);
        if (projects.isEmpty()) {
            throw new ServiceException("数据范围内无可导出的项目");
        }
        List<FeeProjectProfitVo> rows = projects.stream()
            .map(this::buildReportRow).collect(Collectors.toList());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ExcelWriter writer = EasyExcel.write(out).build();
        try {
            // Sheet1：利润报表（纸面/实际分离+进度）
            WriteSheet sheet1 = EasyExcel.writerSheet(0, "项目利润报表").head(buildReportHead()).build();
            writer.write(buildReportRows(rows), sheet1);
            // Sheet2：按月收支明细（选月时附加）
            if (StrUtil.isNotBlank(month)) {
                List<Long> projectIds = projects.stream().map(FeeProjectVo::getId).collect(Collectors.toList());
                WriteSheet sheet2 = EasyExcel.writerSheet(1, month + "-收支明细").head(buildDetailHead()).build();
                writer.write(buildMonthDetailRows(month, projectIds, projects), sheet2);
            }
        } finally {
            writer.finish();
        }
        return out.toByteArray();
    }

    /**
     * 组装报表行（利润拆解+进度条数据）
     */
    private FeeProjectProfitVo buildReportRow(FeeProjectVo project) {
        FeeProjectProfitVo vo = projectService.getProfitDetail(project.getId());
        vo.setProjectStatus(project.getStatus());
        vo.setProjectManagerName(project.getProjectManagerName());
        vo.setIncomeProgress(calcIncomeProgress(vo.getVerifiedIncome(), vo.getCurrentContractAmount()));
        return vo;
    }

    /**
     * 收入确认进度 = 已核验收入/合同额（0~1，合同额为0时进度为0）
     */
    private BigDecimal calcIncomeProgress(BigDecimal verifiedIncome, BigDecimal contractAmount) {
        if (contractAmount == null || contractAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal progress = zero(verifiedIncome)
            .divide(contractAmount, 4, RoundingMode.HALF_UP);
        return progress.compareTo(BigDecimal.ONE) > 0 ? BigDecimal.ONE : progress;
    }

    private List<List<String>> buildReportHead() {
        List<List<String>> head = new ArrayList<>();
        for (String column : new String[]{"项目名称", "项目经理", "状态", "当前合同额", "已核验收入",
            "待核验收入", "总支出", "折旧分摊", "项目分成", "坏账计提", "纸面利润", "实际利润",
            "利润率", "收入确认进度"}) {
            List<String> cell = new ArrayList<>();
            cell.add(column);
            head.add(cell);
        }
        return head;
    }

    private List<List<Object>> buildReportRows(List<FeeProjectProfitVo> rows) {
        List<List<Object>> data = new ArrayList<>();
        for (FeeProjectProfitVo vo : rows) {
            List<Object> row = new ArrayList<>();
            row.add(vo.getProjectName());
            row.add(vo.getProjectManagerName());
            row.add(vo.getProjectStatus());
            row.add(vo.getCurrentContractAmount());
            row.add(vo.getVerifiedIncome());
            row.add(vo.getPendingIncome());
            row.add(vo.getTotalExpense());
            row.add(vo.getDepreciationShare());
            row.add(vo.getSharingAmount());
            row.add(vo.getBadDebtProvision());
            row.add(vo.getPaperProfit());
            row.add(vo.getActualProfit());
            row.add(vo.getProfitRate());
            row.add(vo.getIncomeProgress());
            data.add(row);
        }
        return data;
    }

    private List<List<String>> buildDetailHead() {
        List<List<String>> head = new ArrayList<>();
        for (String column : new String[]{"日期", "项目名称", "类型", "单号/依据", "金额", "状态"}) {
            List<String> cell = new ArrayList<>();
            cell.add(column);
            head.add(cell);
        }
        return head;
    }

    /**
     * 按月收支明细行（收入确认/收款/付款/报销，限导出项目范围）
     */
    private List<List<Object>> buildMonthDetailRows(String month, List<Long> projectIds, List<FeeProjectVo> projects) {
        java.util.Map<Long, String> nameMap = projects.stream()
            .collect(Collectors.toMap(FeeProjectVo::getId, FeeProjectVo::getProjectName, (a, b) -> a));
        Date monthStart = DateUtil.parse(month + "-01", "yyyy-MM-dd");
        Date monthEnd = DateUtil.endOfMonth(monthStart);
        List<List<Object>> data = new ArrayList<>();

        List<FeeIncomeConfirmation> incomes = incomeMapper.selectList(
            Wrappers.<FeeIncomeConfirmation>lambdaQuery()
                .in(FeeIncomeConfirmation::getProjectId, projectIds)
                .eq(FeeIncomeConfirmation::getVerifyStatus, "VERIFIED")
                .ge(FeeIncomeConfirmation::getConfirmDate, monthStart)
                .le(FeeIncomeConfirmation::getConfirmDate, monthEnd));
        for (FeeIncomeConfirmation income : incomes) {
            data.add(detailRow(income.getConfirmDate(), nameMap.get(income.getProjectId()),
                "收入确认", income.getBasisNo(), zero(income.getConfirmedAmount()), income.getVerifyStatus()));
        }

        List<FeeReceipt> receipts = receiptMapper.selectList(
            Wrappers.<FeeReceipt>lambdaQuery()
                .in(FeeReceipt::getProjectId, projectIds)
                .eq(FeeReceipt::getStatus, "APPROVED")
                .ge(FeeReceipt::getReceivedDate, monthStart)
                .le(FeeReceipt::getReceivedDate, monthEnd));
        for (FeeReceipt receipt : receipts) {
            data.add(detailRow(receipt.getReceivedDate(), nameMap.get(receipt.getProjectId()),
                "收款", receipt.getReceiptNo(), zero(receipt.getAmount()), receipt.getStatus()));
        }

        List<FeePayment> payments = paymentMapper.selectList(
            Wrappers.<FeePayment>lambdaQuery()
                .in(FeePayment::getProjectId, projectIds)
                .eq(FeePayment::getStatus, "APPROVED")
                .ge(FeePayment::getPaymentDate, monthStart)
                .le(FeePayment::getPaymentDate, monthEnd));
        for (FeePayment payment : payments) {
            data.add(detailRow(payment.getPaymentDate(), nameMap.get(payment.getProjectId()),
                "付款", payment.getPaymentNo(), zero(payment.getAmount()), payment.getStatus()));
        }

        List<ExpenseBill> bills = billMapper.selectList(
            Wrappers.<ExpenseBill>lambdaQuery()
                .in(ExpenseBill::getProjectId, projectIds)
                .eq(ExpenseBill::getStatus, "APPROVED")
                .eq(ExpenseBill::getExpensePeriod, month));
        for (ExpenseBill bill : bills) {
            data.add(detailRow(bill.getExpenseDate(), nameMap.get(bill.getProjectId()),
                "报销", bill.getBillNo(), zero(bill.getAmount()), bill.getStatus()));
        }

        // 首列已格式化为 yyyy-MM-dd，字典序即时间序（null 排前）
        data.sort(Comparator.comparing(row -> row.get(0) == null ? "" : row.get(0).toString()));
        return data;
    }

    private List<Object> detailRow(Date date, String projectName, String type, String refNo,
                                   BigDecimal amount, String status) {
        List<Object> row = new ArrayList<>();
        row.add(date == null ? null : DateUtil.format(date, "yyyy-MM-dd"));
        row.add(projectName);
        row.add(type);
        row.add(refNo);
        row.add(amount);
        row.add(status);
        return row;
    }

    /**
     * 收入确认依据类型标签
     */
    private String basisLabel(String basisType) {
        if (basisType == null) {
            return "其他依据";
        }
        switch (basisType) {
            case "CONTRACT":
                return "合同依据";
            case "INVOICE":
                return "发票依据";
            case "RECEIPT":
                return "回单依据";
            default:
                return "其他依据";
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
