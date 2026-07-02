package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.domain.bo.BudgetPreparationDetailBo;
import com.ruoyi.system.domain.vo.BudgetPreparationDetailVo;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import com.ruoyi.system.service.IBudgetPreparationDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * 预算编制明细表Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-19
 */
@RequiredArgsConstructor
@Service
public class BudgetPreparationDetailServiceImpl implements IBudgetPreparationDetailService {

    private final BudgetPreparationDetailMapper baseMapper;
    private final BudgetPreparationMapper preparationMapper;

    /**
     * 查询预算编制明细
     */
    @Override
    public BudgetPreparationDetailVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    /**
     * 根据预算单ID查询明细列表
     */
    @Override
    public List<BudgetPreparationDetailVo> queryListBySheetId(Long sheetId) {
        LambdaQueryWrapper<BudgetPreparationDetail> lqw = Wrappers.lambdaQuery();
        lqw.eq(BudgetPreparationDetail::getSheetId, sheetId);
        lqw.orderByAsc(BudgetPreparationDetail::getSortOrder);
        return baseMapper.selectVoList(lqw);
    }

    /**
     * 新增预算编制明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(BudgetPreparationDetailBo bo) {
        BudgetPreparationDetail add = BeanUtil.toBean(bo, BudgetPreparationDetail.class);
        
        // 自动计算差异金额和差异率
        calculateVariance(add);
        
        validEntityBeforeSave(add);
        return baseMapper.insert(add) > 0;
    }

    /**
     * 修改预算编制明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(BudgetPreparationDetailBo bo) {
        BudgetPreparationDetail update = BeanUtil.toBean(bo, BudgetPreparationDetail.class);
        
        // 自动计算差异金额和差异率
        calculateVariance(update);
        
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 批量保存预算编制明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchSave(List<BudgetPreparationDetailBo> detailList) {
        if (detailList == null || detailList.isEmpty()) {
            return true;
        }
        
        // 获取第一个明细的sheetId，用于删除旧数据
        Long sheetId = detailList.get(0).getSheetId();
        if (sheetId == null) {
            throw new ServiceException("预算单ID不能为空");
        }
        
        // 先删除该预算单的所有明细
        deleteBySheetId(sheetId);
        
        // 批量插入新明细
        BigDecimal totalBudget = BigDecimal.ZERO;
        for (BudgetPreparationDetailBo bo : detailList) {
            BudgetPreparationDetail detail = BeanUtil.toBean(bo, BudgetPreparationDetail.class);
            
            // 自动计算差异金额和差异率
            calculateVariance(detail);
            
            validEntityBeforeSave(detail);
            baseMapper.insert(detail);
            
            // 累加预算总额
            if (detail.getBudgetAmount() != null) {
                totalBudget = totalBudget.add(detail.getBudgetAmount());
            }
        }
        
        // 自动更新主表的预算总额
        BudgetPreparation sheet = preparationMapper.selectById(sheetId);
        if (sheet != null) {
            sheet.setTotalBudget(totalBudget);
            preparationMapper.updateById(sheet);
        }
        
        return true;
    }

    /**
     * 删除预算编制明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteById(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    /**
     * 根据预算单ID删除所有明细
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteBySheetId(Long sheetId) {
        LambdaQueryWrapper<BudgetPreparationDetail> lqw = Wrappers.lambdaQuery();
        lqw.eq(BudgetPreparationDetail::getSheetId, sheetId);
        return baseMapper.delete(lqw) > 0;
    }

    /**
     * 计算差异金额和差异率
     */
    private void calculateVariance(BudgetPreparationDetail detail) {
        BigDecimal budgetAmount = detail.getBudgetAmount() != null ? detail.getBudgetAmount() : BigDecimal.ZERO;
        BigDecimal actualAmount = detail.getActualAmount() != null ? detail.getActualAmount() : BigDecimal.ZERO;
        
        // 差异金额 = 预算金额 - 实际金额
        BigDecimal varianceAmount = budgetAmount.subtract(actualAmount);
        detail.setVarianceAmount(varianceAmount);
        
        // 差异率 = (差异金额 / 预算金额) * 100%
        BigDecimal varianceRate = BigDecimal.ZERO;
        if (budgetAmount.compareTo(BigDecimal.ZERO) != 0) {
            varianceRate = varianceAmount.divide(budgetAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        detail.setVarianceRate(varianceRate);
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(BudgetPreparationDetail entity) {
        // TODO 做一些数据校验，如科目是否存在等
    }
}
