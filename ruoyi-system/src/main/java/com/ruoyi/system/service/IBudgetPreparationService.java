package com.ruoyi.system.service;

import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;
import com.ruoyi.system.domain.vo.BudgetPreparationDetailVo;
import com.ruoyi.system.domain.vo.BudgetRejectHistoryVo;
import com.ruoyi.system.domain.vo.ValidationResultVo;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.bo.BudgetCategoryQuotaBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
     * 完成编制（将状态设置为 Completed，只有 Completed 才能提交审核）
     */
    Boolean completePreparation(Long id);

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

    /**
     * 获取上月已审批通过的报销实际数据或预算编制数据（用于初始化本月预算金额）
     * 优先取报销单，无报销单时回退到上月预算编制科目金额
     * @param orgId 部门ID
     * @param budgetYear 预算年度（上月）
     * @param budgetMonth 预算月份（上月）
     * @return Map包含 dataSource(EXPENSE/BUDGET/NONE) + details(明细列表)
     */
    Map<String, Object> getPreviousMonthApprovedDetails(Long orgId, Integer budgetYear, Integer budgetMonth);

    /**
     * 查询老板大类预算额度（T5.14 ①）：返回启用费用大类主数据 + 当期已录额度 + 单据状态
     * @param budgetYear 预算年度
     * @param budgetMonth 预算月份
     * @param departmentId 部门ID（年月+部门维度，null=用户所属部门）
     * @return Map包含 preparationId/sheetNo/status/editable/quotas/departmentId/departmentName
     */
    Map<String, Object> getCategoryQuotas(Integer budgetYear, Integer budgetMonth, Long departmentId);

    /**
     * 老板保存大类预算额度（粗粒度，只管大类不管科目，T5.14 ①）
     * 当期无编制单则新建草稿单；已有草稿/驳回单则复用更新；其余状态拦截
     * @param bo 大类额度保存对象
     * @return 预算编制单ID
     */
    Long saveCategoryQuotas(BudgetCategoryQuotaBo bo);

    /**
     * 大类额度按比例拆分到预算科目生成初稿（T5.14 ②）
     * 拆分依据优先级：近3个月历史实绩比例 → 财务预设默认拆分比例 → 均分；尾差修正保证合计=大类额度。
     * 仅草稿/驳回单可生成；先删后插覆盖旧明细。
     * @param sheetId 预算编制单ID（已录入大类额度）
     * @return Map包含 sheetId/detailCount/details
     */
    Map<String, Object> generateSplitDraft(Long sheetId);
}
