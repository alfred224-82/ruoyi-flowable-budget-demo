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
import com.ruoyi.system.domain.bo.MappingRuleBo;
import com.ruoyi.system.domain.vo.BudgetPreviewVo;
import com.ruoyi.system.domain.vo.MappingRuleVo;
import com.ruoyi.system.service.IMappingRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * 映射规则管理Controller
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/mappingRule")
public class MappingRuleController extends BaseController {

    private final IMappingRuleService mappingRuleService;

    /**
     * 查询映射规则列表
     */
    @SaCheckPermission("fee:mappingRule:list")
    @GetMapping("/list")
    public TableDataInfo<MappingRuleVo> list(MappingRuleBo bo, PageQuery pageQuery) {
        Page<MappingRuleVo> page = mappingRuleService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 获取映射规则详细信息
     */
    @SaCheckPermission("fee:mappingRule:query")
    @GetMapping("/{id}")
    public R<MappingRuleVo> getInfo(@PathVariable Long id) {
        return R.ok(mappingRuleService.queryById(id));
    }

    /**
     * 新增映射规则
     */
    @SaCheckPermission("fee:mappingRule:add")
    @Log(title = "映射规则", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody MappingRuleBo bo) {
        return toAjax(mappingRuleService.insertByBo(bo));
    }

    /**
     * 修改映射规则
     */
    @SaCheckPermission("fee:mappingRule:edit")
    @Log(title = "映射规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody MappingRuleBo bo) {
        return toAjax(mappingRuleService.updateByBo(bo));
    }

    /**
     * 删除映射规则
     */
    @SaCheckPermission("fee:mappingRule:remove")
    @Log(title = "映射规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(mappingRuleService.deleteWithValidByIds(Arrays.asList(ids)));
    }

    /**
     * 预览预算归属（填写报销单用，无需权限）
     */
    @PostMapping("/preview")
    public R<BudgetPreviewVo> preview(@RequestBody Map<String, Object> params) {
        String itemCategory = (String) params.get("itemCategory");
        String purposeTag = (String) params.get("purposeTag");
        Object deptIdObj = params.get("deptId");
        Long deptId = deptIdObj != null ? Long.valueOf(deptIdObj.toString()) : null;
        String budgetPeriod = (String) params.get("budgetPeriod");
        return R.ok(mappingRuleService.previewBudgetSubject(itemCategory, purposeTag, deptId, budgetPeriod));
    }
}
