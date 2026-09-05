package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeAccrualRecord;
import com.ruoyi.system.domain.FeeLoanLedger;
import com.ruoyi.system.mapper.FeeAccrualRecordMapper;
import com.ruoyi.system.mapper.FeeLoanLedgerMapper;
import com.ruoyi.system.service.ILoanLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 借款台账Service业务层处理
 * 设计文档 Phase 7 Task 7.2
 *
 * 利息计提逻辑：
 * monthly_interest = principal × annual_rate ÷ 12
 * 写入 fee_accrual_record（source_type=LOAN_INTEREST, track_type=EXPENSE）
 * 利息归入费用轨 → 仅软预警，不阻断设备采购
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanLedgerServiceImpl implements ILoanLedgerService {

    private final FeeLoanLedgerMapper loanLedgerMapper;
    private final FeeAccrualRecordMapper accrualRecordMapper;

    @Override
    public Boolean addLoan(FeeLoanLedger loan) {
        // 自动计算月利息 = 本金 × 年化利率 ÷ 12
        if (loan.getMonthlyInterest() == null && loan.getPrincipal() != null && loan.getAnnualRate() != null) {
            loan.setMonthlyInterest(loan.getPrincipal()
                .multiply(loan.getAnnualRate())
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP));
        }
        if (loan.getAccumulatedInterest() == null) {
            loan.setAccumulatedInterest(BigDecimal.ZERO);
        }
        if (loan.getStatus() == null) {
            loan.setStatus("ACTIVE");
        }
        if (loan.getInterestCycle() == null) {
            loan.setInterestCycle("MONTHLY");
        }
        if (loan.getBudgetSubjectCode() == null) {
            loan.setBudgetSubjectCode("3003001");
        }
        if (loan.getSource() == null) {
            loan.setSource("MANUAL");
        }
        return loanLedgerMapper.insert(loan) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int executeMonthlyInterest(String period) {
        log.info("[LoanInterest] 开始执行月度利息计提, period={}", period);
        int count = 0;
        Date periodDate = parsePeriod(period);

        // 查询所有 ACTIVE 借款
        List<FeeLoanLedger> activeLoans = loanLedgerMapper.selectList(
            Wrappers.<FeeLoanLedger>lambdaQuery()
                .eq(FeeLoanLedger::getStatus, "ACTIVE")
                .le(FeeLoanLedger::getStartDate, periodDate)
        );

        for (FeeLoanLedger loan : activeLoans) {
            try {
                // 检查是否已在本期计提过
                if (hasAccrued(period, loan.getId())) {
                    continue;
                }

                // 季付的只在季末月计提
                if ("QUARTERLY".equals(loan.getInterestCycle())) {
                    int month = Integer.parseInt(period.substring(5, 7));
                    if (month % 3 != 0) {
                        continue;
                    }
                }

                BigDecimal interestAmount = loan.getMonthlyInterest();
                // 季付则一次提3个月
                if ("QUARTERLY".equals(loan.getInterestCycle())) {
                    interestAmount = interestAmount.multiply(BigDecimal.valueOf(3));
                }

                // 写入计提记录
                FeeAccrualRecord record = new FeeAccrualRecord();
                record.setAccrualPeriod(period);
                record.setSourceType("LOAN_INTEREST");
                record.setSourceId(loan.getId());
                record.setSourceCode(loan.getLoanCode());
                record.setDeptId(loan.getDeptId());
                record.setBudgetSubjectCode(loan.getBudgetSubjectCode());
                record.setTrackType("EXPENSE");
                record.setAmount(interestAmount);
                record.setVoucherStatus("DRAFT");
                record.setCreateTime(new Date());
                accrualRecordMapper.insert(record);

                // 更新累计利息
                loan.setAccumulatedInterest(loan.getAccumulatedInterest().add(interestAmount));
                loanLedgerMapper.updateById(loan);
                count++;
            } catch (Exception e) {
                log.error("[LoanInterest] 借款{}利息计提失败: {}", loan.getLoanCode(), e.getMessage(), e);
            }
        }
        log.info("[LoanInterest] 利息计提完成, period={}, 计提{}笔", period, count);
        return count;
    }

    @Override
    public Boolean settle(Long id) {
        FeeLoanLedger loan = loanLedgerMapper.selectById(id);
        if (loan == null) {
            throw new ServiceException("借款记录不存在");
        }
        if (!"ACTIVE".equals(loan.getStatus()) && !"OVERDUE".equals(loan.getStatus())) {
            throw new ServiceException("仅正常或逾期状态的借款可以结清");
        }
        loan.setStatus("SETTLED");
        return loanLedgerMapper.updateById(loan) > 0;
    }

    @Override
    public void checkOverdue() {
        Date today = new Date();
        // 查询所有 ACTIVE 且到期日已过的借款
        List<FeeLoanLedger> overdueLoans = loanLedgerMapper.selectList(
            Wrappers.<FeeLoanLedger>lambdaQuery()
                .eq(FeeLoanLedger::getStatus, "ACTIVE")
                .lt(FeeLoanLedger::getEndDate, today)
        );
        for (FeeLoanLedger loan : overdueLoans) {
            loan.setStatus("OVERDUE");
            loanLedgerMapper.updateById(loan);
            log.warn("[LoanInterest] 借款{}已逾期，到期日={}", loan.getLoanCode(), loan.getEndDate());
        }
        if (!overdueLoans.isEmpty()) {
            log.warn("[LoanInterest] 逾期检测完成，共{}笔借款逾期", overdueLoans.size());
        }
    }

    @Override
    public Page<FeeLoanLedger> listLoans(String status, PageQuery pageQuery) {
        return loanLedgerMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeLoanLedger>lambdaQuery()
                .eq(StrUtil.isNotBlank(status), FeeLoanLedger::getStatus, status)
                .orderByDesc(FeeLoanLedger::getCreateTime)
        );
    }

    private boolean hasAccrued(String period, Long loanId) {
        return accrualRecordMapper.selectCount(
            Wrappers.<FeeAccrualRecord>lambdaQuery()
                .eq(FeeAccrualRecord::getAccrualPeriod, period)
                .eq(FeeAccrualRecord::getSourceType, "LOAN_INTEREST")
                .eq(FeeAccrualRecord::getSourceId, loanId)
                .ne(FeeAccrualRecord::getVoucherStatus, "VOIDED")
        ) > 0;
    }

    private Date parsePeriod(String period) {
        try {
            return new SimpleDateFormat("yyyy-MM").parse(period);
        } catch (ParseException e) {
            throw new ServiceException("期间格式错误，应为YYYY-MM: " + period);
        }
    }
}
