package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeSubsidyStandard;
import com.ruoyi.system.mapper.FeeSubsidyStandardMapper;
import com.ruoyi.system.service.ISubsidyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 补助管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubsidyServiceImpl implements ISubsidyService {

    private final FeeSubsidyStandardMapper subsidyStandardMapper;

    @Override
    public SubsidyCalcResult calculate(String subsidyType, int days, String industryCode) {
        // 1. 查询补助标准（行业优先，回退通用）
        FeeSubsidyStandard standard = getStandard(subsidyType, industryCode);
        if (standard == null) {
            throw new ServiceException("未找到补助类型[" + subsidyType + "]的配置标准");
        }

        // 2. 校验最大天数限制
        boolean exceededMaxDays = false;
        int actualDays = days;
        if (standard.getMaxDays() != null && days > standard.getMaxDays()) {
            exceededMaxDays = true;
            actualDays = standard.getMaxDays();
            log.warn("补助天数{}超过最大限制{}，按最大天数计算", days, standard.getMaxDays());
        }

        // 3. 计算补助金额 = 单位金额 × 天数
        BigDecimal amount = standard.getUnitAmount().multiply(BigDecimal.valueOf(actualDays));

        // 4. 返回结果
        return new SubsidyCalcResult(
            amount,
            standard.getBudgetSubjectCode(),
            "职工福利费",
            standard.getSubsidyName(),
            exceededMaxDays,
            standard.getMaxDays()
        );
    }

    /**
     * 获取补助标准（行业优先，回退通用）
     */
    private FeeSubsidyStandard getStandard(String subsidyType, String industryCode) {
        // 先查行业专属
        if (StrUtil.isNotBlank(industryCode) && !"*".equals(industryCode)) {
            FeeSubsidyStandard standard = subsidyStandardMapper.selectOne(
                Wrappers.<FeeSubsidyStandard>lambdaQuery()
                    .eq(FeeSubsidyStandard::getSubsidyType, subsidyType)
                    .eq(FeeSubsidyStandard::getIndustryCode, industryCode)
                    .eq(FeeSubsidyStandard::getStatus, "ENABLED")
            );
            if (standard != null) {
                return standard;
            }
        }
        // 回退通用配置
        return subsidyStandardMapper.selectOne(
            Wrappers.<FeeSubsidyStandard>lambdaQuery()
                .eq(FeeSubsidyStandard::getSubsidyType, subsidyType)
                .eq(FeeSubsidyStandard::getIndustryCode, "*")
                .eq(FeeSubsidyStandard::getStatus, "ENABLED")
        );
    }
}
