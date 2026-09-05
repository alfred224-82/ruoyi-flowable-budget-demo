package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.FeeCategorySplitRatioVo;
import com.ruoyi.system.domain.vo.FeeExpenseCategoryVo;

import java.util.List;

/**
 * 费用大类主数据Service接口（T5.13）
 * admin 维护费用大类；大类额度为预算编制与报销管控的上层限额
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeExpenseCategoryService {

    /**
     * 查询费用大类列表（当前租户，按排序号升序）
     */
    List<FeeExpenseCategoryVo> queryList();

    /**
     * 查询启用的费用大类（编制/报销下拉用）
     */
    List<FeeExpenseCategoryVo> queryActiveList();

    /**
     * 查询费用大类详情
     */
    FeeExpenseCategoryVo queryById(Long id);

    /**
     * 新增/修改费用大类（编码租户内唯一；内置大类编码不可改）
     *
     * @return 大类ID
     */
    Long saveCategory(FeeExpenseCategoryVo vo);

    /**
     * 批量删除费用大类（内置不可删；级联删除默认拆分比例）
     */
    int deleteCategory(List<Long> ids);

    /**
     * 查询大类的默认拆分比例列表
     */
    List<FeeCategorySplitRatioVo> listRatios(Long categoryId);

    /**
     * 保存大类的默认拆分比例（整体替换，比例合计必须=100）
     */
    void saveRatios(Long categoryId, List<FeeCategorySplitRatioVo> ratios);
}
