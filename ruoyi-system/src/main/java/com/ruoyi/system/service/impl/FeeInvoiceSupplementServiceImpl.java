package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.FeeInvoiceSupplement;
import com.ruoyi.system.domain.vo.FeeInvoiceSupplementVo;
import com.ruoyi.system.mapper.ExpenseBillMapper;
import com.ruoyi.system.mapper.FeeInvoiceSupplementMapper;
import com.ruoyi.system.service.IEmployeeCreditService;
import com.ruoyi.system.service.IFeeInvoiceSupplementService;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.ITenantFeatureGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 补票记录Service实现（T5.8 / ADR-018）
 * 额度预占 → 补票转实扣 → 超时释放 + 异常标记
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeInvoiceSupplementServiceImpl implements IFeeInvoiceSupplementService {

    private final FeeInvoiceSupplementMapper supplementMapper;
    private final ExpenseBillMapper billMapper;
    private final IFeeNotificationService notificationService;
    // M6 T6.3 信用体系：补票期限/按时逾期事件联动（开关关闭时自动退化标准行为）
    private final IEmployeeCreditService creditService;
    private final ITenantFeatureGuard tenantFeatureGuard;

    /**
     * 无发票后补通道开关编码（T5.8 下移高级版，T6.8）
     */
    private static final String FEATURE_INVOICE_SUPPLEMENT = "FEAT_INVOICE_SUPPLEMENT";

    @Override
    public TableDataInfo<FeeInvoiceSupplementVo> queryPageList(String status, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeInvoiceSupplement> wrapper = Wrappers.<FeeInvoiceSupplement>lambdaQuery()
            .eq(StrUtil.isNotBlank(status), FeeInvoiceSupplement::getStatus, status)
            .orderByAsc(FeeInvoiceSupplement::getEstimatedDate);
        Page<FeeInvoiceSupplementVo> page = supplementMapper.selectVoPage(pageQuery.build(), wrapper);
        fillBillInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public FeeInvoiceSupplementVo queryById(Long id) {
        FeeInvoiceSupplementVo vo = supplementMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("补票记录不存在");
        }
        fillBillInfo(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createSupplement(Long billId, Date estimatedDate) {
        ExpenseBill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new ServiceException("关联报销单不存在");
        }
        // 同一报销单防重复预占
        Long exist = supplementMapper.selectCount(Wrappers.<FeeInvoiceSupplement>lambdaQuery()
            .eq(FeeInvoiceSupplement::getBillId, billId));
        if (exist != null && exist > 0) {
            throw new ServiceException("该报销单已存在补票记录，请勿重复创建");
        }
        FeeInvoiceSupplement supplement = new FeeInvoiceSupplement();
        supplement.setBillId(billId);
        // 额度预占 = 报销金额（待补票后转实扣）
        supplement.setReservedAmount(bill.getAmount());
        supplement.setStatus("PENDING");
        // 预计补票日期默认提交日 + 信用期限（T6.3：优秀14天/其余7天）
        supplement.setEstimatedDate(estimatedDate != null ? estimatedDate
            : DateUtil.offsetDay(new Date(), creditService.getSupplementDays(bill.getApplicantId())));
        supplementMapper.insert(supplement);
        return supplement.getId();
    }

    @Override
    public int auditSupplement(Long id, String auditReason) {
        if (StrUtil.isBlank(auditReason)) {
            throw new ServiceException("后补单财务审核理由必填");
        }
        FeeInvoiceSupplement supplement = supplementMapper.selectById(id);
        if (supplement == null) {
            throw new ServiceException("补票记录不存在");
        }
        FeeInvoiceSupplement update = new FeeInvoiceSupplement();
        update.setId(id);
        update.setAuditReason(auditReason);
        return supplementMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int supplementInvoice(Long id, String invoiceUrls) {
        if (StrUtil.isBlank(invoiceUrls)) {
            throw new ServiceException("请上传补票发票");
        }
        FeeInvoiceSupplement supplement = supplementMapper.selectById(id);
        if (supplement == null) {
            throw new ServiceException("补票记录不存在");
        }
        if ("COMPLETED".equals(supplement.getStatus())) {
            throw new ServiceException("该记录已完成补票，请勿重复操作");
        }
        FeeInvoiceSupplement update = new FeeInvoiceSupplement();
        update.setId(id);
        update.setInvoiceUrls(invoiceUrls);
        update.setSupplementDate(new Date());
        update.setStatus("COMPLETED");
        // 预占转实扣：保留 reservedAmount 作为实扣凭证金额，状态闭环
        int rows = supplementMapper.updateById(update);

        // T6.3 信用事件：预计日期内补票 → 按时+2（含连续奖励，开关关闭时内部跳过）
        if (update.getSupplementDate().compareTo(supplement.getEstimatedDate()) <= 0) {
            ExpenseBill bill = billMapper.selectById(supplement.getBillId());
            if (bill != null) {
                creditService.recordOnTime(bill.getApplicantId());
            }
        }

        try {
            notificationService.notifyRole("finance", "补票已完成",
                "报销单补票已完成，预占额度已转实扣",
                "SYSTEM", "INVOICE_SUPPLEMENT", id);
        } catch (Exception e) {
            log.warn("补票完成通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markOverdue(Long id) {
        FeeInvoiceSupplement supplement = supplementMapper.selectById(id);
        if (supplement == null) {
            throw new ServiceException("补票记录不存在");
        }
        if (!"PENDING".equals(supplement.getStatus())) {
            throw new ServiceException("仅待补状态的记录可标记超时");
        }
        FeeInvoiceSupplement update = new FeeInvoiceSupplement();
        update.setId(id);
        update.setStatus("OVERDUE");
        // 超时释放预占额度并异常标记
        update.setReservedAmount(BigDecimal.ZERO);
        int rows = supplementMapper.updateById(update);

        // T6.3 信用事件：补票逾期 → -5（开关关闭时内部跳过）
        ExpenseBill bill = billMapper.selectById(supplement.getBillId());
        if (bill != null) {
            creditService.recordOverdue(bill.getApplicantId());
        }

        try {
            notificationService.notifyRole("finance", "补票超时异常",
                "报销单补票已超预计日期，预占额度已释放，请财务跟进异常处理",
                "SYSTEM", "INVOICE_SUPPLEMENT", id);
        } catch (Exception e) {
            log.warn("补票超时通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanAndReleaseOverdue() {
        List<FeeInvoiceSupplement> overdues = supplementMapper.selectList(
            Wrappers.<FeeInvoiceSupplement>lambdaQuery()
                .eq(FeeInvoiceSupplement::getStatus, "PENDING")
                .lt(FeeInvoiceSupplement::getEstimatedDate, DateUtil.beginOfDay(new Date())));
        // T6.8 ③：仅对开通无发票后补(FEAT_INVOICE_SUPPLEMENT)的租户扫描，基础版租户跳过（无执行记录）
        Set<Long> enabledTenants = tenantFeatureGuard.enabledTenantIds(FEATURE_INVOICE_SUPPLEMENT);
        int count = 0;
        for (FeeInvoiceSupplement supplement : overdues) {
            if (!enabledTenants.contains(supplement.getTenantId())) {
                continue;
            }
            try {
                markOverdue(supplement.getId());
                count++;
            } catch (Exception e) {
                log.error("补票超时释放失败, id={}", supplement.getId(), e);
            }
        }
        return count;
    }

    /**
     * 批量填充报销单号与金额
     */
    private void fillBillInfo(List<FeeInvoiceSupplementVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> billIds = list.stream().map(FeeInvoiceSupplementVo::getBillId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (billIds.isEmpty()) {
            return;
        }
        Map<Long, ExpenseBill> billMap = billMapper.selectBatchIds(billIds).stream()
            .collect(Collectors.toMap(ExpenseBill::getId, Function.identity()));
        list.forEach(vo -> {
            ExpenseBill bill = billMap.get(vo.getBillId());
            if (bill != null) {
                vo.setBillNo(bill.getBillNo());
                vo.setBillAmount(bill.getAmount());
            }
        });
    }
}
