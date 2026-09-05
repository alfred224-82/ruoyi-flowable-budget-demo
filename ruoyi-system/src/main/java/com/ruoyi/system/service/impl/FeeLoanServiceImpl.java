package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeLoan;
import com.ruoyi.system.domain.FeeLoanInterest;
import com.ruoyi.system.domain.vo.FeeLoanInterestVo;
import com.ruoyi.system.domain.vo.FeeLoanVo;
import com.ruoyi.system.mapper.FeeLoanInterestMapper;
import com.ruoyi.system.mapper.FeeLoanMapper;
import com.ruoyi.system.service.IFeeLoanService;
import com.ruoyi.system.service.ITenantFeatureGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 贷款Service实现（T5.9 / ADR-027）
 * 基础版简化：月息 = 贷款金额 * 年利率 / 12（等额本息暂不逐月递减本金）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeLoanServiceImpl implements IFeeLoanService {

    private final FeeLoanMapper loanMapper;
    private final FeeLoanInterestMapper interestMapper;
    private final ITenantFeatureGuard tenantFeatureGuard;

    /**
     * 隐形成本开关编码（T5.9 下移高级版，T6.8）
     */
    private static final String FEATURE_HIDDEN_COST = "FEAT_HIDDEN_COST";

    @Override
    public TableDataInfo<FeeLoanVo> queryPageList(String status, PageQuery pageQuery) {
        Page<FeeLoanVo> page = loanMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeLoan>lambdaQuery()
                .eq(StrUtil.isNotBlank(status), FeeLoan::getStatus, status)
                .orderByDesc(FeeLoan::getCreateTime));
        return TableDataInfo.build(page);
    }

    @Override
    public Long saveLoan(FeeLoan loan) {
        if (StrUtil.isBlank(loan.getLoanName()) || loan.getAmount() == null || loan.getAnnualRate() == null) {
            throw new ServiceException("贷款名称、金额与年利率不能为空");
        }
        if (StrUtil.isBlank(loan.getStatus())) {
            loan.setStatus("ACTIVE");
        }
        if (StrUtil.isBlank(loan.getMethod())) {
            loan.setMethod("INTEREST_FIRST");
        }
        if (loan.getStartDate() == null) {
            loan.setStartDate(new Date());
        }
        if (loan.getId() != null) {
            loanMapper.updateById(loan);
            return loan.getId();
        }
        loanMapper.insert(loan);
        return loan.getId();
    }

    @Override
    public int settleLoan(Long id) {
        FeeLoan loan = loanMapper.selectById(id);
        if (loan == null) {
            throw new ServiceException("贷款不存在");
        }
        FeeLoan update = new FeeLoan();
        update.setId(id);
        update.setStatus("SETTLED");
        update.setEndDate(new Date());
        return loanMapper.updateById(update);
    }

    @Override
    public TableDataInfo<FeeLoanInterestVo> queryInterestPageList(String accrualMonth, PageQuery pageQuery) {
        Page<FeeLoanInterestVo> page = interestMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeLoanInterest>lambdaQuery()
                .eq(StrUtil.isNotBlank(accrualMonth), FeeLoanInterest::getAccrualMonth, accrualMonth)
                .orderByDesc(FeeLoanInterest::getAccrualMonth)
                .orderByAsc(FeeLoanInterest::getId));
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int accrueInterests(String accrualMonth) {
        String month = StrUtil.isNotBlank(accrualMonth) ? accrualMonth
            : DateUtil.format(DateUtil.offsetMonth(new Date(), -1), "yyyy-MM");
        List<FeeLoan> actives = loanMapper.selectList(Wrappers.<FeeLoan>lambdaQuery()
            .eq(FeeLoan::getStatus, "ACTIVE"));
        // T6.8 ③：仅对开通隐形成本(FEAT_HIDDEN_COST)的租户计提，基础版租户跳过（无执行记录）
        Set<Long> enabledTenants = tenantFeatureGuard.enabledTenantIds(FEATURE_HIDDEN_COST);
        int count = 0;
        for (FeeLoan loan : actives) {
            if (!enabledTenants.contains(loan.getTenantId())) {
                continue;
            }
            // 幂等：同月同贷款仅一条
            Long exist = interestMapper.selectCount(Wrappers.<FeeLoanInterest>lambdaQuery()
                .eq(FeeLoanInterest::getAccrualMonth, month)
                .eq(FeeLoanInterest::getLoanId, loan.getId()));
            if (exist != null && exist > 0) {
                continue;
            }
            // 月息 = 贷款金额 * 年利率 / 12
            BigDecimal monthly = zero(loan.getAmount()).multiply(zero(loan.getAnnualRate()))
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            FeeLoanInterest interest = new FeeLoanInterest();
            interest.setTenantId(loan.getTenantId());
            interest.setLoanId(loan.getId());
            interest.setAccrualMonth(month);
            interest.setAmount(monthly);
            interestMapper.insert(interest);
            count++;
        }
        log.info("利息计提完成, 月份={}, 计提{}条", month, count);
        return count;
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
