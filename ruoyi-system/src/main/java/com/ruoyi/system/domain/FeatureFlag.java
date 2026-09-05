package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 功能开关对象 feature_flag（T1.7 / T10.3 表驱动开关）
 * <p>系统级共享表，不含 tenant_id（已加入 IGNORE_TENANT_TABLES）。开关从
 * {@code EditionFeatureConfig} 静态集合迁到本表，表为准、常量为兜底（表缺失时启用并告警）。
 * <p>platform_admin 可启停（status），但【不可】改版本归属（basic/advanced/pro_enabled）以免串版本。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("feature_flag")
public class FeatureFlag extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 开关编码（FEAT_*，与设计文档 5.1 一致）
     */
    private String code;

    /**
     * 开关名称
     */
    private String name;

    /**
     * 开关分组：A(资金管控下移11)/B(高级版原有9)/C(多业态底座新增6)/D(专业版原有7)
     */
    private String flagGroup;

    /**
     * 所属业态包（NULL=全业态通用）
     */
    private String bizCode;

    /**
     * 基础版默认是否开启（0否 1是）
     */
    private Integer basicEnabled;

    /**
     * 高级版默认是否开启（0否 1是）
     */
    private Integer advancedEnabled;

    /**
     * 专业版默认是否开启（0否 1是）
     */
    private Integer proEnabled;

    /**
     * 状态（0启用 1停用）
     */
    private String status;

    /**
     * 备注
     */
    private String remark;
}
