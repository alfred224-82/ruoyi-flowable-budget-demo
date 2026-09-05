package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量审核结果VO（T6.7 / FEAT_EVIDENCE_CHAIN）
 * 逐条独立事务审批，返回成功数与失败明细（不整体回滚）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Data
public class BatchApprovalResultVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成功条数
     */
    private int successNum;

    /**
     * 失败明细（单据ID：失败原因）
     */
    private List<String> failures = new ArrayList<>();
}
