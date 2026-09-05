package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeSalaryRecord;
import com.ruoyi.system.domain.FeeStaffSalary;
import com.ruoyi.system.domain.vo.FeeSalaryRecordVo;
import com.ruoyi.system.domain.vo.FeeStaffSalaryVo;
import com.ruoyi.system.mapper.FeeSalaryRecordMapper;
import com.ruoyi.system.mapper.FeeStaffSalaryMapper;
import com.ruoyi.system.service.IFeeStaffSalaryService;
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
 * 员工薪资Service实现（T5.9 / ADR-024，公司级方案A）
 * 每月计提：月薪 + 社保公司部分 + 公积金公司部分 + 其他补贴（公司级固定成本）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeStaffSalaryServiceImpl implements IFeeStaffSalaryService {

    private final FeeStaffSalaryMapper salaryMapper;
    private final FeeSalaryRecordMapper recordMapper;
    private final ITenantFeatureGuard tenantFeatureGuard;

    /**
     * 隐形成本开关编码（T5.9 下移高级版，T6.8）
     */
    private static final String FEATURE_HIDDEN_COST = "FEAT_HIDDEN_COST";

    @Override
    public TableDataInfo<FeeStaffSalaryVo> queryPageList(String status, PageQuery pageQuery) {
        Page<FeeStaffSalaryVo> page = salaryMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeStaffSalary>lambdaQuery()
                .eq(StrUtil.isNotBlank(status), FeeStaffSalary::getStatus, status)
                .orderByDesc(FeeStaffSalary::getCreateTime));
        return TableDataInfo.build(page);
    }

    @Override
    public Long saveSalary(FeeStaffSalary salary) {
        if (salary.getUserId() == null || salary.getMonthlyBase() == null) {
            throw new ServiceException("用户与月薪基数不能为空");
        }
        // 社保/公积金公司部分缺省时按比例自动计算
        salary.setSocialAmount(resolveAmount(salary.getSocialAmount(), salary.getMonthlyBase(), salary.getSocialRate()));
        salary.setProvidentAmount(resolveAmount(salary.getProvidentAmount(), salary.getMonthlyBase(), salary.getProvidentRate()));
        if (StrUtil.isBlank(salary.getStatus())) {
            salary.setStatus("ACTIVE");
        }
        if (salary.getEffectiveDate() == null) {
            salary.setEffectiveDate(new Date());
        }
        // userId 唯一（在职）
        FeeStaffSalary exist = salaryMapper.selectOne(Wrappers.<FeeStaffSalary>lambdaQuery()
            .eq(FeeStaffSalary::getUserId, salary.getUserId())
            .ne(salary.getId() != null, FeeStaffSalary::getId, salary.getId())
            .last("LIMIT 1"));
        if (exist != null) {
            salary.setId(exist.getId());
            salaryMapper.updateById(salary);
            return exist.getId();
        }
        salaryMapper.insert(salary);
        return salary.getId();
    }

    @Override
    public int resign(Long id) {
        FeeStaffSalary salary = salaryMapper.selectById(id);
        if (salary == null) {
            throw new ServiceException("薪资基数不存在");
        }
        FeeStaffSalary update = new FeeStaffSalary();
        update.setId(id);
        update.setStatus("RESIGNED");
        return salaryMapper.updateById(update);
    }

    @Override
    public TableDataInfo<FeeSalaryRecordVo> queryRecordPageList(String accrualMonth, PageQuery pageQuery) {
        Page<FeeSalaryRecordVo> page = recordMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeSalaryRecord>lambdaQuery()
                .eq(StrUtil.isNotBlank(accrualMonth), FeeSalaryRecord::getAccrualMonth, accrualMonth)
                .orderByDesc(FeeSalaryRecord::getAccrualMonth)
                .orderByAsc(FeeSalaryRecord::getId));
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int accrueSalaries(String accrualMonth) {
        String month = StrUtil.isNotBlank(accrualMonth) ? accrualMonth
            : DateUtil.format(DateUtil.offsetMonth(new Date(), -1), "yyyy-MM");
        List<FeeStaffSalary> actives = salaryMapper.selectList(
            Wrappers.<FeeStaffSalary>lambdaQuery().eq(FeeStaffSalary::getStatus, "ACTIVE"));
        // T6.8 ③：仅对开通隐形成本(FEAT_HIDDEN_COST)的租户计提，基础版租户跳过（无执行记录）
        Set<Long> enabledTenants = tenantFeatureGuard.enabledTenantIds(FEATURE_HIDDEN_COST);
        int count = 0;
        for (FeeStaffSalary salary : actives) {
            if (!enabledTenants.contains(salary.getTenantId())) {
                continue;
            }
            // 幂等：同月同人仅一条
            Long exist = recordMapper.selectCount(Wrappers.<FeeSalaryRecord>lambdaQuery()
                .eq(FeeSalaryRecord::getAccrualMonth, month)
                .eq(FeeSalaryRecord::getUserId, salary.getUserId()));
            if (exist != null && exist > 0) {
                continue;
            }
            BigDecimal social = zero(salary.getSocialAmount());
            BigDecimal provident = zero(salary.getProvidentAmount());
            BigDecimal allowance = zero(salary.getOtherAllowance());
            FeeSalaryRecord record = new FeeSalaryRecord();
            record.setTenantId(salary.getTenantId());
            record.setAccrualMonth(month);
            record.setUserId(salary.getUserId());
            record.setUserName(salary.getUserName());
            record.setBaseAmount(zero(salary.getMonthlyBase()));
            record.setSocialAmount(social);
            record.setProvidentAmount(provident);
            record.setAllowanceAmount(allowance);
            record.setTotalAmount(record.getBaseAmount().add(social).add(provident).add(allowance));
            recordMapper.insert(record);
            count++;
        }
        log.info("薪资计提完成, 月份={}, 计提{}条", month, count);
        return count;
    }

    /**
     * 金额缺省时按 基数*比例 计算
     */
    private BigDecimal resolveAmount(BigDecimal amount, BigDecimal base, BigDecimal rate) {
        if (amount != null) {
            return amount;
        }
        if (rate != null && base != null) {
            return base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
