package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeElasticQuotaConfig;
import com.ruoyi.system.service.IElasticQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 弹性额度配置Controller（T6.1 / FEAT_ELASTIC_QUOTA，高级版增量）
 * 部门大类超支阈值内预警放行（配置挂在部门预算编制页 3217）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/elasticQuota")
public class ElasticQuotaController extends BaseController {

    private final IElasticQuotaService elasticQuotaService;

    /**
     * 弹性额度配置列表
     */
    @RequireFeature("FEAT_ELASTIC_QUOTA")
    @SaCheckPermission("fee:deptBudget:query")
    @GetMapping("/list")
    public R<List<FeeElasticQuotaConfig>> list() {
        return R.ok(elasticQuotaService.listConfigs());
    }

    /**
     * 新增弹性额度配置
     */
    @RequireFeature("FEAT_ELASTIC_QUOTA")
    @SaCheckPermission("fee:deptBudget:edit")
    @Log(title = "弹性额度配置", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody FeeElasticQuotaConfig config) {
        return toAjax(elasticQuotaService.saveConfig(config));
    }

    /**
     * 修改弹性额度配置
     */
    @RequireFeature("FEAT_ELASTIC_QUOTA")
    @SaCheckPermission("fee:deptBudget:edit")
    @Log(title = "弹性额度配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody FeeElasticQuotaConfig config) {
        return toAjax(elasticQuotaService.updateConfig(config));
    }

    /**
     * 删除弹性额度配置
     */
    @RequireFeature("FEAT_ELASTIC_QUOTA")
    @SaCheckPermission("fee:deptBudget:edit")
    @Log(title = "弹性额度配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(elasticQuotaService.deleteConfig(id));
    }
}
