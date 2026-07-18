package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.ReservedBudget;
import com.ruoyi.system.domain.bo.ReservedBudgetBo;
import com.ruoyi.system.domain.vo.ReservedBudgetVo;
import com.ruoyi.system.mapper.ReservedBudgetMapper;
import com.ruoyi.system.service.IReservedBudgetService;
import com.ruoyi.system.service.ISysMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

/**
 * 预留预算Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservedBudgetServiceImpl implements IReservedBudgetService {

    private final ReservedBudgetMapper reservedBudgetMapper;
    private final ISysMessageService sysMessageService;

    @Override
    public ReservedBudgetVo queryById(Long id) {
        return reservedBudgetMapper.selectVoById(id);
    }

    @Override
    public Page<ReservedBudgetVo> selectPageList(ReservedBudgetBo bo, PageQuery pageQuery) {
        Wrapper<ReservedBudget> wrapper = buildQueryWrapper(bo);
        return reservedBudgetMapper.selectVoPage(pageQuery.build(), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(ReservedBudgetBo bo) {
        ReservedBudget budget = BeanCopyUtils.copy(bo, ReservedBudget.class);
        budget.setAllocatedAmount(BigDecimal.ZERO);
        budget.setCreatorId(LoginHelper.getUserId());
        if (StrUtil.isBlank(budget.getStatus())) {
            budget.setStatus("ACTIVE");
        }
        if (budget.getWarningThreshold() == null) {
            budget.setWarningThreshold(new BigDecimal("80.00"));
        }
        reservedBudgetMapper.insert(budget);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(ReservedBudgetBo bo) {
        ReservedBudget budget = BeanCopyUtils.copy(bo, ReservedBudget.class);
        reservedBudgetMapper.updateById(budget);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(List<Long> ids) {
        return reservedBudgetMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    public ReservedBudget getMyReservedBudget(String budgetPeriod) {
        // 根据角色自动确定层级
        Set<String> roleKeysSet = LoginHelper.getLoginUser().getRolePermission();
        String roleKey = (roleKeysSet != null && !roleKeysSet.isEmpty()) ? roleKeysSet.iterator().next() : "";
        String levelType;
        Long deptId = LoginHelper.getDeptId();

        if ("admin".equals(roleKey) || "headquarters".equals(roleKey)) {
            levelType = "HQ";
            deptId = null;
        } else if ("branch".equals(roleKey)) {
            levelType = "BRANCH";
        } else {
            levelType = "DEPT";
        }

        return findByLevelAndDept(budgetPeriod, levelType, deptId);
    }

    @Override
    public ReservedBudget findByLevelAndDept(String budgetPeriod, String levelType, Long deptId) {
        Wrapper<ReservedBudget> wrapper = Wrappers.<ReservedBudget>lambdaQuery()
            .eq(ReservedBudget::getBudgetPeriod, budgetPeriod)
            .eq(ReservedBudget::getLevelType, levelType)
            .eq(deptId != null, ReservedBudget::getDeptId, deptId)
            .eq(ReservedBudget::getStatus, "ACTIVE")
            .last("LIMIT 1");
        return reservedBudgetMapper.selectOne(wrapper);
    }

    @Override
    public void checkWarning(Long reservedId) {
        ReservedBudget reserved = reservedBudgetMapper.selectById(reservedId);
        if (reserved == null) {
            return;
        }

        BigDecimal total = reserved.getTotalAmount();
        BigDecimal allocated = reserved.getAllocatedAmount();
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        // 计算使用比例
        BigDecimal usageRatio = allocated.multiply(new BigDecimal("100"))
            .divide(total, 2, RoundingMode.HALF_UP);

        BigDecimal threshold = reserved.getWarningThreshold() != null
            ? reserved.getWarningThreshold() : new BigDecimal("80.00");

        if (usageRatio.compareTo(threshold) >= 0) {
            // 发送预警通知给编制人
            Long notifyUserId = reserved.getCreatorId();
            if (notifyUserId != null) {
                sysMessageService.sendMessage(notifyUserId, "预留预算预警",
                    "预留预算（" + reserved.getBudgetPeriod() + " - " + reserved.getLevelType()
                        + "）使用比例已达 " + usageRatio + "%，超过预警阈值 " + threshold + "%，请及时关注。",
                    "SYSTEM_WARNING", "RESERVED_BUDGET", reservedId);
            }
        }
    }

    private Wrapper<ReservedBudget> buildQueryWrapper(ReservedBudgetBo bo) {
        return Wrappers.<ReservedBudget>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getBudgetPeriod()), ReservedBudget::getBudgetPeriod, bo.getBudgetPeriod())
            .eq(StrUtil.isNotBlank(bo.getLevelType()), ReservedBudget::getLevelType, bo.getLevelType())
            .eq(bo.getDeptId() != null, ReservedBudget::getDeptId, bo.getDeptId())
            .eq(StrUtil.isNotBlank(bo.getStatus()), ReservedBudget::getStatus, bo.getStatus())
            .orderByDesc(ReservedBudget::getCreateTime);
    }
}
