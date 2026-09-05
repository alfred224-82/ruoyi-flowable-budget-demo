package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.BillingOrderCreateBo;
import com.ruoyi.system.domain.vo.SysBillingOrderVo;

/**
 * 计费订单服务接口（T14.1 下单/开通/续费）
 * <p>下单据套餐换算金额与到期时间，支付成功后回写 sys_tenant（license_type/edition/expire_date/status）
 * 并触发分账（{@link IProfitSharingService#shareForOrder}）。
 * <p>【严禁自动扣款】：试用(need_pay=0)直接开通；付费套餐需显式确认收款（markPaid），系统绝不主动扣款。
 * <p>首月特惠(first_only=1)同租户仅可享受一次。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IBillingOrderService {

    /**
     * 订单分页查询
     *
     * @param orderNo   订单号（可空，模糊）
     * @param payStatus 支付状态（可空）
     * @param tenantId  租户ID（可空；平台超管跨租户筛查）
     */
    TableDataInfo<SysBillingOrderVo> queryPageList(String orderNo, String payStatus, Long tenantId, PageQuery pageQuery);

    /**
     * 订单详情（填充套餐名称）
     */
    SysBillingOrderVo queryById(Long id);

    /**
     * 下单（平台代下单/续费）：校验套餐与首月特惠资格，生成订单；
     * 试用或离线/手动收款(offline/manual)即时开通并回写租户 + 分账，其余渠道置 PENDING 待确认收款。
     *
     * @return 订单视图
     */
    SysBillingOrderVo createOrder(BillingOrderCreateBo bo);

    /**
     * 确认收款（离线/第三方回调）：PENDING → PAID，回写租户到期并触发分账
     *
     * @param id            订单ID
     * @param transactionId 第三方流水号（可空）
     * @return 影响行数
     */
    int markPaid(Long id, String transactionId);

    /**
     * 取消未支付订单：PENDING → CANCELLED
     *
     * @return 影响行数
     */
    int cancel(Long id);
}
