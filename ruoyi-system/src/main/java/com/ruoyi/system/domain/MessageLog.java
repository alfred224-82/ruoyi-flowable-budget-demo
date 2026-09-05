package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 消息发送记录对象 biz_message_log（T13.3 IMessageProvider 留痕）
 * <p>发送失败/订阅授权不足降级为 SKIPPED，不阻塞主流程；表仅含 create_time，故不继承 BaseEntity。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("biz_message_log")
public class MessageLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 发送时租户通道：wechat/wecom */
    private String accessChannel;

    /** 消息Provider：wechat_template/wechat_kf/wecom_app */
    private String provider;

    /** 业务类型：approval/warning/expire_remind/live_remind */
    private String bizType;

    /** 接收者(openid/企微userid/用户ID,脱敏) */
    private String receiver;

    /** 模板ID */
    private String templateId;

    /** 标题 */
    private String title;

    /** 内容摘要(脱敏) */
    private String content;

    /** 结果：SUCCESS/FAILED/SKIPPED(订阅授权不足跳过) */
    private String result;

    /** 失败原因 */
    private String errorMsg;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
