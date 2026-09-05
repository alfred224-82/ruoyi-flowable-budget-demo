package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 功能开关视图对象 feature_flag（T10.3）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class FeatureFlagVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 开关编码（FEAT_*） */
    private String code;

    /** 开关名称 */
    private String name;

    /** 开关分组：A/B/C/D */
    private String flagGroup;

    /** 所属业态包（NULL=全业态通用） */
    private String bizCode;

    /** 基础版默认是否开启（0否 1是） */
    private Integer basicEnabled;

    /** 高级版默认是否开启（0否 1是） */
    private Integer advancedEnabled;

    /** 专业版默认是否开启（0否 1是） */
    private Integer proEnabled;

    /** 状态（0启用 1停用） */
    private String status;

    /** 备注 */
    private String remark;
}
