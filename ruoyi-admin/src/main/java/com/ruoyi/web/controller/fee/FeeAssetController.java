package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeAsset;
import com.ruoyi.system.domain.FeeDepreciationRule;
import com.ruoyi.system.domain.vo.FeeAssetDepreciationVo;
import com.ruoyi.system.domain.vo.FeeAssetVo;
import com.ruoyi.system.domain.vo.FeeDepreciationRuleVo;
import com.ruoyi.system.service.IFeeAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产折旧Controller（T5.9）
 * 资产维护（月折旧自动算）+ 折旧规则 + 月度计提
 * 注：路径 /fee/assetDepr 区别于旧版资产卡片 AssetCardController（/fee/asset）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/assetDepr")
@RequireFeature("FEAT_HIDDEN_COST")
public class FeeAssetController extends BaseController {

    private final IFeeAssetService assetService;

    /**
     * 资产列表
     */
    @SaCheckPermission("fee:asset:list")
    @GetMapping("/list")
    public TableDataInfo<FeeAssetVo> list(
        @RequestParam(required = false) Long projectId,
        @RequestParam(required = false) String status,
        PageQuery pageQuery) {
        return assetService.queryPageList(projectId, status, pageQuery);
    }

    /**
     * 新增/修改资产（月折旧额自动计算）
     */
    @SaCheckPermission("fee:asset:edit")
    @Log(title = "资产维护", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Long> save(@RequestBody FeeAsset asset) {
        return R.ok(assetService.saveAsset(asset));
    }

    /**
     * 资产报废（停止折旧）
     */
    @SaCheckPermission("fee:asset:edit")
    @Log(title = "资产报废", businessType = BusinessType.UPDATE)
    @PutMapping("/scrap/{id}")
    public R<Void> scrap(@PathVariable Long id) {
        return toAjax(assetService.scrapAsset(id));
    }

    /**
     * 折旧规则列表
     */
    @SaCheckPermission("fee:depreciationRule:list")
    @GetMapping("/rule/list")
    public R<List<FeeDepreciationRuleVo>> ruleList() {
        return R.ok(assetService.listRules());
    }

    /**
     * 保存折旧规则（资产分类唯一）
     */
    @SaCheckPermission("fee:depreciationRule:edit")
    @Log(title = "折旧规则", businessType = BusinessType.UPDATE)
    @PostMapping("/rule")
    public R<Long> saveRule(@RequestBody FeeDepreciationRule rule) {
        return R.ok(assetService.saveRule(rule));
    }

    /**
     * 折旧记录列表
     */
    @SaCheckPermission("fee:asset:query")
    @GetMapping("/depreciation/list")
    public TableDataInfo<FeeAssetDepreciationVo> depreciationList(
        @RequestParam(required = false) String accrualMonth,
        @RequestParam(required = false) Long projectId,
        PageQuery pageQuery) {
        return assetService.queryDepreciationPageList(accrualMonth, projectId, pageQuery);
    }

    /**
     * 手动触发月度折旧计提（幂等）
     */
    @SaCheckPermission("fee:asset:edit")
    @Log(title = "折旧计提", businessType = BusinessType.UPDATE)
    @PutMapping("/accrue")
    public R<Integer> accrue(@RequestParam(required = false) String accrualMonth) {
        return R.ok(assetService.accrueDepreciation(accrualMonth));
    }
}
