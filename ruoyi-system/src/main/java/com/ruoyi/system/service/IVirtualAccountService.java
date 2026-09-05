package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.SysVirtualAccountVo;

import java.math.BigDecimal;

/**
 * 租户虚拟账户服务接口（T14.3 平台代收模式分账入账/提现）
 * <p>每租户唯一账户；余额变更走乐观锁（version）+ 有限重试防并发覆盖。
 * <p>资金流：分账入账 credit → 提现申请 freeze（冻结）→ 审核通过 deduct（扣减）/ 驳回 unfreeze（解冻）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IVirtualAccountService {

    /**
     * 查询租户虚拟账户，不存在则初始化（余额 0）
     */
    SysVirtualAccountVo getOrCreate(Long tenantId);

    /**
     * 查询租户虚拟账户，不存在返回 null
     */
    SysVirtualAccountVo queryByTenant(Long tenantId);

    /**
     * 入账：可用余额 + / 累计入账 +（金额 &lt;=0 忽略，如试用 0 元订单）
     */
    void credit(Long tenantId, BigDecimal amount);

    /**
     * 冻结：可用余额 - / 冻结余额 +（提现申请；余额不足抛异常）
     */
    void freeze(Long tenantId, BigDecimal amount);

    /**
     * 解冻：冻结余额 - / 可用余额 +（提现驳回）
     */
    void unfreeze(Long tenantId, BigDecimal amount);

    /**
     * 扣减：冻结余额 - / 累计提现 +（提现审核通过并打款）
     */
    void deduct(Long tenantId, BigDecimal amount);
}
