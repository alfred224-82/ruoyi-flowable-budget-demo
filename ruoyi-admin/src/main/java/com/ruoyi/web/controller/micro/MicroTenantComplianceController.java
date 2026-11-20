package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.bo.SensitiveWordLibraryBo;
import com.ruoyi.system.domain.bo.VoiceComplianceRuleBo;
import com.ruoyi.system.domain.bo.VoiceRecordingPolicyBo;
import com.ruoyi.system.domain.vo.SensitiveWordLibraryVo;
import com.ruoyi.system.domain.vo.VoiceComplianceRuleVo;
import com.ruoyi.system.domain.vo.VoiceRecordingPolicyVo;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.ISensitiveWordService;
import com.ruoyi.system.service.IVoiceComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理员语音合规配置Controller（T18.3，小微版 M18）
 * <p>「语音与合规配置」菜单组（micro:compliance:config:*）：
 * <ul>
 *   <li>语音开关（按 Activity 类型，继承行业默认）</li>
 *   <li>敏感词管理（继承平台底库+自定义）</li>
 *   <li>录音留存设置（≤ Admin 上限）</li>
 *   <li>转写留痕开关</li>
 * </ul>
 * <p>★租户管理员（platform_admin）专属。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/compliance/config")
public class MicroTenantComplianceController extends BaseController {

    private final IVoiceComplianceService complianceService;
    private final ISensitiveWordService sensitiveWordService;
    private final SysTenantMapper tenantMapper;

    // ===== 合规规则 CRUD =====

    /**
     * 合规规则列表（分页）
     */
    @SaCheckPermission("micro:compliance:config:rule")
    @GetMapping("/rule/list")
    public TableDataInfo<VoiceComplianceRuleVo> ruleList(VoiceComplianceRuleBo bo, PageQuery pageQuery) {
        return complianceService.queryRulePage(bo, pageQuery);
    }

    /**
     * 合规规则详情
     */
    @SaCheckPermission("micro:compliance:config:rule")
    @GetMapping("/rule/{id}")
    public R<VoiceComplianceRuleVo> ruleDetail(@PathVariable Long id) {
        return R.ok(complianceService.queryRuleById(id));
    }

    /**
     * 新增合规规则（★activityType 由后端从 sys_tenant.industry 自动填充，前端不传）
     */
    @SaCheckPermission("micro:compliance:config:rule")
    @Log(title = "合规规则", businessType = BusinessType.INSERT)
    @PostMapping("/rule")
    public R<Long> addRule(@Validated(AddGroup.class) @RequestBody VoiceComplianceRuleBo bo) {
        // 活动类型 = 当前租户行业编码（board_game / fitness / fresh / spa ...）
        Long tenantId = LoginHelper.getTenantId();
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant != null && tenant.getIndustry() != null) {
            bo.setActivityType(tenant.getIndustry());
        }
        return R.ok(complianceService.insertRule(bo));
    }

    /**
     * 修改合规规则
     */
    @SaCheckPermission("micro:compliance:config:rule")
    @Log(title = "合规规则", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public R<Void> editRule(@Validated(EditGroup.class) @RequestBody VoiceComplianceRuleBo bo) {
        return toAjax(complianceService.updateRule(bo));
    }

    /**
     * 删除合规规则
     */
    @SaCheckPermission("micro:compliance:config:rule")
    @Log(title = "合规规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/rule/{id}")
    public R<Void> removeRule(@PathVariable Long id) {
        return toAjax(complianceService.deleteRule(id));
    }

    // ===== 录音留存策略 =====

    /**
     * 查询当前租户录音策略
     */
    @SaCheckPermission("micro:compliance:config:policy")
    @GetMapping("/policy")
    public R<VoiceRecordingPolicyVo> getPolicy() {
        return R.ok(complianceService.queryPolicy());
    }

    /**
     * 保存/更新录音策略（≤ Admin 全局上限）
     */
    @SaCheckPermission("micro:compliance:config:policy")
    @Log(title = "录音策略", businessType = BusinessType.UPDATE)
    @PutMapping("/policy")
    public R<Long> savePolicy(@RequestBody VoiceRecordingPolicyBo bo) {
        return R.ok(complianceService.savePolicy(bo));
    }

    // ===== 租户自定义敏感词管理 =====

    /**
     * 敏感词列表（平台底库 + 本租户自定义，分页）
     */
    @SaCheckPermission("micro:compliance:config:sensitiveWord")
    @GetMapping("/sensitiveWord/list")
    public TableDataInfo<SensitiveWordLibraryVo> wordList(SensitiveWordLibraryBo bo, PageQuery pageQuery) {
        return sensitiveWordService.queryWordPage(bo, pageQuery, false);
    }

    /**
     * 新增自定义敏感词
     */
    @SaCheckPermission("micro:compliance:config:sensitiveWord")
    @Log(title = "自定义敏感词", businessType = BusinessType.INSERT)
    @PostMapping("/sensitiveWord")
    public R<Long> addWord(@Validated(AddGroup.class) @RequestBody SensitiveWordLibraryBo bo) {
        return R.ok(sensitiveWordService.insertWord(bo, false));
    }

    /**
     * 修改自定义敏感词
     */
    @SaCheckPermission("micro:compliance:config:sensitiveWord")
    @Log(title = "自定义敏感词", businessType = BusinessType.UPDATE)
    @PutMapping("/sensitiveWord")
    public R<Void> editWord(@Validated(EditGroup.class) @RequestBody SensitiveWordLibraryBo bo) {
        return toAjax(sensitiveWordService.updateWord(bo));
    }

    /**
     * 删除自定义敏感词（★系统内置不可删）
     */
    @SaCheckPermission("micro:compliance:config:sensitiveWord")
    @Log(title = "自定义敏感词", businessType = BusinessType.DELETE)
    @DeleteMapping("/sensitiveWord/{id}")
    public R<Void> removeWord(@PathVariable Long id) {
        return toAjax(sensitiveWordService.deleteWord(id));
    }

    /**
     * 查询有效敏感词列表（按策略合并平台底库+自定义）
     */
    @SaCheckPermission("micro:compliance:config:sensitiveWord")
    @GetMapping("/sensitiveWord/effective")
    public R<List<SensitiveWordLibraryVo>> effectiveWords(
        @RequestParam(required = false, defaultValue = "INHERIT") String strategy) {
        return R.ok(sensitiveWordService.getEffectiveWords(strategy));
    }
}
