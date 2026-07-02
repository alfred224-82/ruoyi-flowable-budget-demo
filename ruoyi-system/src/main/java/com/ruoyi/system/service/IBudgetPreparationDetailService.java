package com.ruoyi.system.service;

import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.domain.vo.BudgetPreparationDetailVo;
import com.ruoyi.system.domain.bo.BudgetPreparationDetailBo;

import java.util.List;

/**
 * 预算编制明细表Service接口
 *
 * @author ruoyi
 * @date 2026-06-19
 */
public interface IBudgetPreparationDetailService {

    /**
     * 查询预算编制明细
     */
    BudgetPreparationDetailVo queryById(Long id);

    /**
     * 根据预算单ID查询明细列表
     */
    List<BudgetPreparationDetailVo> queryListBySheetId(Long sheetId);

    /**
     * 新增预算编制明细
     */
    Boolean insertByBo(BudgetPreparationDetailBo bo);

    /**
     * 修改预算编制明细
     */
    Boolean updateByBo(BudgetPreparationDetailBo bo);

    /**
     * 批量保存预算编制明细
     */
    Boolean batchSave(List<BudgetPreparationDetailBo> detailList);

    /**
     * 删除预算编制明细
     */
    Boolean deleteById(Long id);

    /**
     * 根据预算单ID删除所有明细
     */
    Boolean deleteBySheetId(Long sheetId);
}
