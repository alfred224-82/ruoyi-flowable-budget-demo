package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.core.service.FeatureChecker;
import com.ruoyi.system.domain.FeeExchangeRate;
import com.ruoyi.system.mapper.FeeExchangeRateMapper;
import com.ruoyi.system.service.IExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 汇率管理Controller
 * 设计文档 Phase 10 Task 10.4
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/exchangeRate")
public class ExchangeRateController extends BaseController {

    private final IExchangeRateService exchangeRateService;
    private final FeeExchangeRateMapper exchangeRateMapper;
    private final FeatureChecker featureChecker;

    /**
     * 汇率列表
     */
    @RequireFeature("FEE_MULTI_CURRENCY")
    @SaCheckPermission("fee:exchangeRate:list")
    @GetMapping("/list")
    public TableDataInfo<FeeExchangeRate> list(
            @RequestParam(required = false) String currencyCode,
            @RequestParam(required = false) String ratePeriod,
            PageQuery pageQuery) {
        Page<FeeExchangeRate> page = exchangeRateMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeExchangeRate>lambdaQuery()
                .eq(currencyCode != null, FeeExchangeRate::getCurrencyCode, currencyCode)
                .eq(ratePeriod != null, FeeExchangeRate::getRatePeriod, ratePeriod)
                .orderByDesc(FeeExchangeRate::getRatePeriod)
        );
        return TableDataInfo.build(page);
    }

    /**
     * 保存汇率
     */
    @RequireFeature("FEE_MULTI_CURRENCY")
    @SaCheckPermission("fee:exchangeRate:edit")
    @Log(title = "汇率管理", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody FeeExchangeRate rate) {
        // 检查是否已存在同期同币种
        FeeExchangeRate existing = exchangeRateMapper.selectOne(
            Wrappers.<FeeExchangeRate>lambdaQuery()
                .eq(FeeExchangeRate::getCurrencyCode, rate.getCurrencyCode())
                .eq(FeeExchangeRate::getRatePeriod, rate.getRatePeriod())
        );
        if (existing != null) {
            rate.setId(existing.getId());
            return toAjax(exchangeRateMapper.updateById(rate) > 0);
        }
        return toAjax(exchangeRateMapper.insert(rate) > 0);
    }

    /**
     * 折算人民币
     */
    @RequireFeature("FEE_MULTI_CURRENCY")
    @GetMapping("/convert")
    public R<BigDecimal> convert(
            @RequestParam String currencyCode,
            @RequestParam BigDecimal originalAmount,
            @RequestParam String period) {
        return R.ok(exchangeRateService.convertToCny(currencyCode, originalAmount, period));
    }

    /**
     * 获取指定月份汇率
     */
    @RequireFeature("FEE_MULTI_CURRENCY")
    @GetMapping("/rate")
    public R<BigDecimal> getRate(
            @RequestParam String currencyCode,
            @RequestParam String period) {
        return R.ok(exchangeRateService.getRate(currencyCode, period));
    }

    /**
     * 获取可用币种列表（按版本区分）
     * - 基础版（FEE_MULTI_CURRENCY未开启）：仅返回CNY
     * - 高级版/企业版（FEE_MULTI_CURRENCY已开启）：返回CNY + 已配置外币
     */
    @GetMapping("/currencies")
    public R<List<Map<String, String>>> listCurrencies() {
        List<Map<String, String>> result = new ArrayList<>();
        // 始终包含人民币
        Map<String, String> cny = new HashMap<>();
        cny.put("code", "CNY");
        cny.put("name", "人民币");
        result.add(cny);

        // 基础版：FEE_MULTI_CURRENCY未开启，仅返回CNY
        if (!featureChecker.isFeatureEnabled("FEE_MULTI_CURRENCY")) {
            return R.ok(result);
        }

        // 高级版/企业版：追加已配置的外币币种
        List<FeeExchangeRate> all = exchangeRateMapper.selectList(
            Wrappers.<FeeExchangeRate>lambdaQuery()
                .select(FeeExchangeRate::getCurrencyCode, FeeExchangeRate::getCurrencyName)
                .groupBy(FeeExchangeRate::getCurrencyCode, FeeExchangeRate::getCurrencyName)
        );
        for (FeeExchangeRate r : all) {
            if (!"CNY".equalsIgnoreCase(r.getCurrencyCode())) {
                Map<String, String> item = new HashMap<>();
                item.put("code", r.getCurrencyCode());
                item.put("name", r.getCurrencyName() != null ? r.getCurrencyName() : r.getCurrencyCode());
                result.add(item);
            }
        }
        return R.ok(result);
    }

    /**
     * 删除汇率记录
     */
    @RequireFeature("FEE_MULTI_CURRENCY")
    @SaCheckPermission("fee:exchangeRate:remove")
    @Log(title = "汇率管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(exchangeRateMapper.deleteById(id) > 0);
    }
}
