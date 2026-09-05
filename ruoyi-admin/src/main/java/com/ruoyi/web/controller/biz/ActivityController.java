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
import com.ruoyi.system.domain.bo.ProjectBo;
import com.ruoyi.system.domain.vo.ProjectVo;
import com.ruoyi.system.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 活动管理Controller（T12.1）
 * <p>活动复用 project 统一容器（强制 type=activity），不另建活动主表；
 * 报名/成团走 {@link ProjectSignupController}，主题装修走 {@link ThemeController}，直播位走 {@link LiveAnnouncementController}。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/activity")
public class ActivityController extends BaseController {

    private final IProjectService projectService;

    /** 活动容器类型 */
    private static final String TYPE_ACTIVITY = "activity";
    /** 活动结束状态 */
    private static final String STATUS_FINISHED = "FINISHED";

    /**
     * 活动列表（强制 type=activity）
     */
    @SaCheckPermission("biz:activity:list")
    @GetMapping("/list")
    public TableDataInfo<ProjectVo> list(ProjectBo bo, PageQuery pageQuery) {
        bo.setType(TYPE_ACTIVITY);
        return projectService.queryPageList(bo, pageQuery);
    }

    /**
     * 活动详情
     */
    @SaCheckPermission("biz:activity:query")
    @GetMapping("/{id}")
    public R<ProjectVo> getInfo(@PathVariable Long id) {
        return R.ok(projectService.queryById(id));
    }

    /**
     * 新建活动（强制 type=activity）
     */
    @SaCheckPermission("biz:activity:add")
    @Log(title = "活动管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ProjectBo bo) {
        bo.setType(TYPE_ACTIVITY);
        return R.ok(projectService.insertProject(bo));
    }

    /**
     * 编辑活动
     */
    @SaCheckPermission("biz:activity:edit")
    @Log(title = "活动管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ProjectBo bo) {
        return toAjax(projectService.updateProject(bo));
    }

    /**
     * 发布活动（DRAFT→PUBLISHED，发布后报名可用）
     */
    @SaCheckPermission("biz:activity:edit")
    @Log(title = "活动管理", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{id}")
    public R<Void> publish(@PathVariable Long id) {
        return toAjax(projectService.publish(id));
    }

    /**
     * 结束活动（→FINISHED）
     */
    @SaCheckPermission("biz:activity:edit")
    @Log(title = "活动管理", businessType = BusinessType.UPDATE)
    @PutMapping("/finish/{id}")
    public R<Void> finish(@PathVariable Long id) {
        ProjectBo bo = new ProjectBo();
        bo.setId(id);
        bo.setStatus(STATUS_FINISHED);
        return toAjax(projectService.updateProject(bo));
    }

    /**
     * 删除活动
     */
    @SaCheckPermission("biz:activity:remove")
    @Log(title = "活动管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(projectService.deleteByIds(Arrays.asList(ids)));
    }
}
