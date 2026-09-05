package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.SysVirtualAccount;
import com.ruoyi.system.domain.vo.SysVirtualAccountVo;
import com.ruoyi.system.mapper.SysVirtualAccountMapper;
import com.ruoyi.system.service.IVirtualAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.function.Consumer;

/**
 * 租户虚拟账户服务实现（T14.3）
 * <p>sys_virtual_account 仅含 create_time/update_time + version，故实体不继承 BaseEntity，
 * tenantId 由服务层显式写入（平台超管跨租户操作时多租户插件不注入）。
 * <p>余额变更统一走 {@link #mutate} 乐观锁重试，保证并发下不覆盖。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirtualAccountServiceImpl implements IVirtualAccountService {

    /** 乐观锁冲突最大重试次数 */
    private static final int MAX_RETRY = 5;

    private final SysVirtualAccountMapper accountMapper;

    @Override
    public SysVirtualAccountVo getOrCreate(Long tenantId) {
        return BeanCopyUtils.copy(loadOrInit(tenantId), SysVirtualAccountVo.class);
    }

    @Override
    public SysVirtualAccountVo queryByTenant(Long tenantId) {
        SysVirtualAccount acc = selectByTenant(tenantId);
        return acc == null ? null : BeanCopyUtils.copy(acc, SysVirtualAccountVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void credit(Long tenantId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            // 试用 0 元订单等无需入账
            return;
        }
        mutate(tenantId, acc -> {
            acc.setBalance(nvl(acc.getBalance()).add(amount));
            acc.setTotalIncome(nvl(acc.getTotalIncome()).add(amount));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void freeze(Long tenantId, BigDecimal amount) {
        requirePositive(amount);
        mutate(tenantId, acc -> {
            if (nvl(acc.getBalance()).compareTo(amount) < 0) {
                throw new ServiceException("可用余额不足，无法冻结 " + amount);
            }
            acc.setBalance(nvl(acc.getBalance()).subtract(amount));
            acc.setFrozenBalance(nvl(acc.getFrozenBalance()).add(amount));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfreeze(Long tenantId, BigDecimal amount) {
        requirePositive(amount);
        mutate(tenantId, acc -> {
            acc.setFrozenBalance(nvl(acc.getFrozenBalance()).subtract(amount));
            acc.setBalance(nvl(acc.getBalance()).add(amount));
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long tenantId, BigDecimal amount) {
        requirePositive(amount);
        mutate(tenantId, acc -> {
            acc.setFrozenBalance(nvl(acc.getFrozenBalance()).subtract(amount));
            acc.setTotalWithdrawn(nvl(acc.getTotalWithdrawn()).add(amount));
        });
    }

    /**
     * 乐观锁重试变更：每次重载最新账户（含 version），应用变更后 updateById；
     * 若影响行数为 0（版本冲突）则重试，超过上限抛异常。
     */
    private void mutate(Long tenantId, Consumer<SysVirtualAccount> mutator) {
        for (int i = 0; i < MAX_RETRY; i++) {
            SysVirtualAccount acc = loadOrInit(tenantId);
            mutator.accept(acc);
            if (accountMapper.updateById(acc) > 0) {
                return;
            }
            log.warn("[VirtualAccount] 乐观锁冲突重试 tenantId={} 第{}次", tenantId, i + 1);
        }
        throw new ServiceException("虚拟账户更新冲突，请稍后重试");
    }

    private SysVirtualAccount loadOrInit(Long tenantId) {
        SysVirtualAccount acc = selectByTenant(tenantId);
        if (acc != null) {
            return acc;
        }
        acc = new SysVirtualAccount();
        acc.setTenantId(tenantId);
        acc.setBalance(BigDecimal.ZERO);
        acc.setFrozenBalance(BigDecimal.ZERO);
        acc.setTotalIncome(BigDecimal.ZERO);
        acc.setTotalWithdrawn(BigDecimal.ZERO);
        acc.setStatus("0");
        acc.setVersion(0);
        try {
            accountMapper.insert(acc);
        } catch (DuplicateKeyException e) {
            // 并发初始化：回读已存在账户
            acc = selectByTenant(tenantId);
        }
        return acc;
    }

    private SysVirtualAccount selectByTenant(Long tenantId) {
        return accountMapper.selectOne(Wrappers.<SysVirtualAccount>lambdaQuery()
            .eq(SysVirtualAccount::getTenantId, tenantId)
            .last("limit 1"));
    }

    private void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("金额必须大于 0");
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
