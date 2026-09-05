package com.ruoyi.system.service.message;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息发送上下文（T13.3 IMessageProvider 抽象入参）
 * <p>由业务侧构造，分发器据 channel/provider 选择具体 Provider；引擎不感知通道差异。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class MessageContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 租户通道：wechat/wecom（provider 为空时据此选择默认 Provider） */
    private String channel;

    /** 显式指定 Provider：wechat_template/wechat_kf/wecom_app（优先于 channel 推断） */
    private String provider;

    /** 业务类型：approval/warning/expire_remind/live_remind */
    private String bizType;

    /** 接收者（openid/企微userid/用户ID） */
    private String receiver;

    /** 模板ID */
    private String templateId;

    /** 标题 */
    private String title;

    /** 正文内容 */
    private String content;

    /** 模板变量/扩展数据 */
    private Map<String, Object> data = new HashMap<>();
}
