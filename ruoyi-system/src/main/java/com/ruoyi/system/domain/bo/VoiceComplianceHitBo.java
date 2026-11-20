package com.ruoyi.system.domain.bo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 合规命中记录查询对象 voice_compliance_hit（T18.1/T18.4，小微版 M18）
 * <p>★仅查询，无新增/编辑（由系统自动写入）。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Data
public class VoiceComplianceHitBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 命中类型筛选 */
    private String hitType;

    /** 处理状态筛选 */
    private String status;

    /** 命中时间-起始 */
    private Date hitTimeStart;

    /** 命中时间-结束 */
    private Date hitTimeEnd;

    /** 关联规则ID筛选 */
    private Long ruleId;
}
