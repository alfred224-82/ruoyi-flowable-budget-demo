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
import com.ruoyi.system.domain.bo.SensitiveWordLibraryBo;
import com.ruoyi.system.domain.vo.SensitiveWordLibraryVo;
import com.ruoyi.system.service.ISensitiveWordService;
import com.ruoyi.system.service.IVoiceComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin 平台合规管理Controller（T18.2，小微版 M18）
 * <p>「平台合规」菜单组（system:compliance:*）：
 * <ul>
 *   <li>语音合规总开关（全局 enable/disable）</li>
 *   <li>录音留存策略（全局上限天数）</li>
 *   <li>平台敏感词库 CRUD + 批量导入</li>
 *   <li>合规告警看板（近 7 天命中趋势）</li>
 * </ul>
 * <p>★Admin 专属，非租户管理员。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/compliance")
public class MicroAdminComplianceController extends BaseController {

    private final IVoiceComplianceService complianceService;
    private final ISensitiveWordService sensitiveWordService;

    // ===== 语音合规总开关 / 录音留存策略 =====

    /**
     * ★查询全局录音留存上限天数
     */
    @SaCheckPermission("system:compliance:config")
    @GetMapping("/globalRetention")
    public R<Integer> getGlobalRetention() {
        return R.ok(complianceService.getGlobalMaxRetentionDays());
    }

    /**
     * ★设置全局录音留存上限天数
     */
    @SaCheckPermission("system:compliance:config")
    @Log(title = "合规配置", businessType = BusinessType.UPDATE)
    @PutMapping("/globalRetention")
    public R<Void> setGlobalRetention(@RequestParam int days) {
        complianceService.setGlobalMaxRetentionDays(days);
        return R.ok();
    }

    // ===== 合规告警看板 =====

    /**
     * ★合规告警看板（近 7 天命中趋势 + 按类型统计 + 待处理数）
     */
    @SaCheckPermission("system:compliance:alert")
    @GetMapping("/alertDashboard")
    public R<List<Map<String, Object>>> alertDashboard() {
        return R.ok(complianceService.complianceAlertDashboard());
    }

    // ===== 平台敏感词库 CRUD =====

    /**
     * 平台敏感词列表（分页，tenant_id IS NULL）
     */
    @SaCheckPermission("system:compliance:sensitiveWord")
    @GetMapping("/sensitiveWord/list")
    public TableDataInfo<SensitiveWordLibraryVo> wordList(SensitiveWordLibraryBo bo, PageQuery pageQuery) {
        return sensitiveWordService.queryWordPage(bo, pageQuery, true);
    }

    /**
     * 平台敏感词详情
     */
    @SaCheckPermission("system:compliance:sensitiveWord")
    @GetMapping("/sensitiveWord/{id}")
    public R<SensitiveWordLibraryVo> wordDetail(@PathVariable Long id) {
        return R.ok(sensitiveWordService.queryWordById(id));
    }

    /**
     * 新增平台敏感词
     */
    @SaCheckPermission("system:compliance:sensitiveWord")
    @Log(title = "平台敏感词", businessType = BusinessType.INSERT)
    @PostMapping("/sensitiveWord")
    public R<Long> addWord(@Validated(AddGroup.class) @RequestBody SensitiveWordLibraryBo bo) {
        return R.ok(sensitiveWordService.insertWord(bo, true));
    }

    /**
     * 修改平台敏感词
     */
    @SaCheckPermission("system:compliance:sensitiveWord")
    @Log(title = "平台敏感词", businessType = BusinessType.UPDATE)
    @PutMapping("/sensitiveWord")
    public R<Void> editWord(@Validated(EditGroup.class) @RequestBody SensitiveWordLibraryBo bo) {
        return toAjax(sensitiveWordService.updateWord(bo));
    }

    /**
     * 删除平台敏感词
     */
    @SaCheckPermission("system:compliance:sensitiveWord")
    @Log(title = "平台敏感词", businessType = BusinessType.DELETE)
    @DeleteMapping("/sensitiveWord/{id}")
    public R<Void> removeWord(@PathVariable Long id) {
        return toAjax(sensitiveWordService.deleteWord(id));
    }

    /**
     * ★批量导入平台敏感词
     */
    @SaCheckPermission("system:compliance:sensitiveWord")
    @Log(title = "平台敏感词导入", businessType = BusinessType.INSERT)
    @PostMapping("/sensitiveWord/import")
    public R<Integer> importWords(@RequestParam List<String> words,
                                   @RequestParam(required = false, defaultValue = "CUSTOM") String category,
                                   @RequestParam(required = false, defaultValue = "WARN") String level) {
        return R.ok(sensitiveWordService.importWords(words, category, level, true));
    }
}
