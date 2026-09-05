package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.FeeProfitTipConfig;
import com.ruoyi.system.mapper.FeeProfitTipConfigMapper;
import com.ruoyi.system.service.IProfitTipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 盈利提示Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitTipServiceImpl implements IProfitTipService {

    private final FeeProfitTipConfigMapper profitTipConfigMapper;

    @Override
    public ProfitTipResult getTip(String budgetSubjectCode, String industryCode, BigDecimal amount) {
        // 1. 根据科目编码判断科目类型（简化实现：根据编码前缀判断）
        String subjectType = getSubjectType(budgetSubjectCode);

        // 2. 查询盈利提示配置（行业优先，回退通用）
        FeeProfitTipConfig config = getConfig(subjectType, industryCode);

        // 3. 构建默认提示
        String tipText;
        String tipColor;
        String tipType;
        boolean forcePopup = false;

        if (config != null) {
            tipText = config.getTipText();
            tipColor = config.getTipColor();
            tipType = config.getTipType();
            // 大额判断
            if (config.getLargeAmountThreshold() != null && amount != null
                && amount.compareTo(config.getLargeAmountThreshold()) > 0) {
                forcePopup = true;
            }
            if (config.getForcePopup() != null && config.getForcePopup() == 1) {
                forcePopup = true;
            }
        } else {
            // 默认提示
            switch (subjectType) {
                case "INCOME":
                    tipText = "该笔收入将增加当期净利润";
                    tipColor = "#67C23A";
                    tipType = "INCREASE";
                    break;
                case "EXPENSE":
                    tipText = "该笔支出将扣减当期净利润";
                    tipColor = "#F56C6C";
                    tipType = "DECREASE";
                    break;
                case "ASSET":
                    tipText = "该笔资产购置当期不影响利润，后续折旧分期扣减";
                    tipColor = "#909399";
                    tipType = "NEUTRAL";
                    break;
                case "LIABILITY":
                    tipText = "借款本金不影响利润，利息持续扣减";
                    tipColor = "#909399";
                    tipType = "NEUTRAL";
                    break;
                default:
                    tipText = "该笔业务将影响当期损益";
                    tipColor = "#333333";
                    tipType = "NEUTRAL";
            }
        }

        return new ProfitTipResult(tipText, tipColor, tipType, forcePopup);
    }

    /**
     * 根据科目编码判断科目类型
     */
    private String getSubjectType(String budgetSubjectCode) {
        if (StrUtil.isBlank(budgetSubjectCode)) {
            return "EXPENSE";
        }
        // 简化实现：根据科目编码前缀判断
        // 1xxx = 资产, 2xxx = 负债, 3xxx = 费用/成本, 4xxx = 收入
        if (budgetSubjectCode.startsWith("1")) {
            return "ASSET";
        } else if (budgetSubjectCode.startsWith("2")) {
            return "LIABILITY";
        } else if (budgetSubjectCode.startsWith("4") || budgetSubjectCode.startsWith("5")) {
            return "INCOME";
        } else {
            return "EXPENSE";
        }
    }

    /**
     * 获取盈利提示配置（行业优先，回退通用）
     */
    private FeeProfitTipConfig getConfig(String subjectType, String industryCode) {
        // 先查行业专属
        if (StrUtil.isNotBlank(industryCode) && !"*".equals(industryCode)) {
            FeeProfitTipConfig config = profitTipConfigMapper.selectOne(
                Wrappers.<FeeProfitTipConfig>lambdaQuery()
                    .eq(FeeProfitTipConfig::getSubjectType, subjectType)
                    .eq(FeeProfitTipConfig::getIndustryCode, industryCode)
                    .eq(FeeProfitTipConfig::getStatus, "ENABLED")
            );
            if (config != null) {
                return config;
            }
        }
        // 回退通用配置
        return profitTipConfigMapper.selectOne(
            Wrappers.<FeeProfitTipConfig>lambdaQuery()
                .eq(FeeProfitTipConfig::getSubjectType, subjectType)
                .eq(FeeProfitTipConfig::getIndustryCode, "*")
                .eq(FeeProfitTipConfig::getStatus, "ENABLED")
        );
    }
}
