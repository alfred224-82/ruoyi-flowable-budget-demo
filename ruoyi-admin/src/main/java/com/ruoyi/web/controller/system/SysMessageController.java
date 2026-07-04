package com.ruoyi.web.controller.system;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.SysMessageVo;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 系统消息通知
 *
 * @author ruoyi
 * @date 2026-07-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/message")
public class SysMessageController extends BaseController {

    private final ISysMessageService sysMessageService;

    /**
     * 获取当前用户未读消息数
     */
    @GetMapping("/unreadCount")
    public R<Long> getUnreadCount() {
        return R.ok(sysMessageService.getUnreadCount());
    }

    /**
     * 分页查询当前用户消息列表
     */
    @GetMapping("/list")
    public TableDataInfo<SysMessageVo> list(
        @RequestParam(required = false) String messageType,
        @RequestParam(required = false) Integer isRead,
        PageQuery pageQuery) {
        return sysMessageService.selectPageMessageList(messageType, isRead, pageQuery);
    }

    /**
     * 标记单条消息为已读
     */
    @PutMapping("/read/{id}")
    public R<Void> markAsRead(@PathVariable Long id) {
        return toAjax(sysMessageService.markAsRead(id));
    }

    /**
     * 标记全部消息为已读
     */
    @PutMapping("/readAll")
    public R<Void> markAllAsRead() {
        return toAjax(sysMessageService.markAllAsRead());
    }
}
