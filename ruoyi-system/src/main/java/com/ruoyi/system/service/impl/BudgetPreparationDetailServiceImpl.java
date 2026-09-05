package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.BudgetPreparation;
import com.ruoyi.system.domain.BudgetPreparationDetail;
import com.ruoyi.system.domain.BudgetSubject;
import com.ruoyi.system.domain.FeeExpenseCategory;
import com.ruoyi.system.domain.bo.BudgetPreparationDetailBo;
import com.ruoyi.system.domain.vo.BudgetCategoryQuotaItemVo;
import com.ruoyi.system.domain.vo.BudgetPreparationDetailVo;
import com.ruoyi.system.mapper.BudgetPreparationDetailMapper;
import com.ruoyi.system.mapper.BudgetPreparationMapper;
import com.ruoyi.system.mapper.BudgetSubjectMapper;
import com.ruoyi.system.mapper.FeeExpenseCategoryMapper;
import com.ruoyi.system.service.IBudgetPreparationDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final BudgetSubjectMapper subjectMapper;
    private final FeeExpenseCategoryMapper feeExpenseCategoryMapper;

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
        // T5.14④ 微调校验：叠加已有明细后不得突破大类限额
        checkCategoryLimit(add, null);
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
        // T5.14④ 微调校验：排除自身后叠加不得突破大类限额
        checkCategoryLimit(update, update.getId());
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
        
        // 先删除该预算单的所有明细，批量保存以本次列表为准：先整体校验大类限额再落库
        validateBatchCategoryLimit(sheetId, detailList);
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

    /**
     * T5.14④ 财务微调校验（单条）：该科目所属大类下已有明细之和 + 本条不得突破大类限额。
     * 未配置大类额度/科目无映射B时不拦截（保持既有行为）。
     *
     * @param detail        待保存明细（含 sheetId/subjectCode/budgetAmount）
     * @param excludeDetailId 统计已有明细时排除的明细ID（更新时排除自身，新增传 null）
     */
    private void checkCategoryLimit(BudgetPreparationDetail detail, Long excludeDetailId) {
        if (detail.getSheetId() == null || StringUtils.isBlank(detail.getSubjectCode())) {
            return;
        }
        Map<String, BigDecimal> quotas = loadCategoryQuotas(detail.getSheetId());
        if (quotas.isEmpty()) {
            return;
        }
        Map<String, String> subjectCategoryMap = loadSubjectCategoryMap(detail.getSubjectCode());
        String categoryCode = subjectCategoryMap.get(detail.getSubjectCode());
        if (categoryCode == null || !quotas.containsKey(categoryCode)) {
            return;
        }
        // 已有明细按大类汇总（排除自身）
        Map<String, BigDecimal> usedByCategory = sumDetailsByCategory(detail.getSheetId(),
            detailMapper2List(detail.getSheetId(), excludeDetailId));
        BigDecimal used = usedByCategory.getOrDefault(categoryCode, BigDecimal.ZERO);
        BigDecimal amount = detail.getBudgetAmount() != null ? detail.getBudgetAmount() : BigDecimal.ZERO;
        BigDecimal quota = quotas.get(categoryCode);
        if (used.add(amount).compareTo(quota) > 0) {
            throw new ServiceException("科目[" + detail.getSubjectCode() + "]所属费用大类["
                + resolveCategoryName(categoryCode) + "]明细合计将达 " + used.add(amount)
                + "，超出大类限额 " + quota + "，微调不得突破");
        }
    }

    /**
     * T5.14④ 财务微调校验（批量）：批量保存以本次列表为准，按大类分组求和不得突破限额。
     */
    private void validateBatchCategoryLimit(Long sheetId, List<BudgetPreparationDetailBo> detailList) {
        Map<String, BigDecimal> quotas = loadCategoryQuotas(sheetId);
        if (quotas.isEmpty()) {
            return;
        }
        List<String> subjectCodes = new ArrayList<>();
        for (BudgetPreparationDetailBo bo : detailList) {
            if (StringUtils.isNotBlank(bo.getSubjectCode())) {
                subjectCodes.add(bo.getSubjectCode());
            }
        }
        if (subjectCodes.isEmpty()) {
            return;
        }
        Map<String, String> subjectCategoryMap = loadSubjectCategoryMap(subjectCodes.toArray(new String[0]));
        Map<String, BigDecimal> sumByCategory = new HashMap<>();
        for (BudgetPreparationDetailBo bo : detailList) {
            String categoryCode = subjectCategoryMap.get(bo.getSubjectCode());
            if (categoryCode == null || !quotas.containsKey(categoryCode)) {
                continue;
            }
            BigDecimal amount = bo.getBudgetAmount() != null ? bo.getBudgetAmount() : BigDecimal.ZERO;
            sumByCategory.merge(categoryCode, amount, BigDecimal::add);
        }
        for (Map.Entry<String, BigDecimal> e : sumByCategory.entrySet()) {
            BigDecimal quota = quotas.get(e.getKey());
            if (e.getValue().compareTo(quota) > 0) {
                throw new ServiceException("费用大类[" + resolveCategoryName(e.getKey())
                    + "]明细合计 " + e.getValue() + " 超出大类限额 " + quota + "，微调不得突破");
            }
        }
    }

    /**
     * 加载编制单大类额度（categoryCode → quota），无额度配置返回空 Map
     */
    private Map<String, BigDecimal> loadCategoryQuotas(Long sheetId) {
        Map<String, BigDecimal> quotas = new HashMap<>();
        BudgetPreparation sheet = preparationMapper.selectById(sheetId);
        if (sheet == null || StringUtils.isBlank(sheet.getCategoryQuotas())) {
            return quotas;
        }
        List<BudgetCategoryQuotaItemVo> items =
            JsonUtils.parseArray(sheet.getCategoryQuotas(), BudgetCategoryQuotaItemVo.class);
        if (items == null) {
            return quotas;
        }
        for (BudgetCategoryQuotaItemVo item : items) {
            if (StringUtils.isNotBlank(item.getCategoryCode()) && item.getAmount() != null
                && item.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                quotas.put(item.getCategoryCode(), item.getAmount());
            }
        }
        return quotas;
    }

    /**
     * 映射B：科目编码 → 费用大类编码（未映射的科目不在结果中）
     */
    private Map<String, String> loadSubjectCategoryMap(String... subjectCodes) {
        Map<String, String> result = new HashMap<>();
        List<BudgetSubject> subjects = subjectMapper.selectList(Wrappers.<BudgetSubject>lambdaQuery()
            .in(BudgetSubject::getSubjectCode, (Object[]) subjectCodes));
        for (BudgetSubject s : subjects) {
            if (StringUtils.isNotBlank(s.getCategoryCode())) {
                result.put(s.getSubjectCode(), s.getCategoryCode());
            }
        }
        return result;
    }

    /**
     * 查询编制单已有明细（可排除指定明细）
     */
    private List<BudgetPreparationDetail> detailMapper2List(Long sheetId, Long excludeDetailId) {
        return baseMapper.selectList(Wrappers.<BudgetPreparationDetail>lambdaQuery()
            .eq(BudgetPreparationDetail::getSheetId, sheetId)
            .ne(excludeDetailId != null, BudgetPreparationDetail::getId, excludeDetailId));
    }

    /**
     * 明细列表按大类汇总预算金额（无映射B的科目不计入）
     */
    private Map<String, BigDecimal> sumDetailsByCategory(Long sheetId, List<BudgetPreparationDetail> details) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (details == null || details.isEmpty()) {
            return result;
        }
        List<String> codes = new ArrayList<>();
        for (BudgetPreparationDetail d : details) {
            if (StringUtils.isNotBlank(d.getSubjectCode())) {
                codes.add(d.getSubjectCode());
            }
        }
        if (codes.isEmpty()) {
            return result;
        }
        Map<String, String> subjectCategoryMap = loadSubjectCategoryMap(codes.toArray(new String[0]));
        for (BudgetPreparationDetail d : details) {
            String categoryCode = subjectCategoryMap.get(d.getSubjectCode());
            if (categoryCode == null) {
                continue;
            }
            BigDecimal amount = d.getBudgetAmount() != null ? d.getBudgetAmount() : BigDecimal.ZERO;
            result.merge(categoryCode, amount, BigDecimal::add);
        }
        return result;
    }

    /**
     * 大类名称解析（主数据缺失时回退编码）
     */
    private String resolveCategoryName(String categoryCode) {
        try {
            FeeExpenseCategory category = feeExpenseCategoryMapper.selectOne(
                Wrappers.<FeeExpenseCategory>lambdaQuery()
                    .eq(FeeExpenseCategory::getCategoryCode, categoryCode)
                    .last("LIMIT 1"));
            return category != null ? category.getCategoryName() : categoryCode;
        } catch (Exception e) {
            return categoryCode;
        }
    }
}
