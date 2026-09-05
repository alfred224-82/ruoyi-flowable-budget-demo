package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeAiPromptConfigBo;
import com.ruoyi.system.domain.vo.FeeAiPromptConfigVo;
import com.ruoyi.system.service.IAiPromptConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * AI引导话术配置Controller
 * 设计文档 Phase 10 Task 10.4
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/aiPrompt")
public class AiPromptConfigController extends BaseController {

    private final IAiPromptConfigService aiPromptConfigService;

    /**
     * 按场景+行业获取话术模板（前端AI引导调用）
     */
    @GetMapping("/get")
    public R<FeeAiPromptConfigVo> getPrompt(
            @RequestParam String sceneCode,
            @RequestParam(required = false, defaultValue = "*") String industryCode) {
        return R.ok(aiPromptConfigService.getPrompt(sceneCode, industryCode));
    }

    /**
     * 查询话术配置列表
     */
    @SaCheckPermission("fee:aiPrompt:list")
    @GetMapping("/list")
    public TableDataInfo<FeeAiPromptConfigVo> list(FeeAiPromptConfigBo bo, PageQuery pageQuery) {
        Page<FeeAiPromptConfigVo> page = aiPromptConfigService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 查询全部启用的话术配置（下拉选择用）
     */
    @GetMapping("/listEnabled")
    public R<List<FeeAiPromptConfigVo>> listEnabled() {
        return R.ok(aiPromptConfigService.listEnabled());
    }

    /**
     * 获取话术配置详细信息
     */
    @SaCheckPermission("fee:aiPrompt:query")
    @GetMapping("/{id}")
    public R<FeeAiPromptConfigVo> getInfo(@PathVariable Long id) {
        return R.ok(aiPromptConfigService.queryById(id));
    }

    /**
     * 新增话术配置
     */
    @SaCheckPermission("fee:aiPrompt:add")
    @Log(title = "AI话术配置", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody FeeAiPromptConfigBo bo) {
        return toAjax(aiPromptConfigService.insertByBo(bo));
    }

    /**
     * 修改话术配置
     */
    @SaCheckPermission("fee:aiPrompt:edit")
    @Log(title = "AI话术配置", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody FeeAiPromptConfigBo bo) {
        return toAjax(aiPromptConfigService.updateByBo(bo));
    }

    /**
     * 删除话术配置
     */
    @SaCheckPermission("fee:aiPrompt:remove")
    @Log(title = "AI话术配置", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(aiPromptConfigService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
