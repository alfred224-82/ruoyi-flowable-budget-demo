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
import com.ruoyi.system.domain.FeeProfitTipConfig;
import com.ruoyi.system.mapper.FeeProfitTipConfigMapper;
import com.ruoyi.system.service.IProfitTipService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 盈利提示Controller
 * 设计文档 Phase 8 Task 8.3
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/profitTip")
public class ProfitTipController extends BaseController {

    private final IProfitTipService profitTipService;
    private final FeeProfitTipConfigMapper profitTipConfigMapper;

    /**
     * 获取盈利提示（前端报销页面调用）
     */
    @GetMapping("/get")
    public R<IProfitTipService.ProfitTipResult> getTip(
            @RequestParam String subjectCode,
            @RequestParam(required = false, defaultValue = "*") String industryCode,
            @RequestParam(required = false, defaultValue = "0") BigDecimal amount) {
        return R.ok(profitTipService.getTip(subjectCode, industryCode, amount));
    }

    /**
     * 盈利提示配置列表
     */
    @SaCheckPermission("fee:profitTip:list")
    @GetMapping("/config/list")
    public TableDataInfo<FeeProfitTipConfig> configList(
            @RequestParam(required = false) String industryCode, PageQuery pageQuery) {
        Page<FeeProfitTipConfig> page = profitTipConfigMapper.selectPage(
            pageQuery.build(),
            Wrappers.<FeeProfitTipConfig>lambdaQuery()
                .eq(industryCode != null, FeeProfitTipConfig::getIndustryCode, industryCode)
                .orderByAsc(FeeProfitTipConfig::getSubjectType)
        );
        return TableDataInfo.build(page);
    }

    /**
     * 保存盈利提示配置
     */
    @RequireFeature("FEE_PROFIT_CUSTOM_TIP")
    @SaCheckPermission("fee:profitTip:edit")
    @Log(title = "盈利提示配置", businessType = BusinessType.UPDATE)
    @PostMapping("/config")
    public R<Void> saveConfig(@RequestBody FeeProfitTipConfig config) {
        if (config.getId() != null) {
            return toAjax(profitTipConfigMapper.updateById(config) > 0);
        }
        return toAjax(profitTipConfigMapper.insert(config) > 0);
    }

    /**
     * 删除盈利提示配置
     */
    @RequireFeature("FEE_PROFIT_CUSTOM_TIP")
    @SaCheckPermission("fee:profitTip:remove")
    @Log(title = "盈利提示配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/config/{id}")
    public R<Void> removeConfig(@PathVariable Long id) {
        return toAjax(profitTipConfigMapper.deleteById(id) > 0);
    }
}
