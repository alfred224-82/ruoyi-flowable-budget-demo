package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.bo.BudgetAdjustmentAppBo;
import com.ruoyi.system.domain.vo.BudgetAdjustmentAppVo;

import java.math.BigDecimal;

/**
 * 预算调整申请Service接口
 *
 * @author ruoyi
 * @date 2026-07-15
 */
public interface IBudgetAdjustmentAppService {

    /**
     * 查询申请详情
     */
    BudgetAdjustmentAppVo queryById(Long id);

    /**
     * 分页查询申请列表
     */
    Page<BudgetAdjustmentAppVo> selectPageList(BudgetAdjustmentAppBo bo, PageQuery pageQuery);

    /**
     * 报销人发起预算调整申请
     */
    BudgetAdjustmentAppVo apply(BudgetAdjustmentAppBo bo);

    /**
     * 审批人同意分摊
     *
     * @param id             申请单ID
     * @param allocateAmount 分摊金额（分公司/总公司审批时必填，表示分摊到下级资金池的金额；部门审批时可为空，默认按差额计算）
     * @param opinion        审批意见
     */
    Boolean approve(Long id, BigDecimal allocateAmount, String opinion);

    /**
     * 审批人驳回
     */
    Boolean reject(Long id, String reason);

    /**
     * 向上级转审
     */
    Boolean escalate(Long id, String reason);

    /**
     * 待我审批列表
     */
    Page<BudgetAdjustmentAppVo> pendingList(PageQuery pageQuery);

    /**
     * 获取当前用户部门下指定科目的预算余额
     */
    BigDecimal getSubjectBudget(String subjectCode);

    /**
     * 申请人撤回申请（仅审批中的可撤回）
     */
    Boolean cancel(Long id);
}
