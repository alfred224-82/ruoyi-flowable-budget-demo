package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.bo.OverBudgetApprovalBo;
import com.ruoyi.system.domain.vo.BudgetAdjustmentLogVo;
import com.ruoyi.system.domain.vo.OverBudgetApprovalVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 超预算审批Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IOverBudgetApprovalService {

    /**
     * 分页查询超预算审批列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    Page<OverBudgetApprovalVo> selectPageList(OverBudgetApprovalBo bo, PageQuery pageQuery);

    /**
     * 查询审批单详情
     *
     * @param id 主键ID
     * @return 审批单VO
     */
    OverBudgetApprovalVo queryById(Long id);

    /**
     * 处理审批（通过/驳回/调整科目预算）
     *
     * @param approvalId        审批单ID
     * @param action            审批动作
     * @param adjustFromSubject 调出科目
     * @param adjustAmount      调剂金额
     * @param opinion           审批意见
     * @return 影响行数
     */
    Boolean processApproval(Long approvalId, String action, String adjustFromSubject,
                            BigDecimal adjustAmount, String opinion);

    /**
     * 查询调剂流水
     *
     * @param approvalId 审批单ID
     * @return 调剂流水列表
     */
    List<BudgetAdjustmentLogVo> queryAdjustmentLog(Long approvalId);
}
