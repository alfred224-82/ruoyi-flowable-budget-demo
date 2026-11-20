package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 敏感词命中日志视图对象 sensitive_word_hit_log（M18 T18.1，看板/只读列表用）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Data
public class SensitiveWordHitLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long userId;
    /** 输入渠道：text/voice_ocr/image_ocr */
    private String inputChannel;
    /** 命中词（PASS 时为 null） */
    private String hitWord;
    /** 命中分类 */
    private String hitCategory;
    /** 命中位置 */
    private Integer hitPosition;
    /** 原文脱敏截取（前 100 字） */
    private String originalText;
    /** 结果：PASS/FAIL */
    private String result;
    private String bizType;
    private String bizId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
