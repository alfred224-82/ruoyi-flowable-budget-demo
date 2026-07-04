package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统消息通知对象 sys_message
 *
 * @author ruoyi
 * @date 2026-07-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 接收用户ID
     */
    private Long userId;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（APPROVAL-审批通知/REJECT-驳回通知/SYSTEM-系统通知）
     */
    private String messageType;

    /**
     * 业务类型（如 budget_preparation）
     */
    private String bizType;

    /**
     * 业务ID（用于跳转）
     */
    private Long bizId;

    /**
     * 是否已读（0未读 1已读）
     */
    private Integer isRead;
}
