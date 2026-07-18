package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.vo.ExpenseBillVo;

/**
 * 报销单审批Service接口
 *
 * @author ruoyi
 * @date 2026-07-15
 */
public interface IExpenseApprovalService {

    /**
     * 查询待审批报销单列表（按角色过滤）
     *
     * @param pageQuery 分页参数
     * @return 待审批报销单分页列表
     */
    Page<ExpenseBillVo> selectPendingApprovalPage(PageQuery pageQuery);

    /**
     * 部门经理审批通过
     *
     * @param billId  报销单ID
     * @param opinion 审批意见
     * @return 影响行数
     */
    Boolean approve(Long billId, String opinion);

    /**
     * 部门经理驳回（reason必填）
     *
     * @param billId  报销单ID
     * @param reason  驳回原因
     * @param opinion 审批意见
     * @return 影响行数
     */
    Boolean reject(Long billId, String reason, String opinion);
}
