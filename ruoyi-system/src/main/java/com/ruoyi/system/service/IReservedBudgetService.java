package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.ReservedBudget;
import com.ruoyi.system.domain.bo.ReservedBudgetBo;
import com.ruoyi.system.domain.vo.ReservedBudgetVo;

import java.util.List;

/**
 * 预留预算Service接口
 *
 * @author ruoyi
 * @date 2026-07-15
 */
public interface IReservedBudgetService {

    /**
     * 查询预留预算详情
     */
    ReservedBudgetVo queryById(Long id);

    /**
     * 分页查询预留预算列表
     */
    Page<ReservedBudgetVo> selectPageList(ReservedBudgetBo bo, PageQuery pageQuery);

    /**
     * 新增预留预算
     */
    Boolean insertByBo(ReservedBudgetBo bo);

    /**
     * 修改预留预算
     */
    Boolean updateByBo(ReservedBudgetBo bo);

    /**
     * 删除预留预算
     */
    Boolean deleteWithValidByIds(List<Long> ids);

    /**
     * 获取当前用户的预留预算（根据角色自动确定层级）
     */
    ReservedBudget getMyReservedBudget(String budgetPeriod);

    /**
     * 根据层级和部门查询预留预算
     */
    ReservedBudget findByLevelAndDept(String budgetPeriod, String levelType, Long deptId);

    /**
     * 预留预算预警检查（使用比例 > 阈值时发预警通知）
     */
    void checkWarning(Long reservedId);
}
