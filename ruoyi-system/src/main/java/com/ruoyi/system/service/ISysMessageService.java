package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.SysMessageVo;

import java.util.List;

/**
 * 系统消息通知Service接口
 *
 * @author ruoyi
 * @date 2026-07-04
 */
public interface ISysMessageService {

    /**
     * 获取当前用户未读消息数
     */
    Long getUnreadCount();

    /**
     * 分页查询当前用户消息列表
     */
    TableDataInfo<SysMessageVo> selectPageMessageList(String messageType, Integer isRead, PageQuery pageQuery);

    /**
     * 标记单条消息为已读
     */
    Boolean markAsRead(Long id);

    /**
     * 标记当前用户全部消息为已读
     */
    Boolean markAllAsRead();

    /**
     * 发送系统消息（内部调用，不需要Controller）
     *
     * @param userId      接收用户ID
     * @param title       消息标题
     * @param content     消息内容
     * @param messageType 消息类型（APPROVAL/REJECT/SYSTEM）
     * @param bizType     业务类型
     * @param bizId       业务ID
     */
    void sendMessage(Long userId, String title, String content, String messageType, String bizType, Long bizId);

    /**
     * 批量发送消息（给多个用户）
     */
    void sendBatchMessage(List<Long> userIds, String title, String content, String messageType, String bizType, Long bizId);
}
