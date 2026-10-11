package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeRulePublishBo;
import com.ruoyi.system.domain.vo.FeeRuleGrayscaleCompareVo;
import com.ruoyi.system.domain.vo.FeeRuleVersionVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import com.ruoyi.system.service.IFeeRuleVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则版本快照Controller（T7.4 / FEAT_RULE_GRAYSCALE）
 * 灰度发布（默认10%比对命中率）+ 版本快照（锁定预算用当时快照）+ AI语义推荐
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/ruleVersion")
@RequireFeature("FEAT_RULE_ENGINE_BASE")
public class FeeRuleVersionController extends BaseController {

    private final IFeeRuleVersionService ruleVersionService;

    /**
     * 规则版本列表（版本号倒序）
     */
    @SaCheckPermission("fee:mappingRule:list")
    @GetMapping("/list")
    public R<List<FeeRuleVersionVo>> list() {
        return R.ok(ruleVersionService.listVersions());
    }

    /**
     * 发布当前映射规则为新版本（快照），按灰度百分比生效
     */
    @SaCheckPermission("fee:mappingRule:publish")
    @Log(title = "规则版本发布", businessType = BusinessType.INSERT)
    @PostMapping("/publish")
    public R<Long> publish(@Validated @RequestBody FeeRulePublishBo bo) {
        return R.ok(ruleVersionService.publish(bo));
    }

    /**
     * 灰度命中率比对（新版本规则对灰度分组单据的命中率 vs 上一版本）
     */
    @SaCheckPermission("fee:mappingRule:query")
    @GetMapping("/compare/{id}")
    public R<FeeRuleGrayscaleCompareVo> compare(@PathVariable Long id) {
        return R.ok(ruleVersionService.compareHitRate(id));
    }

    /**
     * 全量发布（灰度验证通过后）
     */
    @SaCheckPermission("fee:mappingRule:publish")
    @Log(title = "规则版本发布", businessType = BusinessType.UPDATE)
    @PutMapping("/fullRelease/{id}")
    public R<Void> fullRelease(@PathVariable Long id) {
        return toAjax(ruleVersionService.fullRelease(id));
    }

    /**
     * 回滚（标记该版本回滚，不删除历史）
     */
    @SaCheckPermission("fee:mappingRule:publish")
    @Log(title = "规则版本发布", businessType = BusinessType.UPDATE)
    @PutMapping("/rollback/{id}")
    public R<Void> rollback(@PathVariable Long id) {
        return toAjax(ruleVersionService.rollback(id));
    }

    /**
     * AI 语义推荐科目（未命中关键字时由语义模型推荐候选科目）
     *
     * @param text 报销摘要/用途描述
     */
    @SaCheckPermission("fee:mappingRule:query")
    @GetMapping("/recommend")
    public R<List<MappingRuleVo>> recommend(@RequestParam String text) {
        return R.ok(ruleVersionService.recommendSubject(text));
    }
}
