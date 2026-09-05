package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.FeeDesensitizeRule;
import com.ruoyi.system.service.IDesensitizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据脱敏规则Controller
 * 设计文档 Phase 10 Task 10.4
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/desensitize")
public class DesensitizeRuleController extends BaseController {

    private final IDesensitizeService desensitizeService;

    /**
     * 脱敏规则列表
     */
    @RequireFeature("FEE_DESENSITIZE_CUSTOM")
    @SaCheckPermission("fee:desensitize:list")
    @GetMapping("/list")
    public R<List<FeeDesensitizeRule>> list() {
        return R.ok(desensitizeService.listRules());
    }

    /**
     * 保存脱敏规则
     */
    @RequireFeature("FEE_DESENSITIZE_CUSTOM")
    @SaCheckPermission("fee:desensitize:edit")
    @Log(title = "脱敏规则", businessType = BusinessType.UPDATE)
    @PostMapping
    public R<Void> save(@RequestBody FeeDesensitizeRule rule) {
        return toAjax(desensitizeService.saveRule(rule));
    }

    /**
     * 删除脱敏规则
     */
    @RequireFeature("FEE_DESENSITIZE_CUSTOM")
    @SaCheckPermission("fee:desensitize:remove")
    @Log(title = "脱敏规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        return toAjax(desensitizeService.deleteRule(id));
    }

    /**
     * 测试脱敏效果
     */
    @RequireFeature("FEE_DESENSITIZE_CUSTOM")
    @GetMapping("/test")
    public R<String> testDesensitize(@RequestParam String text) {
        return R.ok(desensitizeService.desensitize(text));
    }

    /**
     * 敏感词检测
     */
    @RequireFeature("FEE_DESENSITIZE_CUSTOM")
    @GetMapping("/checkSensitive")
    public R<Boolean> checkSensitive(
            @RequestParam String text,
            @RequestParam(required = false, defaultValue = "*") String industryCode) {
        return R.ok(desensitizeService.containsSensitiveWord(text, industryCode));
    }
}
