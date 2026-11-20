package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.bo.VisitorProfileBo;
import com.ruoyi.system.domain.vo.VisitorProfileVo;
import com.ruoyi.system.service.IVisitorProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Boss 游客信息管理Controller（M31 T31.6/T31.8）
 * <p>perms: micro:customer:profile:view / micro:customer:profile:edit / micro:customer:profile:upgrade
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/customer/profile")
public class MicroCustomerProfileController extends BaseController {

    private final IVisitorProfileService visitorProfileService;

    /**
     * Boss 查询临时游客列表（status=TEMPORARY + is_complete=0）
     */
    @SaCheckPermission("micro:customer:profile:view")
    @GetMapping("/list")
    public TableDataInfo<VisitorProfileVo> list(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        List<VisitorProfileVo> list = visitorProfileService.listTempVisitors(pageNum, pageSize);
        TableDataInfo<VisitorProfileVo> rspData = new TableDataInfo<>();
        rspData.setRows(list);
        rspData.setTotal(list.size());
        return rspData;
    }

    /**
     * Boss 补全游客信息
     */
    @SaCheckPermission("micro:customer:profile:edit")
    @Log(title = "游客信息管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{userId}")
    public R<Void> update(@PathVariable Long userId, @Validated @RequestBody VisitorProfileBo bo) {
        bo.setUserId(userId);
        visitorProfileService.updateVisitorProfile(bo);
        return R.ok();
    }

    /**
     * Boss 批量转正临时游客（T31.8）
     */
    @SaCheckPermission("micro:customer:profile:upgrade")
    @Log(title = "游客批量转正", businessType = BusinessType.UPDATE)
    @PostMapping("/batchUpgrade")
    public R<Map<String, Object>> batchUpgrade(@RequestBody List<Long> userIds) {
        return R.ok(visitorProfileService.batchUpgradeToFormal(userIds));
    }
}
