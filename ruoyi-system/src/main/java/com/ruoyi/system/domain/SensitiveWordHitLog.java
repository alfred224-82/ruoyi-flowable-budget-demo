package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 敏感词命中日志对象 sensitive_word_hit_log（M18 T18.1）
 * <p>三路（text/voice_ocr/image_ocr）共用；PASS/FAIL 均记录（PASS 仅记调用事实，hit_word 为 NULL）。
 * <p>★仅 create_time 无完整审计列，故不继承 BaseEntity；tenant_id 走标准租户隔离
 *   （未加入 IGNORE_TENANT_TABLES，租户查询自动过滤，Admin 免过滤可跨租户聚合）。
 * <p>★由引擎在 @Async 线程写入，无登录上下文，tenantId 显式赋值。
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
@TableName("sensitive_word_hit_log")
public class SensitiveWordHitLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 触发用户ID */
    private Long userId;

    /** 输入渠道：text/voice_ocr/image_ocr */
    private String inputChannel;

    /** 命中词（PASS 时为 NULL） */
    private String hitWord;

    /** 命中分类 */
    private String hitCategory;

    /** 命中位置（原文中的字符偏移） */
    private Integer hitPosition;

    /** 原文脱敏截取（前 100 字） */
    private String originalText;

    /** 结果：PASS/FAIL */
    private String result;

    /** 业务类型：rating/activity/poster/voice/chat */
    private String bizType;

    /** 业务主键 */
    private String bizId;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
