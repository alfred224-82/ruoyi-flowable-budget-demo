package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeExchangeRate;
import com.ruoyi.system.mapper.FeeExchangeRateMapper;
import com.ruoyi.system.service.IExchangeRateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 汇率管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateServiceImpl implements IExchangeRateService {

    private final FeeExchangeRateMapper exchangeRateMapper;

    @Override
    public BigDecimal getRate(String currencyCode, String period) {
        // CNY 直接返回 1
        if ("CNY".equalsIgnoreCase(currencyCode)) {
            return BigDecimal.ONE;
        }
        FeeExchangeRate rate = exchangeRateMapper.selectOne(
            Wrappers.<FeeExchangeRate>lambdaQuery()
                .eq(FeeExchangeRate::getCurrencyCode, currencyCode)
                .eq(FeeExchangeRate::getRatePeriod, period)
        );
        return rate != null ? rate.getRateToCny() : null;
    }

    @Override
    public BigDecimal convertToCny(String currencyCode, BigDecimal originalAmount, String period) {
        if ("CNY".equalsIgnoreCase(currencyCode)) {
            return originalAmount;
        }
        BigDecimal rate = getRate(currencyCode, period);
        if (rate == null) {
            throw new ServiceException("未找到币种[" + currencyCode + "]在期间[" + period + "]的汇率配置，请先维护汇率");
        }
        return originalAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
