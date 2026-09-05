package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.MessageLogVo;
import com.ruoyi.system.service.IMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息发送记录Controller（T13.3 IMessageProvider 留痕，基础版标配）
 * <p>全通道发送记录可查；发送由业务侧调用 {@link IMessageService#send} 触发，本控制器仅提供审计查询。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/message")
public class MessageController extends BaseController {

    private final IMessageService messageService;

    /**
     * 消息发送记录列表
     */
    @SaCheckPermission("biz:message:list")
    @GetMapping("/list")
    public TableDataInfo<MessageLogVo> list(@RequestParam(required = false) String bizType,
                                            @RequestParam(required = false) String result,
                                            @RequestParam(required = false) String provider,
                                            PageQuery pageQuery) {
        return messageService.queryPageList(bizType, result, provider, pageQuery);
    }
}
