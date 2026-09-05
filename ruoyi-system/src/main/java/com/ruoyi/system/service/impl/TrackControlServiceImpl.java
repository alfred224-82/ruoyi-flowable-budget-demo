package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.FeeBudgetTrackConfig;
import com.ruoyi.system.mapper.FeeBudgetTrackConfigMapper;
import com.ruoyi.system.service.ITrackControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 三轨预算管控Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackControlServiceImpl implements ITrackControlService {

    private final FeeBudgetTrackConfigMapper trackConfigMapper;

    @Override
    public TrackCheckResult checkTrack(String budgetSubjectCode, BigDecimal amount, String industryCode) {
        // 1. 查询科目轨道配置
        FeeBudgetTrackConfig config = getTrackConfig(budgetSubjectCode, industryCode);
        if (config == null) {
            // 未配置轨道，默认通过（费用轨软预警）
            return TrackCheckResult.pass("EXPENSE");
        }

        String trackType = config.getTrackType();
        String controlMode = config.getControlMode();

        // 2. 计算该轨道下可用余额（简化实现：实际应查询同轨道所有科目预算总和 - 已使用总和）
        // TODO: 接入实际预算余额计算逻辑
        BigDecimal trackBalance = getTrackBalance(trackType, industryCode);
        if (trackBalance == null) {
            // 未配置预算，默认通过
            return TrackCheckResult.pass(trackType);
        }

        // 3. 判断余额是否充足
        if (trackBalance.compareTo(amount) >= 0) {
            return TrackCheckResult.pass(trackType);
        }

        // 4. 余额不足，按管控模式处理
        String message = String.format("【%s轨】预算余额不足，当前余额：¥%.2f，申请金额：¥%.2f",
            getTrackName(trackType), trackBalance, amount);

        if ("HARD".equals(controlMode)) {
            // 硬阻断：禁止提交
            return TrackCheckResult.blocked(message, trackType);
        } else {
            // 软预警：允许提交但标记预警
            return TrackCheckResult.warning(message, trackType);
        }
    }

    @Override
    public String getTrackType(String budgetSubjectCode, String industryCode) {
        FeeBudgetTrackConfig config = getTrackConfig(budgetSubjectCode, industryCode);
        return config != null ? config.getTrackType() : "EXPENSE";
    }

    /**
     * 获取科目轨道配置（行业优先，回退通用）
     */
    private FeeBudgetTrackConfig getTrackConfig(String budgetSubjectCode, String industryCode) {
        // 先查行业专属
        if (StrUtil.isNotBlank(industryCode) && !"*".equals(industryCode)) {
            FeeBudgetTrackConfig config = trackConfigMapper.selectOne(
                Wrappers.<FeeBudgetTrackConfig>lambdaQuery()
                    .eq(FeeBudgetTrackConfig::getBudgetSubjectCode, budgetSubjectCode)
                    .eq(FeeBudgetTrackConfig::getIndustryCode, industryCode)
                    .eq(FeeBudgetTrackConfig::getStatus, "ENABLED")
            );
            if (config != null) {
                return config;
            }
        }
        // 回退通用配置
        return trackConfigMapper.selectOne(
            Wrappers.<FeeBudgetTrackConfig>lambdaQuery()
                .eq(FeeBudgetTrackConfig::getBudgetSubjectCode, budgetSubjectCode)
                .eq(FeeBudgetTrackConfig::getIndustryCode, "*")
                .eq(FeeBudgetTrackConfig::getStatus, "ENABLED")
        );
    }

    /**
     * 获取轨道余额（简化实现）
     * TODO: 接入实际预算余额计算
     */
    private BigDecimal getTrackBalance(String trackType, String industryCode) {
        // 简化实现：返回null表示未配置预算限制
        return null;
    }

    private String getTrackName(String trackType) {
        switch (trackType) {
            case "EXPENSE": return "费用";
            case "CAPITAL": return "资本";
            case "CASHFLOW": return "现金";
            default: return trackType;
        }
    }
}
