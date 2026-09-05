package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.domain.vo.FeeOrgBudgetVersionVo;
import com.ruoyi.system.domain.vo.FeeOrgConsolidatedVo;

import java.util.List;

/**
 * 组织预算Service接口（T7.1 / FEAT_MULTI_ORG + FEAT_ORG_SNAPSHOT）
 * 按组织分别编制与锁定快照、集团合并视角
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeOrgBudgetService {

    /**
     * 组织维度预算编制列表（数据范围：本组织+下级组织）
     *
     * @param orgId  组织ID（必填）
     * @param period 预算期间 yyyy-MM（可空）
     */
    List<BudgetPreparationVo> listOrgPreparations(Long orgId, String period);

    /**
     * 集团合并视角：各组织编制汇总 + 集团合计行
     *
     * @param period 预算期间 yyyy-MM（可空）
     */
    List<FeeOrgConsolidatedVo> consolidated(String period);

    /**
     * 锁定组织预算快照（版本号+1不覆盖，锁定后不可变）
     */
    FeeOrgBudgetVersionVo lockSnapshot(Long orgId, String period);

    /**
     * 组织快照版本列表
     */
    List<FeeOrgBudgetVersionVo> listVersions(Long orgId);

    /**
     * 快照详情
     */
    FeeOrgBudgetVersionVo getVersion(Long id);
}
