package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.FeeAccrualRecord;
import com.ruoyi.system.domain.FeeAssetCard;
import com.ruoyi.system.domain.FeeContractLedger;
import com.ruoyi.system.mapper.FeeAccrualRecordMapper;
import com.ruoyi.system.mapper.FeeAssetCardMapper;
import com.ruoyi.system.mapper.FeeContractLedgerMapper;
import com.ruoyi.system.service.IAccrualService;
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
 * 周期计提引擎Service业务层处理
 * 设计文档 Phase 6 Task 6.4
 *
 * 核心逻辑：
 * 1. 资产折旧：月折旧 = (原值 - 原值×残值率) / 折旧月数
 * 2. 合同摊销：月摊销 = 合同总额 / 摊销月数
 * 3. 计提记录写入 fee_accrual_record，track_type 统一为 EXPENSE
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccrualServiceImpl implements IAccrualService {

    private final FeeAssetCardMapper assetCardMapper;
    private final FeeContractLedgerMapper contractLedgerMapper;
    private final FeeAccrualRecordMapper accrualRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int executeMonthlyAccrual(String period) {
        log.info("[AccrualEngine] 开始执行月度计提, period={}", period);
        int count = 0;

        // 1. 资产折旧计提
        count += accrueAssets(period);

        // 2. 合同摊销计提
        count += accrueContracts(period);

        log.info("[AccrualEngine] 月度计提完成, period={}, 总计提{}笔", period, count);
        return count;
    }

    /**
     * 资产折旧计提
     */
    private int accrueAssets(String period) {
        int count = 0;
        Date periodDate = parsePeriod(period);

        // 查询所有 ACTIVE 状态且启用日期在期间之前的资产
        List<FeeAssetCard> activeAssets = assetCardMapper.selectList(
            Wrappers.<FeeAssetCard>lambdaQuery()
                .eq(FeeAssetCard::getStatus, "ACTIVE")
                .le(FeeAssetCard::getStartDate, periodDate)
        );

        for (FeeAssetCard asset : activeAssets) {
            try {
                // 检查是否已在本期计提过（防重复）
                if (hasAccrued(period, "ASSET_DEPRECIATION", asset.getId())) {
                    continue;
                }

                // 计算应提总额 = 原值 - 原值×残值率
                BigDecimal residualValue = asset.getOriginalValue()
                    .multiply(asset.getResidualRate())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                BigDecimal totalDepreciable = asset.getOriginalValue().subtract(residualValue);

                // 检查是否已提足
                if (asset.getAccumulatedAmount().compareTo(totalDepreciable) >= 0) {
                    // 标记为已提足
                    asset.setStatus("FULLY_DEPRECIATED");
                    assetCardMapper.updateById(asset);
                    log.info("[AccrualEngine] 资产{}已提足折旧，标记FULLY_DEPRECIATED", asset.getAssetCode());
                    continue;
                }

                // 月折旧额
                BigDecimal monthlyAmount = asset.getMonthlyAmount();
                // 最后一期可能不足月折旧额
                BigDecimal remaining = totalDepreciable.subtract(asset.getAccumulatedAmount());
                if (remaining.compareTo(monthlyAmount) < 0) {
                    monthlyAmount = remaining;
                }

                // 写入计提记录
                FeeAccrualRecord record = buildRecord(period, "ASSET_DEPRECIATION",
                    asset.getId(), asset.getAssetCode(), asset.getDeptId(),
                    asset.getBudgetSubjectCode(), monthlyAmount);
                accrualRecordMapper.insert(record);

                // 更新累计折旧
                asset.setAccumulatedAmount(asset.getAccumulatedAmount().add(monthlyAmount));
                if (asset.getAccumulatedAmount().compareTo(totalDepreciable) >= 0) {
                    asset.setStatus("FULLY_DEPRECIATED");
                }
                assetCardMapper.updateById(asset);
                count++;
            } catch (Exception e) {
                log.error("[AccrualEngine] 资产{}计提失败: {}", asset.getAssetCode(), e.getMessage(), e);
            }
        }
        return count;
    }

    /**
     * 合同摊销计提
     */
    private int accrueContracts(String period) {
        int count = 0;
        Date periodDate = parsePeriod(period);

        // 查询所有 ACTIVE 且在有效期内的合同
        List<FeeContractLedger> activeContracts = contractLedgerMapper.selectList(
            Wrappers.<FeeContractLedger>lambdaQuery()
                .eq(FeeContractLedger::getStatus, "ACTIVE")
                .le(FeeContractLedger::getStartDate, periodDate)
                .ge(FeeContractLedger::getEndDate, periodDate)
        );

        for (FeeContractLedger contract : activeContracts) {
            try {
                // 检查是否已在本期计提过
                if (hasAccrued(period, "CONTRACT_AMORTIZE", contract.getId())) {
                    continue;
                }

                // 月摊销额
                BigDecimal monthlyAmount = contract.getMonthlyAmount();

                // 检查是否已摊完
                BigDecimal remaining = contract.getTotalAmount().subtract(contract.getAccumulatedAmount());
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                    contract.setStatus("COMPLETED");
                    contractLedgerMapper.updateById(contract);
                    continue;
                }
                if (remaining.compareTo(monthlyAmount) < 0) {
                    monthlyAmount = remaining;
                }

                // 写入计提记录
                FeeAccrualRecord record = buildRecord(period, "CONTRACT_AMORTIZE",
                    contract.getId(), contract.getContractCode(), contract.getDeptId(),
                    contract.getBudgetSubjectCode(), monthlyAmount);
                accrualRecordMapper.insert(record);

                // 更新累计摊销
                contract.setAccumulatedAmount(contract.getAccumulatedAmount().add(monthlyAmount));
                if (contract.getAccumulatedAmount().compareTo(contract.getTotalAmount()) >= 0) {
                    contract.setStatus("COMPLETED");
                }
                contractLedgerMapper.updateById(contract);
                count++;
            } catch (Exception e) {
                log.error("[AccrualEngine] 合同{}摊销失败: {}", contract.getContractCode(), e.getMessage(), e);
            }
        }
        return count;
    }

    @Override
    public Boolean confirmVoucher(Long accrualRecordId) {
        FeeAccrualRecord record = accrualRecordMapper.selectById(accrualRecordId);
        if (record == null) {
            throw new ServiceException("计提记录不存在");
        }
        if (!"DRAFT".equals(record.getVoucherStatus())) {
            throw new ServiceException("仅草稿状态的凭证可以确认");
        }
        record.setVoucherStatus("CONFIRMED");
        record.setConfirmedBy(LoginHelper.getUserId());
        record.setConfirmedTime(new Date());
        return accrualRecordMapper.updateById(record) > 0;
    }

    @Override
    public Boolean voidVoucher(Long accrualRecordId, String reason) {
        FeeAccrualRecord record = accrualRecordMapper.selectById(accrualRecordId);
        if (record == null) {
            throw new ServiceException("计提记录不存在");
        }
        if ("VOIDED".equals(record.getVoucherStatus())) {
            throw new ServiceException("凭证已作废，不可重复操作");
        }
        record.setVoucherStatus("VOIDED");
        record.setConfirmedBy(LoginHelper.getUserId());
        record.setConfirmedTime(new Date());
        return accrualRecordMapper.updateById(record) > 0;
    }

    @Override
    public Page<FeeAccrualRecord> listRecords(String period, String sourceType, PageQuery pageQuery) {
        return accrualRecordMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeAccrualRecord>lambdaQuery()
                .eq(StrUtil.isNotBlank(period), FeeAccrualRecord::getAccrualPeriod, period)
                .eq(StrUtil.isNotBlank(sourceType), FeeAccrualRecord::getSourceType, sourceType)
                .orderByDesc(FeeAccrualRecord::getCreateTime)
        );
    }

    /**
     * 检查是否已计提（防重复）
     */
    private boolean hasAccrued(String period, String sourceType, Long sourceId) {
        return accrualRecordMapper.selectCount(
            Wrappers.<FeeAccrualRecord>lambdaQuery()
                .eq(FeeAccrualRecord::getAccrualPeriod, period)
                .eq(FeeAccrualRecord::getSourceType, sourceType)
                .eq(FeeAccrualRecord::getSourceId, sourceId)
                .ne(FeeAccrualRecord::getVoucherStatus, "VOIDED")
        ) > 0;
    }

    /**
     * 构建计提记录
     */
    private FeeAccrualRecord buildRecord(String period, String sourceType, Long sourceId,
                                          String sourceCode, Long deptId,
                                          String budgetSubjectCode, BigDecimal amount) {
        FeeAccrualRecord record = new FeeAccrualRecord();
        record.setAccrualPeriod(period);
        record.setSourceType(sourceType);
        record.setSourceId(sourceId);
        record.setSourceCode(sourceCode);
        record.setDeptId(deptId);
        record.setBudgetSubjectCode(budgetSubjectCode);
        record.setTrackType("EXPENSE");
        record.setAmount(amount);
        record.setVoucherStatus("DRAFT");
        record.setCreateTime(new Date());
        return record;
    }

    /**
     * 解析期间字符串为日期
     */
    private Date parsePeriod(String period) {
        try {
            return new SimpleDateFormat("yyyy-MM").parse(period);
        } catch (ParseException e) {
            throw new ServiceException("期间格式错误，应为YYYY-MM: " + period);
        }
    }
}
