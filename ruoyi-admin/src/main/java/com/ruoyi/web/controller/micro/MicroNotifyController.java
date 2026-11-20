package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.MicroShopConfigBo;
import com.ruoyi.system.domain.vo.SysMessageVo;
import com.ruoyi.system.service.IMicroShopConfigService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 消息提醒Controller（T15.20 老板小程序 micro:notify:list / T15.24 店铺管理 通知方式设置 micro:notify:config）
 * <p>消息提醒复用 {@link ISysMessageService}（sys_message，已按当前登录用户隔离）：结算提醒/预警等；
 * 通知方式设置持久化到 tenant_biz_config.module_overrides（{@link IMicroShopConfigService#saveNotifyConfig}）。
 * <p>★V1 零支付合规：通知仅经营提醒，不含任何支付/扣款通知触发。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/notify")
public class MicroNotifyController extends BaseController {

    private final ISysMessageService sysMessageService;
    private final IMicroShopConfigService shopConfigService;

    /**
     * 消息提醒列表（当前登录人，可按消息类型/已读状态过滤）
     */
    @SaCheckPermission("micro:notify:list")
    @GetMapping("/list")
    public TableDataInfo<SysMessageVo> list(@RequestParam(required = false) String messageType,
                                            @RequestParam(required = false) Integer isRead,
                                            PageQuery pageQuery) {
        return sysMessageService.selectPageMessageList(messageType, isRead, pageQuery);
    }

    /**
     * 未读消息数
     */
    @SaCheckPermission("micro:notify:list")
    @GetMapping("/unreadCount")
    public R<Long> unreadCount() {
        return R.ok(sysMessageService.getUnreadCount());
    }

    /**
     * 标记单条消息已读
     */
    @SaCheckPermission("micro:notify:list")
    @PutMapping("/read/{id}")
    public R<Void> read(@PathVariable Long id) {
        return toAjax(Boolean.TRUE.equals(sysMessageService.markAsRead(id)) ? 1 : 0);
    }

    /**
     * 标记全部消息已读
     */
    @SaCheckPermission("micro:notify:list")
    @PutMapping("/readAll")
    public R<Void> readAll() {
        return toAjax(Boolean.TRUE.equals(sysMessageService.markAllAsRead()) ? 1 : 0);
    }

    /**
     * 读取通知方式设置（T15.24 店铺管理）
     */
    @SaCheckPermission("micro:notify:config")
    @GetMapping("/config")
    public R<String> getConfig() {
        return R.ok(shopConfigService.getConfig().getNotifyConfig());
    }

    /**
     * 保存通知方式设置（T15.24 店铺管理，写入 tenant_biz_config.module_overrides.notifyConfig）
     */
    @SaCheckPermission("micro:notify:config")
    @Log(title = "通知方式设置", businessType = BusinessType.UPDATE)
    @PutMapping("/config")
    public R<Void> saveConfig(@RequestBody MicroShopConfigBo bo) {
        return toAjax(shopConfigService.saveNotifyConfig(bo.getNotifyConfig()));
    }
}
