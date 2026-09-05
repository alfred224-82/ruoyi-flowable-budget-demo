package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 数据脱敏规则对象 fee_desensitize_rule
 * 设计文档 Phase 10 Task 10.3
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Data
@TableName("fee_desensitize_rule")
public class FeeDesensitizeRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 脱敏字段名 */
    private String fieldName;

    /** 字段中文名 */
    private String fieldLabel;

    /** 脱敏方式（PHONE/ID_CARD/BANK_CARD/NAME/CUSTOM） */
    private String maskType;

    /** 自定义脱敏正则 */
    private String maskPattern;

    /** 拦截动作（MASK/BLOCK） */
    private String interceptAction;

    /** 敏感词黑名单JSON */
    private String sensitiveWords;

    /** 状态 */
    private String status;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
