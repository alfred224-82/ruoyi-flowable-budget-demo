package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.ProfitSharingRuleBo;
import com.ruoyi.system.domain.vo.SysProfitSharingRecordVo;
import com.ruoyi.system.domain.vo.SysProfitSharingRuleVo;

import java.math.BigDecimal;

/**
 * 分账服务接口（T14.3 平台服务费 + 租户应得，可配置）
 * <p>核心不变式：platform_amount + tenant_amount = order_amount（分账合计=订单金额）。
 * <p>租户应得自动入账其虚拟账户（{@link IVirtualAccountService#credit}）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IProfitSharingService {

    /**
     * 查询租户分账规则，无则返回默认（平台 0、租户全额）
     */
    SysProfitSharingRuleVo getRule(Long tenantId);

    /**
     * 保存（新增/更新）租户分账规则；tenant_rate = 1 - platform_rate
     *
     * @return 影响行数
     */
    int saveRule(Long tenantId, ProfitSharingRuleBo bo);

    /**
     * 对一笔已支付订单执行分账：按规则拆分平台/租户金额，租户应得入账虚拟账户并写分账记录
     *
     * @param orderNo     订单号
     * @param tenantId    租户ID
     * @param orderAmount 订单金额
     * @return 分账记录视图
     */
    SysProfitSharingRecordVo shareForOrder(String orderNo, Long tenantId, BigDecimal orderAmount);

    /**
     * 分账记录分页查询
     *
     * @param orderNo  订单号（可空）
     * @param tenantId 租户ID（可空；平台超管跨租户筛查）
     */
    TableDataInfo<SysProfitSharingRecordVo> queryRecordPageList(String orderNo, Long tenantId, PageQuery pageQuery);
}
