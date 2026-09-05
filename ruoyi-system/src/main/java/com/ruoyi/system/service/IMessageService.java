package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.MessageLogVo;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageResult;

/**
 * 消息服务接口（T13.3）
 * <p>统一封装 Provider 分发 + 发送留痕（写 biz_message_log）；发送失败/降级不阻塞主流程。
 * channel 为空时按当前租户 sys_tenant.access_channel 推断（T13.4 端适配）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IMessageService {

    /**
     * 发送消息并留痕
     *
     * @param context 消息上下文（channel 为空则取当前租户 access_channel）
     * @return 发送结果（SUCCESS/FAILED/SKIPPED）
     */
    MessageResult send(MessageContext context);

    /**
     * 发送记录分页查询
     *
     * @param bizType  业务类型（可空）
     * @param result   结果（可空）
     * @param provider Provider（可空）
     */
    TableDataInfo<MessageLogVo> queryPageList(String bizType, String result, String provider, PageQuery pageQuery);
}
