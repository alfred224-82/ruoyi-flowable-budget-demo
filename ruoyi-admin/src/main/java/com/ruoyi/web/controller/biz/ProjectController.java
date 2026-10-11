package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ProjectBo;
import com.ruoyi.system.domain.vo.FeeUserOptionVo;
import com.ruoyi.system.domain.vo.ProjectMemberVo;
import com.ruoyi.system.domain.vo.ProjectVo;
import com.ruoyi.system.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 项目统一容器Controller（T11.1 / T11.2 / T11.5）
 * <p>企业任务与活动报名共用容器；成员五枚举角色；业态分组工作台。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/project")
public class ProjectController extends BaseController {

    private final IProjectService projectService;

    /**
     * 项目容器列表
     */
    @SaCheckPermission("biz:project:list")
    @GetMapping("/list")
    public TableDataInfo<ProjectVo> list(ProjectBo bo, PageQuery pageQuery) {
        return projectService.queryPageList(bo, pageQuery);
    }

    /**
     * 业态分组工作台：各业态容器数量（T11.5）
     */
    @SaCheckPermission("biz:project:list")
    @GetMapping("/groupCount")
    public R<Map<String, Long>> groupCount() {
        return R.ok(projectService.countGroupByBiz());
    }

    /**
     * 容器详情
     */
    @SaCheckPermission("biz:project:query")
    @GetMapping("/{id}")
    public R<ProjectVo> getInfo(@PathVariable Long id) {
        return R.ok(projectService.queryById(id));
    }

    /**
     * 新建容器
     */
    @SaCheckPermission("biz:project:add")
    @Log(title = "项目容器", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody ProjectBo bo) {
        return R.ok(projectService.insertProject(bo));
    }

    /**
     * 编辑容器
     */
    @SaCheckPermission("biz:project:edit")
    @Log(title = "项目容器", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ProjectBo bo) {
        return toAjax(projectService.updateProject(bo));
    }

    /**
     * 发布容器（DRAFT→PUBLISHED）
     */
    @SaCheckPermission("biz:project:publish")
    @Log(title = "项目容器", businessType = BusinessType.UPDATE)
    @PutMapping("/publish/{id}")
    public R<Void> publish(@PathVariable Long id) {
        return toAjax(projectService.publish(id));
    }

    /**
     * 删除容器
     */
    @SaCheckPermission("biz:project:remove")
    @Log(title = "项目容器", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(projectService.deleteByIds(Arrays.asList(ids)));
    }

    /**
     * 容器成员列表（五枚举）
     */
    @SaCheckPermission("biz:member:list")
    @GetMapping("/members/{projectId}")
    public R<List<ProjectMemberVo>> members(@PathVariable Long projectId) {
        return R.ok(projectService.listMembers(projectId));
    }

    /**
     * 批量保存成员
     */
    @SaCheckPermission("biz:member:edit")
    @Log(title = "项目成员", businessType = BusinessType.UPDATE)
    @PutMapping("/members/{projectId}")
    public R<Void> saveMembers(@PathVariable Long projectId,
                               @RequestBody List<ProjectBo.ProjectMemberBo> members) {
        return toAjax(projectService.saveMembers(projectId, members));
    }

    /**
     * 移除成员
     */
    @SaCheckPermission("biz:member:remove")
    @Log(title = "项目成员", businessType = BusinessType.DELETE)
    @DeleteMapping("/members/{projectId}/{userId}")
    public R<Void> removeMember(@PathVariable Long projectId, @PathVariable Long userId) {
        return toAjax(projectService.removeMember(projectId, userId));
    }

    /**
     * 本租户启用状态用户下拉（容器成员/任务指派选择用）
     */
    @SaCheckPermission(value = {"biz:member:edit", "biz:task:assign", "biz:project:add"}, mode = SaMode.OR)
    @GetMapping("/userOptions")
    public R<List<FeeUserOptionVo>> userOptions() {
        return R.ok(projectService.listUserOptions());
    }
}
