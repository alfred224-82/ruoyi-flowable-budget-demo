package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 敏感词库对象 sensitive_word_library（T18.1，小微版 M18）
 * <p>混合表：tenant_id IS NULL = 平台级敏感词（Admin 管理），
 *         tenant_id 非 NULL = 租户自定义敏感词（租户管理员管理）。
 * <p>★不继承 TenantEntity，因 tenant_id 可为 NULL（平台级），手动管理 tenantId 字段。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sensitive_word_library")
public class SensitiveWordLibrary extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID（NULL=平台级，非NULL=租户自定义） */
    private Long tenantId;

    /** 敏感词内容 */
    private String word;

    /** 敏感词分类：POLITICAL/PORN/VIOLENCE/ABUSE/CUSTOM */
    private String category;

    /** 级别：WARN/BLOCK */
    private String level;

    /** 来源：MANUAL/IMPORT/SYSTEM */
    private String source;

    /** 状态：ENABLED/DISABLED */
    private String status;

    private String remark;
}
