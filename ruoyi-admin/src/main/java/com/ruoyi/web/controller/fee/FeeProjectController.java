package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.FeeProjectBo;
import com.ruoyi.system.domain.vo.FeeMemberLoadVo;
import com.ruoyi.system.domain.vo.FeeProjectMemberVo;
import com.ruoyi.system.domain.vo.FeeProjectProfitVo;
import com.ruoyi.system.domain.vo.FeeProjectSubjectVo;
import com.ruoyi.system.domain.vo.FeeProjectVo;
import com.ruoyi.system.domain.vo.FeeUserOptionVo;
import com.ruoyi.system.service.IFeeProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目管理Controller（T5.1）
 * 立项/审批/项目预算按大类分配/科目/成员
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/project")
public class FeeProjectController extends BaseController {

    private final IFeeProjectService projectService;

    /**
     * 项目列表（数据范围：项目经理仅本人参与项目）
     */
    @SaCheckPermission("fee:project:list")
    @GetMapping("/list")
    public TableDataInfo<FeeProjectVo> list(FeeProjectBo bo, PageQuery pageQuery) {
        return projectService.queryPageList(bo, pageQuery);
    }

    /**
     * 项目详情
     */
    @SaCheckPermission("fee:project:query")
    @GetMapping("/{id}")
    public R<FeeProjectVo> getInfo(@PathVariable Long id) {
        return R.ok(projectService.queryById(id));
    }

    /**
     * 立项（老板建/财务代建/项目经理申请）
     */
    @SaCheckPermission("fee:project:add")
    @Log(title = "项目管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody FeeProjectBo bo) {
        return R.ok(projectService.insertProject(bo));
    }

    /**
     * 编辑项目（仅 待审批/草稿 状态）
     */
    @SaCheckPermission("fee:project:edit")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody FeeProjectBo bo) {
        return toAjax(projectService.updateProject(bo));
    }

    /**
     * 老板审批通过
     */
    @SaCheckPermission("fee:project:approve")
    @Log(title = "项目审批", businessType = BusinessType.UPDATE)
    @PutMapping("/approve/{id}")
    public R<Void> approve(@PathVariable Long id) {
        return toAjax(projectService.approveProject(id));
    }

    /**
     * 审批驳回
     */
    @SaCheckPermission("fee:project:approve")
    @Log(title = "项目审批", businessType = BusinessType.UPDATE)
    @PutMapping("/reject/{id}")
    public R<Void> reject(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return toAjax(projectService.rejectProject(id, reason));
    }

    /**
     * 草稿（含驳回回退）提交审批
     */
    @SaCheckPermission("fee:project:edit")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping("/submit/{id}")
    public R<Void> submit(@PathVariable Long id) {
        return toAjax(projectService.submitProject(id));
    }

    /**
     * 关闭项目
     */
    @SaCheckPermission("fee:project:close")
    @Log(title = "项目管理", businessType = BusinessType.UPDATE)
    @PutMapping("/close/{id}")
    public R<Void> close(@PathVariable Long id) {
        return toAjax(projectService.closeProject(id));
    }

    /**
     * 进行中项目下拉
     */
    @SaCheckPermission("fee:project:list")
    @GetMapping("/activeList")
    public R<List<FeeProjectVo>> activeList() {
        return R.ok(projectService.listActiveProjects());
    }

    /**
     * 项目利润拆解（纸面/实际分离）
     */
    @SaCheckPermission("fee:project:query")
    @GetMapping("/profit/{id}")
    public R<FeeProjectProfitVo> profit(@PathVariable Long id) {
        return R.ok(projectService.getProfitDetail(id));
    }

    /**
     * 项目科目列表
     */
    @SaCheckPermission("fee:project:query")
    @GetMapping("/subjects/{id}")
    public R<List<FeeProjectSubjectVo>> subjects(@PathVariable Long id) {
        return R.ok(projectService.listSubjects(id));
    }

    /**
     * 批量设置项目科目（按大类分配预算）
     */
    @SaCheckPermission("fee:project:edit")
    @Log(title = "项目科目", businessType = BusinessType.UPDATE)
    @PutMapping("/subjects/{id}")
    public R<Void> saveSubjects(@PathVariable Long id, @RequestBody List<FeeProjectBo.FeeProjectSubjectBo> subjects) {
        return toAjax(projectService.saveSubjects(id, subjects));
    }

    /**
     * 项目成员列表
     */
    @SaCheckPermission("fee:project:query")
    @GetMapping("/members/{id}")
    public R<List<FeeProjectMemberVo>> members(@PathVariable Long id) {
        return R.ok(projectService.listMembers(id));
    }

    /**
     * 批量保存项目成员
     */
    @SaCheckPermission("fee:project:edit")
    @Log(title = "项目成员", businessType = BusinessType.UPDATE)
    @PutMapping("/members/{id}")
    public R<Void> saveMembers(@PathVariable Long id, @RequestBody List<FeeProjectBo.FeeProjectMemberBo> members) {
        return toAjax(projectService.saveMembers(id, members));
    }

    /**
     * 报销人(employee)角色成员下拉（项目成员选择用）
     */
    @SaCheckPermission(value = {"fee:project:add", "fee:project:edit"}, mode = SaMode.OR)
    @GetMapping("/reimbursers")
    public R<List<FeeUserOptionVo>> reimbursers() {
        return R.ok(projectService.listReimbursers());
    }

    /**
     * 项目经理(manager)角色成员下拉（立项选择用）
     */
    @SaCheckPermission(value = {"fee:project:add", "fee:project:edit"}, mode = SaMode.OR)
    @GetMapping("/managers")
    public R<List<FeeUserOptionVo>> managers() {
        return R.ok(projectService.listManagers());
    }

    /**
     * 成员跨项目负载检查：投入比例合计>100%仅提示不拦截（T7.5 / FEAT_MEMBER_LOAD）
     *
     * @param userId 成员用户ID（可空=当前登录用户）
     */
    @RequireFeature("FEAT_MEMBER_LOAD")
    @SaCheckPermission("fee:project:query")
    @GetMapping("/memberLoad")
    public R<FeeMemberLoadVo> memberLoad(@RequestParam(required = false) Long userId) {
        return R.ok(projectService.memberLoadCheck(userId));
    }
}
