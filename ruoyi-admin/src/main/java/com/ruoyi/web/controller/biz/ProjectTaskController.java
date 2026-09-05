package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ProjectTaskBo;
import com.ruoyi.system.domain.vo.ProjectTaskVo;
import com.ruoyi.system.service.IProjectTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 项目企业任务Controller（T11.3，type=general 容器）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/task")
public class ProjectTaskController extends BaseController {

    private final IProjectTaskService taskService;

    /**
     * 任务列表
     */
    @SaCheckPermission("biz:task:list")
    @GetMapping("/list")
    public TableDataInfo<ProjectTaskVo> list(ProjectTaskBo bo, PageQuery pageQuery) {
        return taskService.queryPageList(bo, pageQuery);
    }

    /**
     * 任务详情
     */
    @SaCheckPermission("biz:task:query")
    @GetMapping("/{id}")
    public R<ProjectTaskVo> getInfo(@PathVariable Long id) {
        return R.ok(taskService.queryById(id));
    }

    /**
     * 新建任务
     */
    @SaCheckPermission("biz:task:add")
    @Log(title = "企业任务", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ProjectTaskBo bo) {
        return R.ok(taskService.insertTask(bo));
    }

    /**
     * 编辑任务
     */
    @SaCheckPermission("biz:task:edit")
    @Log(title = "企业任务", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ProjectTaskBo bo) {
        return toAjax(taskService.updateTask(bo));
    }

    /**
     * 指派 + 进度更新
     */
    @SaCheckPermission("biz:task:assign")
    @Log(title = "企业任务", businessType = BusinessType.UPDATE)
    @PutMapping("/assign/{id}")
    public R<Void> assign(@PathVariable Long id,
                          @RequestParam(required = false) Long assigneeUserId,
                          @RequestParam(required = false) Integer progress) {
        return toAjax(taskService.assign(id, assigneeUserId, progress));
    }

    /**
     * 删除任务
     */
    @SaCheckPermission("biz:task:remove")
    @Log(title = "企业任务", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(taskService.deleteByIds(Arrays.asList(ids)));
    }
}
