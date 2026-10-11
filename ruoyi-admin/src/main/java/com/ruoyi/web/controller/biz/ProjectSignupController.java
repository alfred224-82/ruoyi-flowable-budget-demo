package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.ProjectSignupBo;
import com.ruoyi.system.domain.vo.ProjectSignupVo;
import com.ruoyi.system.service.IProjectSignupService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 项目活动报名Controller（T11.3 / T11.4）
 * <p>报名登记（满员候补）+ 成团判定（策略模式）+ 签到。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/signup")
public class ProjectSignupController extends BaseController {

    private final IProjectSignupService signupService;

    /**
     * 报名列表
     */
    @SaCheckPermission("biz:signup:list")
    @GetMapping("/list")
    public TableDataInfo<ProjectSignupVo> list(ProjectSignupBo bo, PageQuery pageQuery) {
        return signupService.queryPageList(bo, pageQuery);
    }

    /**
     * 报名登记（满员进候补，返回成团判定结果）
     */
    @SaCheckPermission("biz:signup:add")
    @Log(title = "活动报名", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Map<String, Object>> signup(@Validated(AddGroup.class) @RequestBody ProjectSignupBo bo) {
        return R.ok(signupService.signup(bo));
    }

    /**
     * 取消报名
     */
    @SaCheckPermission("biz:signup:remove")
    @Log(title = "活动报名", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> cancel(@PathVariable Long id) {
        return toAjax(signupService.cancel(id));
    }

    /**
     * 签到（NOT_SIGNED→SIGNED/ABSENT）
     */
    @SaCheckPermission("biz:signup:checkin")
    @Log(title = "活动报名", businessType = BusinessType.UPDATE)
    @PutMapping("/checkin/{id}")
    public R<Void> checkin(@PathVariable Long id, @RequestParam(required = false) String signInStatus) {
        return toAjax(signupService.checkin(id, signInStatus));
    }

    /**
     * 查询容器当前成团状态（策略实时判定，不写库）
     */
    @SaCheckPermission("biz:signup:query")
    @GetMapping("/teamStatus/{projectId}")
    public R<Map<String, Object>> teamStatus(@PathVariable Long projectId) {
        return R.ok(signupService.teamStatus(projectId));
    }
}
