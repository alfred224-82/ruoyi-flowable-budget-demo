package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.bo.BudgetPreparationBo;
import com.ruoyi.system.domain.vo.BudgetPreparationVo;

import java.util.List;
import java.util.Map;

/**
 * 部门级预算Service接口（T6.1 / FEAT_DEPT_BUDGET）
 * 启用 department_id 双粒度：部门预算汇总 → 财务平衡 → 老板确认锁定；
 * 报销按提交人部门扣对应部门额度（BudgetCheckService 已按部门隔离）
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IDeptBudgetService {

    /**
     * 部门预算编制单分页查询（仅 department_id 非空的部门级单据）
     */
    Page<BudgetPreparationVo> deptPageList(Long departmentId, Integer budgetYear, Integer budgetMonth,
                                           String status, PageQuery pageQuery);

    /**
     * 创建部门预算编制单（部门级编制闭环入口，复用公司级编制流程）
     *
     * @param bo 编制业务对象（departmentId 必填）
     */
    Boolean createDeptBudget(BudgetPreparationBo bo);

    /**
     * 部门科目额度汇总（部门预算查询 3291：编制额/已用/剩余）
     *
     * @return 行：subjectCode/subjectName/totalBudget/usedAmount/remaining
     */
    List<Map<String, Object>> deptQuotaSummary(Long departmentId, Integer budgetYear, Integer budgetMonth);
}
