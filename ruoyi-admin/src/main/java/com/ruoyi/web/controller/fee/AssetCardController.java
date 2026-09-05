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
import com.ruoyi.system.domain.FeeAssetCard;
import com.ruoyi.system.mapper.FeeAssetCardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 资产卡片Controller
 * 设计文档 Phase 6 Task 6.6
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/asset")
public class AssetCardController extends BaseController {

    private final FeeAssetCardMapper assetCardMapper;

    /**
     * 资产卡片列表
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:asset:list")
    @GetMapping("/list")
    public TableDataInfo<FeeAssetCard> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String assetType,
            PageQuery pageQuery) {
        Page<FeeAssetCard> page = assetCardMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeAssetCard>lambdaQuery()
                .eq(status != null, FeeAssetCard::getStatus, status)
                .eq(deptId != null, FeeAssetCard::getDeptId, deptId)
                .eq(assetType != null, FeeAssetCard::getAssetType, assetType)
                .orderByDesc(FeeAssetCard::getCreateTime)
        );
        return TableDataInfo.build(page);
    }

    /**
     * 资产卡片详情
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:asset:query")
    @GetMapping("/{id}")
    public R<FeeAssetCard> getInfo(@PathVariable Long id) {
        return R.ok(assetCardMapper.selectById(id));
    }

    /**
     * 新增资产卡片
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:asset:add")
    @Log(title = "资产卡片", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody FeeAssetCard asset) {
        // 自动计算月折旧额 = (原值 - 原值×残值率) / 折旧月数
        if (asset.getMonthlyAmount() == null && asset.getOriginalValue() != null && asset.getUsefulLifeMonths() != null) {
            java.math.BigDecimal residual = asset.getOriginalValue()
                .multiply(asset.getResidualRate() != null ? asset.getResidualRate() : new java.math.BigDecimal("5"))
                .divide(new java.math.BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal depreciable = asset.getOriginalValue().subtract(residual);
            asset.setMonthlyAmount(depreciable.divide(new java.math.BigDecimal(asset.getUsefulLifeMonths()), 2, java.math.RoundingMode.HALF_UP));
        }
        if (asset.getAccumulatedAmount() == null) {
            asset.setAccumulatedAmount(java.math.BigDecimal.ZERO);
        }
        if (asset.getStatus() == null) {
            asset.setStatus("ACTIVE");
        }
        return toAjax(assetCardMapper.insert(asset) > 0);
    }

    /**
     * 修改资产卡片
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:asset:edit")
    @Log(title = "资产卡片", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody FeeAssetCard asset) {
        return toAjax(assetCardMapper.updateById(asset) > 0);
    }

    /**
     * 资产处置
     */
    @RequireFeature("FEE_ACCRUAL_ENGINE")
    @SaCheckPermission("fee:asset:edit")
    @Log(title = "资产处置", businessType = BusinessType.UPDATE)
    @PostMapping("/dispose/{id}")
    public R<Void> dispose(@PathVariable Long id) {
        FeeAssetCard asset = assetCardMapper.selectById(id);
        if (asset == null) {
            return R.fail("资产不存在");
        }
        asset.setStatus("DISPOSED");
        return toAjax(assetCardMapper.updateById(asset) > 0);
    }
}
