package com.ruoyi.web.controller.micro;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.bo.VisitorProfileBo;
import com.ruoyi.system.domain.vo.VisitorProfileVo;
import com.ruoyi.system.service.IVisitorProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 游客自助服务Controller（M31 T31.6/T31.7，登录即可访问）
 * <p>游客查询/完善自己的信息 + 主动升级为正式顾客。
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/visitor")
public class VisitorSelfServiceController extends BaseController {

    private final IVisitorProfileService visitorProfileService;

    /**
     * 当前游客查询自己的补全状态
     */
    @GetMapping("/profile/me")
    public R<VisitorProfileVo> myProfile() {
        Long userId = LoginHelper.getUserId();
        return R.ok(visitorProfileService.getMyProfile(userId));
    }

    /**
     * 游客自助完善信息
     */
    @Log(title = "游客自助完善", businessType = BusinessType.UPDATE)
    @PutMapping("/profile/me")
    public R<Void> updateMyProfile(@Validated @RequestBody VisitorProfileBo bo) {
        Long userId = LoginHelper.getUserId();
        visitorProfileService.updateMyProfile(userId, bo);
        return R.ok();
    }

    /**
     * 游客主动升级为正式顾客（T31.7）
     */
    @Log(title = "游客升级正式顾客", businessType = BusinessType.UPDATE)
    @PostMapping("/upgrade/formal")
    public R<Void> upgradeToFormal(@Validated @RequestBody VisitorProfileBo bo) {
        Long userId = LoginHelper.getUserId();
        visitorProfileService.upgradeToFormal(userId, bo);
        return R.ok();
    }
}
