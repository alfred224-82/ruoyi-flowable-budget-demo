package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeIndustryConfig;
import com.ruoyi.system.service.IIndustryConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行业配置Controller
 * 设计文档 Phase 4 Task 4.5
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/industry")
public class IndustryConfigController extends BaseController {

    private final IIndustryConfigService industryConfigService;

    /**
     * 行业配置列表
     */
    @SaCheckPermission("fee:industry:list")
    @GetMapping("/list")
    public R<List<FeeIndustryConfig>> list() {
        return R.ok(industryConfigService.listAll());
    }

    /**
     * 根据行业编码获取详情
     */
    @SaCheckPermission("fee:industry:query")
    @GetMapping("/{code}")
    public R<FeeIndustryConfig> getInfo(@PathVariable String code) {
        return R.ok(industryConfigService.queryByCode(code));
    }

    /**
     * 新增/编辑行业配置
     */
    @RequireFeature("FEE_INDUSTRY_SINGLE")
    @SaCheckPermission("fee:industry:edit")
    @Log(title = "行业配置", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody FeeIndustryConfig config) {
        return toAjax(industryConfigService.save(config));
    }

    /**
     * 删除行业配置
     */
    @RequireFeature("FEE_INDUSTRY_SINGLE")
    @SaCheckPermission("fee:industry:remove")
    @Log(title = "行业配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(industryConfigService.deleteById(id));
    }

    /**
     * 获取当前用户行业编码
     * 注意：必须使用 ok(msg, data) 双参版本，避免 R.ok(String) 重载将内容放入msg而非data
     */
    @GetMapping("/current")
    public R<String> currentIndustry() {
        return R.ok("操作成功", industryConfigService.getCurrentUserIndustry());
    }
}
