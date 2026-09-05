package com.ruoyi.system.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeAsset;
import com.ruoyi.system.domain.FeeAssetDepreciation;
import com.ruoyi.system.domain.FeeDepreciationRule;
import com.ruoyi.system.domain.vo.FeeAssetDepreciationVo;
import com.ruoyi.system.domain.vo.FeeAssetVo;
import com.ruoyi.system.domain.vo.FeeDepreciationRuleVo;
import com.ruoyi.system.mapper.FeeAssetDepreciationMapper;
import com.ruoyi.system.mapper.FeeAssetMapper;
import com.ruoyi.system.mapper.FeeDepreciationRuleMapper;
import com.ruoyi.system.service.IFeeAssetService;
import com.ruoyi.system.service.ITenantFeatureGuard;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 资产折旧Service实现（T5.9 / ADR-026）
 * 直线法：月折旧额 = (原值 - 残值) / 使用年限月；项目归属资产折旧记入项目分摊
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeAssetServiceImpl implements IFeeAssetService {

    private final FeeAssetMapper assetMapper;
    private final FeeAssetDepreciationMapper depreciationMapper;
    private final FeeDepreciationRuleMapper ruleMapper;
    private final ITenantFeatureGuard tenantFeatureGuard;

    /**
     * 隐形成本开关编码（T5.9 下移高级版，T6.8）
     */
    private static final String FEATURE_HIDDEN_COST = "FEAT_HIDDEN_COST";

    @Override
    public TableDataInfo<FeeAssetVo> queryPageList(Long projectId, String status, PageQuery pageQuery) {
        Page<FeeAssetVo> page = assetMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeAsset>lambdaQuery()
                .eq(projectId != null, FeeAsset::getProjectId, projectId)
                .eq(StrUtil.isNotBlank(status), FeeAsset::getStatus, status)
                .orderByDesc(FeeAsset::getCreateTime));
        return TableDataInfo.build(page);
    }

    @Override
    public Long saveAsset(FeeAsset asset) {
        if (StrUtil.isBlank(asset.getAssetName()) || asset.getOriginalValue() == null) {
            throw new ServiceException("资产名称与原值不能为空");
        }
        // 折旧参数缺省时套用折旧规则
        if (asset.getLifeMonths() == null || asset.getResidualValue() == null) {
            applyDepreciationRule(asset);
        }
        if (asset.getLifeMonths() == null || asset.getLifeMonths() <= 0) {
            throw new ServiceException("使用年限必须大于0（可配置折旧规则自动带入）");
        }
        BigDecimal residual = zero(asset.getResidualValue());
        // 月折旧额 = (原值 - 残值) / 使用年限月
        BigDecimal monthly = asset.getOriginalValue().subtract(residual)
            .divide(BigDecimal.valueOf(asset.getLifeMonths()), 2, RoundingMode.HALF_UP);
        if (monthly.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("残值不得大于原值");
        }
        asset.setMonthlyDepreciation(monthly);
        asset.setNetValue(asset.getOriginalValue());
        asset.setDepreciatedMonths(zeroInt(asset.getDepreciatedMonths()));
        if (StrUtil.isBlank(asset.getStatus())) {
            asset.setStatus("ACTIVE");
        }
        if (StrUtil.isBlank(asset.getAttributionType())) {
            asset.setAttributionType(asset.getProjectId() != null ? "PROJECT" : "COMPANY");
        }
        if (asset.getId() != null) {
            assetMapper.updateById(asset);
            return asset.getId();
        }
        assetMapper.insert(asset);
        return asset.getId();
    }

    @Override
    public int scrapAsset(Long id) {
        FeeAsset asset = assetMapper.selectById(id);
        if (asset == null) {
            throw new ServiceException("资产不存在");
        }
        FeeAsset update = new FeeAsset();
        update.setId(id);
        update.setStatus("SCRAPPED");
        return assetMapper.updateById(update);
    }

    @Override
    public List<FeeDepreciationRuleVo> listRules() {
        return ruleMapper.selectVoList(Wrappers.<FeeDepreciationRule>lambdaQuery()
            .orderByAsc(FeeDepreciationRule::getId));
    }

    @Override
    public Long saveRule(FeeDepreciationRule rule) {
        if (StrUtil.isBlank(rule.getCategory()) || rule.getLifeMonths() == null) {
            throw new ServiceException("资产分类与使用年限不能为空");
        }
        FeeDepreciationRule exist = ruleMapper.selectOne(Wrappers.<FeeDepreciationRule>lambdaQuery()
            .eq(FeeDepreciationRule::getCategory, rule.getCategory())
            .ne(rule.getId() != null, FeeDepreciationRule::getId, rule.getId())
            .last("LIMIT 1"));
        if (rule.getIsEnabled() == null) {
            rule.setIsEnabled(1);
        }
        if (exist != null) {
            rule.setId(exist.getId());
            ruleMapper.updateById(rule);
            return exist.getId();
        }
        ruleMapper.insert(rule);
        return rule.getId();
    }

    @Override
    public TableDataInfo<FeeAssetDepreciationVo> queryDepreciationPageList(String accrualMonth, Long projectId, PageQuery pageQuery) {
        Page<FeeAssetDepreciationVo> page = depreciationMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeAssetDepreciation>lambdaQuery()
                .eq(StrUtil.isNotBlank(accrualMonth), FeeAssetDepreciation::getAccrualMonth, accrualMonth)
                .eq(projectId != null, FeeAssetDepreciation::getProjectId, projectId)
                .orderByDesc(FeeAssetDepreciation::getAccrualMonth)
                .orderByAsc(FeeAssetDepreciation::getId));
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int accrueDepreciation(String accrualMonth) {
        String month = StrUtil.isNotBlank(accrualMonth) ? accrualMonth
            : DateUtil.format(DateUtil.offsetMonth(new Date(), -1), "yyyy-MM");
        List<FeeAsset> actives = assetMapper.selectList(Wrappers.<FeeAsset>lambdaQuery()
            .eq(FeeAsset::getStatus, "ACTIVE"));
        // T6.8 ③：仅对开通隐形成本(FEAT_HIDDEN_COST)的租户计提，基础版租户跳过（无执行记录）
        Set<Long> enabledTenants = tenantFeatureGuard.enabledTenantIds(FEATURE_HIDDEN_COST);
        int count = 0;
        for (FeeAsset asset : actives) {
            if (!enabledTenants.contains(asset.getTenantId())) {
                continue;
            }
            // 已提足折旧不再计提
            if (zeroInt(asset.getDepreciatedMonths()) >= asset.getLifeMonths()) {
                continue;
            }
            // 幂等：同月同资产仅一条
            Long exist = depreciationMapper.selectCount(Wrappers.<FeeAssetDepreciation>lambdaQuery()
                .eq(FeeAssetDepreciation::getAccrualMonth, month)
                .eq(FeeAssetDepreciation::getAssetId, asset.getId()));
            if (exist != null && exist > 0) {
                continue;
            }
            FeeAssetDepreciation record = new FeeAssetDepreciation();
            record.setTenantId(asset.getTenantId());
            record.setAssetId(asset.getId());
            record.setAccrualMonth(month);
            record.setAmount(zero(asset.getMonthlyDepreciation()));
            // 项目归属资产记入项目分摊（利润拆解按此扣减）
            if ("PROJECT".equals(asset.getAttributionType()) && asset.getProjectId() != null) {
                record.setProjectId(asset.getProjectId());
                record.setShareRatio(BigDecimal.ONE);
            }
            depreciationMapper.insert(record);

            // 更新资产已折旧月数与净值
            FeeAsset update = new FeeAsset();
            update.setId(asset.getId());
            update.setDepreciatedMonths(zeroInt(asset.getDepreciatedMonths()) + 1);
            update.setNetValue(zero(asset.getNetValue()).subtract(record.getAmount()));
            assetMapper.updateById(update);
            count++;
        }
        log.info("资产折旧计提完成, 月份={}, 计提{}条", month, count);
        return count;
    }

    /**
     * 套用折旧规则：带入使用年限与残值
     */
    private void applyDepreciationRule(FeeAsset asset) {
        if (StrUtil.isBlank(asset.getCategory())) {
            return;
        }
        FeeDepreciationRule rule = ruleMapper.selectOne(Wrappers.<FeeDepreciationRule>lambdaQuery()
            .eq(FeeDepreciationRule::getCategory, asset.getCategory())
            .eq(FeeDepreciationRule::getIsEnabled, 1)
            .last("LIMIT 1"));
        if (rule == null) {
            return;
        }
        if (asset.getLifeMonths() == null) {
            asset.setLifeMonths(rule.getLifeMonths());
        }
        if (asset.getResidualValue() == null && rule.getResidualRate() != null) {
            asset.setResidualValue(asset.getOriginalValue().multiply(rule.getResidualRate())
                .setScale(2, RoundingMode.HALF_UP));
        }
    }

    private BigDecimal zero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private int zeroInt(Integer value) {
        return value != null ? value : 0;
    }
}
