package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FeeIncomeConfirmationBo;
import com.ruoyi.system.domain.vo.FeeIncomeConfirmationVo;

/**
 * 收入确认Service接口（T5.4）
 * 三态流转：项目经理创建→待核验；财务核验通过/驳回；收款单联动生成已核验
 *
 * @author ruoyi
 * @date 2026-08-29
 */
public interface IFeeIncomeConfirmationService {

    /**
     * 分页查询（数据范围：项目经理仅本人参与项目）
     */
    TableDataInfo<FeeIncomeConfirmationVo> queryPageList(FeeIncomeConfirmationBo bo, PageQuery pageQuery);

    /**
     * 详情
     */
    FeeIncomeConfirmationVo queryById(Long id);

    /**
     * 项目经理创建收入确认（状态=待核验），同时增加项目待核验收入
     */
    Long insertConfirmation(FeeIncomeConfirmationBo bo);

    /**
     * 财务核验通过 → 待核验收入转已核验收入
     */
    int verifyConfirmation(Long id);

    /**
     * 财务核验驳回 → 冲销项目待核验收入
     */
    int rejectConfirmation(Long id, String reason);
}
