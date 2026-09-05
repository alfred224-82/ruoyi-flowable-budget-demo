package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.MessageLog;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.vo.MessageLogVo;
import com.ruoyi.system.mapper.MessageLogMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IMessageService;
import com.ruoyi.system.service.message.MessageContext;
import com.ruoyi.system.service.message.MessageProviderDispatcher;
import com.ruoyi.system.service.message.MessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 消息服务实现（T13.3 / T13.4）
 * <p>分发 Provider 发送并统一留痕；channel 为空时按当前租户 access_channel 推断，缺省 wechat。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements IMessageService {

    private final MessageProviderDispatcher dispatcher;
    private final MessageLogMapper messageLogMapper;
    private final SysTenantMapper tenantMapper;

    /** 内容摘要留痕最大长度（脱敏） */
    private static final int CONTENT_MAX = 200;
    /** 接收者留痕最大长度（脱敏） */
    private static final int RECEIVER_MAX = 64;

    @Override
    public MessageResult send(MessageContext context) {
        if (context == null) {
            return MessageResult.failed(null, "消息上下文为空");
        }
        if (StrUtil.isBlank(context.getChannel())) {
            context.setChannel(resolveChannel());
        }
        MessageResult result = dispatcher.send(context);
        writeLog(context, result);
        return result;
    }

    @Override
    public TableDataInfo<MessageLogVo> queryPageList(String bizType, String result, String provider, PageQuery pageQuery) {
        LambdaQueryWrapper<MessageLog> wrapper = Wrappers.<MessageLog>lambdaQuery()
            .eq(StrUtil.isNotBlank(bizType), MessageLog::getBizType, bizType)
            .eq(StrUtil.isNotBlank(result), MessageLog::getResult, result)
            .eq(StrUtil.isNotBlank(provider), MessageLog::getProvider, provider)
            .orderByDesc(MessageLog::getCreateTime);
        Page<MessageLogVo> page = messageLogMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * 解析当前租户通道（access_channel），缺省 wechat
     */
    private String resolveChannel() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId != null) {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant != null && StrUtil.isNotBlank(tenant.getAccessChannel())) {
                return tenant.getAccessChannel();
            }
        }
        return MessageProviderDispatcher.CHANNEL_WECHAT;
    }

    /**
     * 写发送留痕（失败不阻断主流程，仅告警）
     */
    private void writeLog(MessageContext context, MessageResult result) {
        try {
            MessageLog entity = new MessageLog();
            entity.setAccessChannel(context.getChannel());
            entity.setProvider(result.getProvider());
            entity.setBizType(context.getBizType());
            entity.setReceiver(StrUtil.maxLength(context.getReceiver(), RECEIVER_MAX));
            entity.setTemplateId(context.getTemplateId());
            entity.setTitle(context.getTitle());
            entity.setContent(StrUtil.maxLength(context.getContent(), CONTENT_MAX));
            entity.setResult(result.getResult());
            entity.setErrorMsg(StrUtil.maxLength(result.getErrorMsg(), 1000));
            messageLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("[Message] 发送留痕写入失败 provider={} result={}", result.getProvider(), result.getResult(), e);
        }
    }
}
