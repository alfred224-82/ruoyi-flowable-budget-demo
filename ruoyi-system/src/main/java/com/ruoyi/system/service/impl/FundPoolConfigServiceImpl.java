package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.domain.FeeFundPoolConfig;
import com.ruoyi.system.domain.ReservedBudget;
import com.ruoyi.system.domain.bo.FundPoolConfigBo;
import com.ruoyi.system.domain.vo.FundPoolConfigVo;
import com.ruoyi.system.mapper.FeeFundPoolConfigMapper;
import com.ruoyi.system.mapper.ReservedBudgetMapper;
import com.ruoyi.system.service.IFundPoolConfigService;
import com.ruoyi.system.service.IReservedBudgetService;
import com.ruoyi.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金池配置服务实现
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FundPoolConfigServiceImpl implements IFundPoolConfigService {

    private final FeeFundPoolConfigMapper fundPoolConfigMapper;
    private final ReservedBudgetMapper reservedBudgetMapper;
    private final IReservedBudgetService reservedBudgetService;
    private final ISysRoleService roleService;

    /** 系统默认额度 */
    private static final BigDecimal DEFAULT_DEPT_AMOUNT = new BigDecimal("100000.00");
    private static final BigDecimal DEFAULT_BRANCH_AMOUNT = new BigDecimal("500000.00");
    private static final BigDecimal DEFAULT_HQ_AMOUNT = new BigDecimal("1000000.00");
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("80.00");

    @Override
    public List<FundPoolConfigVo> listAll() {
        List<FeeFundPoolConfig> configs = fundPoolConfigMapper.selectList(
            Wrappers.<FeeFundPoolConfig>lambdaQuery()
                .orderByAsc(FeeFundPoolConfig::getId));
        List<FundPoolConfigVo> result = new ArrayList<>();
        for (FeeFundPoolConfig config : configs) {
            FundPoolConfigVo vo = new FundPoolConfigVo();
            vo.setId(config.getId());
            vo.setLevelType(config.getLevelType());
            vo.setLevelName(config.getLevelName());
            vo.setRoleId(config.getRoleId());
            vo.setRoleKey(config.getRoleKey());
            vo.setRoleName(config.getRoleName());
            vo.setInitialAmount(config.getInitialAmount());
            vo.setWarningThreshold(config.getWarningThreshold());
            vo.setStatus(config.getStatus());
            vo.setCreateTime(config.getCreateTime());
            vo.setUpdateTime(config.getUpdateTime());
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveConfigs(List<FundPoolConfigBo> boList) {
        for (FundPoolConfigBo bo : boList) {
            FeeFundPoolConfig config;
            if (bo.getId() != null) {
                config = fundPoolConfigMapper.selectById(bo.getId());
                if (config == null) {
                    config = new FeeFundPoolConfig();
                }
            } else {
                // 按levelType查找
                config = fundPoolConfigMapper.selectOne(
                    Wrappers.<FeeFundPoolConfig>lambdaQuery()
                        .eq(FeeFundPoolConfig::getLevelType, bo.getLevelType()));
                if (config == null) {
                    config = new FeeFundPoolConfig();
                }
            }

            config.setLevelType(bo.getLevelType());
            config.setLevelName(bo.getLevelName());
            config.setRoleId(bo.getRoleId());
            config.setInitialAmount(bo.getInitialAmount());
            config.setWarningThreshold(bo.getWarningThreshold());
            config.setStatus(bo.getStatus() != null ? bo.getStatus() : "ENABLED");

            // 冗余填充角色信息
            if (bo.getRoleId() != null) {
                SysRole role = roleService.selectRoleById(bo.getRoleId());
                if (role != null) {
                    config.setRoleKey(role.getRoleKey());
                    config.setRoleName(role.getRoleName());
                }
            } else {
                config.setRoleKey(bo.getRoleKey());
                config.setRoleName(bo.getRoleName());
            }

            if (config.getId() == null) {
                fundPoolConfigMapper.insert(config);
            } else {
                fundPoolConfigMapper.updateById(config);
            }
        }
        log.info("保存资金池配置成功，共{}条", boList.size());
        return true;
    }

    @Override
    public BigDecimal getInitialAmount(String levelType) {
        FeeFundPoolConfig config = fundPoolConfigMapper.selectOne(
            Wrappers.<FeeFundPoolConfig>lambdaQuery()
                .eq(FeeFundPoolConfig::getLevelType, levelType)
                .eq(FeeFundPoolConfig::getStatus, "ENABLED")
                .last("LIMIT 1"));
        if (config != null) {
            return config.getInitialAmount();
        }
        // 兜底默认值
        switch (levelType) {
            case "DEPT":
                return DEFAULT_DEPT_AMOUNT;
            case "BRANCH":
                return DEFAULT_BRANCH_AMOUNT;
            case "HQ":
                return DEFAULT_HQ_AMOUNT;
            default:
                return DEFAULT_DEPT_AMOUNT;
        }
    }

    @Override
    public BigDecimal getWarningThreshold(String levelType) {
        FeeFundPoolConfig config = fundPoolConfigMapper.selectOne(
            Wrappers.<FeeFundPoolConfig>lambdaQuery()
                .eq(FeeFundPoolConfig::getLevelType, levelType)
                .eq(FeeFundPoolConfig::getStatus, "ENABLED")
                .last("LIMIT 1"));
        if (config != null) {
            return config.getWarningThreshold();
        }
        return DEFAULT_THRESHOLD;
    }

    @Override
    public String getLevelTypeByRoleKey(String roleKey) {
        FeeFundPoolConfig config = fundPoolConfigMapper.selectOne(
            Wrappers.<FeeFundPoolConfig>lambdaQuery()
                .eq(FeeFundPoolConfig::getRoleKey, roleKey)
                .eq(FeeFundPoolConfig::getStatus, "ENABLED")
                .last("LIMIT 1"));
        if (config != null) {
            return config.getLevelType();
        }
        // 兜底：按原有硬编码逻辑
        if ("admin".equals(roleKey) || "headquarters".equals(roleKey)) {
            return "HQ";
        } else if ("branch".equals(roleKey)) {
            return "BRANCH";
        }
        return "DEPT";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long autoCreateFundPool(String period, String levelType, Long deptId, String deptName) {
        // 1. 检查是否已存在
        ReservedBudget existing = reservedBudgetService.findByLevelAndDept(period, levelType, deptId);
        if (existing != null) {
            return existing.getId();
        }

        // 2. 从配置表读取初始额度和预警阈值
        BigDecimal initialAmount = getInitialAmount(levelType);
        BigDecimal threshold = getWarningThreshold(levelType);

        // 3. 创建 ReservedBudget 记录
        ReservedBudget budget = new ReservedBudget();
        budget.setBudgetPeriod(period);
        budget.setLevelType(levelType);
        budget.setDeptId(deptId);
        budget.setDeptName(deptName);
        budget.setTotalAmount(initialAmount);
        budget.setAllocatedAmount(BigDecimal.ZERO);
        budget.setWarningThreshold(threshold);
        budget.setStatus("ACTIVE");
        budget.setRemark("系统初始化配置自动创建");
        reservedBudgetMapper.insert(budget);

        log.info("自动创建资金池：period={}, levelType={}, deptId={}, amount={}, id={}",
            period, levelType, deptId, initialAmount, budget.getId());
        return budget.getId();
    }
}
