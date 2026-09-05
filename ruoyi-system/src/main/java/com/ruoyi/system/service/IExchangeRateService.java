package com.ruoyi.system.service;

import java.math.BigDecimal;

/**
 * 汇率管理Service接口
 *
 * @author ruoyi
 * @date 2026-07-23
 */
public interface IExchangeRateService {

    /**
     * 获取指定月份汇率
     *
     * @param currencyCode 币种编码
     * @param period       汇率期间（YYYY-MM）
     * @return 兑人民币汇率，未配置返回null
     */
    BigDecimal getRate(String currencyCode, String period);

    /**
     * 折算人民币
     *
     * @param currencyCode   币种编码
     * @param originalAmount 原币金额
     * @param period         汇率期间（YYYY-MM）
     * @return 人民币金额
     */
    BigDecimal convertToCny(String currencyCode, BigDecimal originalAmount, String period);
}
