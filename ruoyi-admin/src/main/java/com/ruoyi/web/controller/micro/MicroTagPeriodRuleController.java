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
import com.ruoyi.system.domain.bo.FixedCostTagBo;
import com.ruoyi.system.domain.bo.PeriodRuleBo;
import com.ruoyi.system.domain.vo.FixedCostTagVo;
import com.ruoyi.system.domain.vo.PeriodRuleVo;
import com.ruoyi.system.service.ITagPeriodRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签与周期规则管理Controller（T18.9，小微版 M18）
 * <p>「固定支出配置」菜单组（micro:tagConfig:* / micro:periodRule:*）：
 * <ul>
 *   <li>标签管理（房租/水电/工资等，CRUD + 排序）</li>
 *   <li>周期规则管理（数字+单位→保存，如“1 个月”“15 天”）</li>
 * </ul>
 * <p>★租户管理员（platform_admin）专属。
 * <p>★周期规则按标签查询端点（/rule/byTag）对 boss 开放，用于固定支出表单自动带出周期规则。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/tagPeriodRule")
public class MicroTagPeriodRuleController extends BaseController {

    private final ITagPeriodRuleService tagPeriodRuleService;

    // ===== 标签管理 =====

    @SaCheckPermission("micro:tagConfig:list")
    @GetMapping("/tag/list")
    public TableDataInfo<FixedCostTagVo> tagList(FixedCostTagBo bo, PageQuery pageQuery) {
        return tagPeriodRuleService.queryTagPage(bo, pageQuery);
    }

    /**
     * 获取全部启用标签（boss 固定支出表单下拉用）
     * <p>无需特定权限，登录即可访问（租户插件自动隔离数据）
     */
    @GetMapping("/tag/all")
    public R<List<FixedCostTagVo>> tagAll(FixedCostTagBo bo) {
        return R.ok(tagPeriodRuleService.queryTagAll(bo));
    }

    @SaCheckPermission("micro:tagConfig:list")
    @GetMapping("/tag/{id}")
    public R<FixedCostTagVo> tagDetail(@PathVariable Long id) {
        return R.ok(tagPeriodRuleService.queryTagById(id));
    }

    @SaCheckPermission("micro:tagConfig:add")
    @Log(title = "支出标签", businessType = BusinessType.INSERT)
    @PostMapping("/tag")
    public R<Long> addTag(@Validated(AddGroup.class) @RequestBody FixedCostTagBo bo) {
        return R.ok(tagPeriodRuleService.insertTag(bo));
    }

    @SaCheckPermission("micro:tagConfig:edit")
    @Log(title = "支出标签", businessType = BusinessType.UPDATE)
    @PutMapping("/tag")
    public R<Void> editTag(@Validated(EditGroup.class) @RequestBody FixedCostTagBo bo) {
        return toAjax(tagPeriodRuleService.updateTag(bo));
    }

    @SaCheckPermission("micro:tagConfig:edit")
    @Log(title = "支出标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/tag/{id}")
    public R<Void> removeTag(@PathVariable Long id) {
        return toAjax(tagPeriodRuleService.deleteTag(id));
    }

    // ===== 周期规则管理 =====

    @SaCheckPermission("micro:periodRule:list")
    @GetMapping("/rule/list")
    public TableDataInfo<PeriodRuleVo> ruleList(PeriodRuleBo bo, PageQuery pageQuery) {
        return tagPeriodRuleService.queryRulePage(bo, pageQuery);
    }

    @SaCheckPermission("micro:periodRule:list")
    @GetMapping("/rule/{id}")
    public R<PeriodRuleVo> ruleDetail(@PathVariable Long id) {
        return R.ok(tagPeriodRuleService.queryRuleById(id));
    }

    @SaCheckPermission("micro:periodRule:add")
    @Log(title = "周期规则", businessType = BusinessType.INSERT)
    @PostMapping("/rule")
    public R<Long> addRule(@Validated(AddGroup.class) @RequestBody PeriodRuleBo bo) {
        return R.ok(tagPeriodRuleService.insertRule(bo));
    }

    @SaCheckPermission("micro:periodRule:edit")
    @Log(title = "周期规则", businessType = BusinessType.UPDATE)
    @PutMapping("/rule")
    public R<Void> editRule(@Validated(EditGroup.class) @RequestBody PeriodRuleBo bo) {
        return toAjax(tagPeriodRuleService.updateRule(bo));
    }

    @SaCheckPermission("micro:periodRule:edit")
    @Log(title = "周期规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/rule/{id}")
    public R<Void> removeRule(@PathVariable Long id) {
        return toAjax(tagPeriodRuleService.deleteRule(id));
    }

    // ===== boss 端：按标签查询周期规则（固定支出表单用） =====

    /**
     * 根据标签ID查询启用的周期规则列表（boss 固定支出表单自动带出）
     * <p>无需特定权限，登录即可访问（租户插件自动隔离）
     */
    @GetMapping("/rule/byTag/{tagId}")
    public R<List<PeriodRuleVo>> rulesByTag(@PathVariable Long tagId) {
        PeriodRuleBo bo = new PeriodRuleBo();
        bo.setTagId(tagId);
        bo.setStatus("ENABLED");
        return R.ok(tagPeriodRuleService.queryRuleAll(bo));
    }
}
