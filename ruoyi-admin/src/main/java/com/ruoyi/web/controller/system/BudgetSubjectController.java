package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.*;
import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.vo.BudgetSubjectVo;
import com.ruoyi.system.domain.bo.BudgetSubjectBo;
import com.ruoyi.system.service.IBudgetSubjectService;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 预算科目
 *
 * @author ruoyi
 * @date 2026-05-30
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/subject")
public class BudgetSubjectController extends BaseController {

    private final IBudgetSubjectService iBudgetSubjectService;

    /**
     * 查询预算科目列表
     */
    @SaCheckPermission("system:subject:list")
    @GetMapping("/list")
    public TableDataInfo<BudgetSubjectVo> list(BudgetSubjectBo bo, PageQuery pageQuery) {
        return iBudgetSubjectService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出预算科目列表
     */
    @SaCheckPermission("system:subject:export")
    @Log(title = "预算科目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(BudgetSubjectBo bo, HttpServletResponse response) {
        List<BudgetSubjectVo> list = iBudgetSubjectService.queryList(bo);
        ExcelUtil.exportExcel(list, "预算科目", BudgetSubjectVo.class, response);
    }

    /**
     * 查询一级科目列表
     */
    @GetMapping("/firstLevel")
    public R<List<BudgetSubjectVo>> firstLevel() {
        return R.ok(iBudgetSubjectService.listFirstLevel());
    }

    /**
     * 根据父级ID查询二级科目列表
     */
    @GetMapping("/secondLevel/{parentId}")
    public R<List<BudgetSubjectVo>> secondLevel(@PathVariable Long parentId) {
        return R.ok(iBudgetSubjectService.listSecondLevel(parentId));
    }

    /**
     * 查询所有启用的科目（树形结构）
     */
    @GetMapping("/all")
    public R<List<BudgetSubjectVo>> all() {
        return R.ok(iBudgetSubjectService.listAllSubjects());
    }

    /**
     * 获取预算科目详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("system:subject:query")
    @GetMapping("/{id}")
    public R<BudgetSubjectVo> getInfo(@NotNull(message = "主键不能为空")
                                     @PathVariable Long id) {
        return R.ok(iBudgetSubjectService.queryById(id));
    }

    /**
     * 新增预算科目
     */
    @SaCheckPermission("system:subject:add")
    @Log(title = "预算科目", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody BudgetSubjectBo bo) {
        return toAjax(iBudgetSubjectService.insertByBo(bo));
    }

    /**
     * 修改预算科目
     */
    @SaCheckPermission("system:subject:edit")
    @Log(title = "预算科目", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody BudgetSubjectBo bo) {
        return toAjax(iBudgetSubjectService.updateByBo(bo));
    }

    /**
     * 删除预算科目
     *
     * @param ids 主键串
     */
    @SaCheckPermission("system:subject:remove")
    @Log(title = "预算科目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(iBudgetSubjectService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
