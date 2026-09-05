package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RequireFeature;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.WecomSyncLogVo;
import com.ruoyi.system.service.IWecomSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 企微通讯录同步Controller（T13.2 自建应用，高级版+ FEAT_WECOM）
 * <p>同步部门/成员到本租户组织架构，幂等可重试，全程写 wecom_sync_log 留痕；OAuth 免登引导见 PortalController。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/wecom")
@RequireFeature("FEAT_WECOM")
public class WecomController extends BaseController {

    private final IWecomSyncService wecomSyncService;

    /**
     * 触发通讯录同步（dept/user/all）
     */
    @SaCheckPermission("biz:wecom:sync")
    @Log(title = "企微通讯录同步", businessType = BusinessType.OTHER)
    @PostMapping("/sync/{syncType}")
    public R<WecomSyncLogVo> sync(@PathVariable String syncType) {
        return R.ok(wecomSyncService.syncContacts(syncType));
    }

    /**
     * 同步日志列表
     */
    @SaCheckPermission("biz:wecom:list")
    @GetMapping("/syncLog")
    public TableDataInfo<WecomSyncLogVo> syncLog(@RequestParam(required = false) String syncType,
                                                 @RequestParam(required = false) String status,
                                                 PageQuery pageQuery) {
        return wecomSyncService.queryPageList(syncType, status, pageQuery);
    }
}
