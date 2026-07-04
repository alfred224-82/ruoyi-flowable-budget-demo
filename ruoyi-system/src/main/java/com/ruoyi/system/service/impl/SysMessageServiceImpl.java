package com.ruoyi.system.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysMessage;
import com.ruoyi.system.domain.vo.SysMessageVo;
import com.ruoyi.system.mapper.SysMessageMapper;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 系统消息通知Service实现
 *
 * @author ruoyi
 * @date 2026-07-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMessageServiceImpl implements ISysMessageService {

    private final SysMessageMapper baseMapper;

    @Override
    public Long getUnreadCount() {
        Long userId = LoginHelper.getUserId();
        return baseMapper.selectCount(
            Wrappers.<SysMessage>lambdaQuery()
                .eq(SysMessage::getUserId, userId)
                .eq(SysMessage::getIsRead, 0)
        );
    }

    @Override
    public TableDataInfo<SysMessageVo> selectPageMessageList(String messageType, Integer isRead, PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        LambdaQueryWrapper<SysMessage> lqw = Wrappers.<SysMessage>lambdaQuery()
            .eq(SysMessage::getUserId, userId)
            .eq(ObjectUtil.isNotEmpty(messageType), SysMessage::getMessageType, messageType)
            .eq(ObjectUtil.isNotNull(isRead), SysMessage::getIsRead, isRead)
            .orderByDesc(SysMessage::getCreateTime);
        Page<SysMessageVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public Boolean markAsRead(Long id) {
        SysMessage message = baseMapper.selectById(id);
        if (message == null) {
            return false;
        }
        // 只能标记自己的消息
        if (!message.getUserId().equals(LoginHelper.getUserId())) {
            return false;
        }
        message.setIsRead(1);
        message.setUpdateTime(new Date());
        return baseMapper.updateById(message) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean markAllAsRead() {
        Long userId = LoginHelper.getUserId();
        LambdaUpdateWrapper<SysMessage> uw = Wrappers.<SysMessage>lambdaUpdate()
            .eq(SysMessage::getUserId, userId)
            .eq(SysMessage::getIsRead, 0)
            .set(SysMessage::getIsRead, 1)
            .set(SysMessage::getUpdateTime, new Date());
        return baseMapper.update(null, uw) > 0;
    }

    @Override
    public void sendMessage(Long userId, String title, String content, String messageType, String bizType, Long bizId) {
        SysMessage message = new SysMessage();
        message.setUserId(userId);
        message.setTitle(title);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setIsRead(0);
        baseMapper.insert(message);
        log.info("发送系统消息: userId={}, type={}, title={}", userId, messageType, title);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatchMessage(List<Long> userIds, String title, String content, String messageType, String bizType, Long bizId) {
        List<SysMessage> messages = new ArrayList<>();
        for (Long userId : userIds) {
            SysMessage message = new SysMessage();
            message.setUserId(userId);
            message.setTitle(title);
            message.setContent(content);
            message.setMessageType(messageType);
            message.setBizType(bizType);
            message.setBizId(bizId);
            message.setIsRead(0);
            messages.add(message);
        }
        baseMapper.insertBatch(messages);
        log.info("批量发送系统消息: count={}, type={}, title={}", userIds.size(), messageType, title);
    }
}
