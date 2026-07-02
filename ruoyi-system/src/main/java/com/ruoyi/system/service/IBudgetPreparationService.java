package com.ruoyi.system.service;

import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.domain.vo.BudgetRejectHistoryVo;
import com.ruoyi.system.domain.vo.ValidationResultVo;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 预算编制主表Service接口
 *
 * @author ruoyi
 * @date 2026-06-19
 */
public interface IBudgetPreparationService {

    /**
     * 查询预算编制
     */
    BudgetPreparationVo queryById(Long id);

    /**
     * 查询预算编制列表
     */
    TableDataInfo<BudgetPreparationVo> queryPageList(BudgetPreparationBo bo, PageQuery pageQuery);

    /**
     * 查询预算编制列表
     */
    List<BudgetPreparationVo> queryList(BudgetPreparationBo bo);

    /**
     * 新增预算编制
     */
    Boolean insertByBo(BudgetPreparationBo bo);

    /**
     * 修改预算编制
     */
    Boolean updateByBo(BudgetPreparationBo bo);

    /**
     * 校验并批量删除预算编制信息
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 提交审核（单个）
     */
    Boolean submitForReview(Long id);

    /**
     * 批量提交审核
     */
    Boolean batchSubmitForReview(List<Long> ids);

    /**
     * 批量审批通过
     */
    Boolean batchApprove(List<Long> ids, String remark);

    /**
     * 审批通过（单个）
     */
    Boolean approve(Long id, String remark);

    /**
     * 审批驳回（单个）
     */
    Boolean reject(Long id, String reason);

    /**
     * 获取校验结果
     */
    List<ValidationResultVo> getValidationResults(Long sheetId);

    /**
     * 分公司打回至部门
     * @param id 预算单ID
     * @param deptId 目标部门ID
     * @param reason 打回理由
     */
    Boolean sendBackToDept(Long id, Long deptId, String reason);

    /**
     * 查询驳回历史
     * @param sheetId 预算单ID
     */
    List<BudgetRejectHistoryVo> queryRejectHistory(Long sheetId);
}
