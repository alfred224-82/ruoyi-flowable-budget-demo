package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息发送记录视图对象 biz_message_log（T13.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class MessageLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 发送时租户通道：wechat/wecom */
    private String accessChannel;

    /** 消息Provider：wechat_template/wechat_kf/wecom_app */
    private String provider;

    /** 业务类型：approval/warning/expire_remind/live_remind */
    private String bizType;

    /** 接收者(脱敏) */
    private String receiver;

    /** 模板ID */
    private String templateId;

    /** 标题 */
    private String title;

    /** 内容摘要(脱敏) */
    private String content;

    /** 结果：SUCCESS/FAILED/SKIPPED */
    private String result;

    /** 失败原因 */
    private String errorMsg;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
