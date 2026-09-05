package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 映射规则变更审计对象 fee_mapping_audit_log（T6.4 / FEAT_RULE_ENGINE_CUSTOM）
 * 草稿→发布全程留痕（ADR-008）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fee_mapping_audit_log")
public class FeeMappingAuditLog extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 映射规则ID
     */
    private Long ruleId;

    /**
     * 操作类型(CREATE/UPDATE/PUBLISH/ROLLBACK/DELETE)
     */
    private String action;

    /**
     * 变更前内容(JSON)
     */
    private String draftContent;

    /**
     * 变更后内容(JSON)
     */
    private String publishContent;

    /**
     * 影响分析(JSON：影响预算/报销数量)
     */
    private String impactAnalysis;

    /**
     * 操作人ID
     */
    private Long operUserId;

    /**
     * 操作人姓名
     */
    private String operUserName;
}
