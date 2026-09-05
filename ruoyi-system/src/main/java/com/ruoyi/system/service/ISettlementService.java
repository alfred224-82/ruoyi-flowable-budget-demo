package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.SysSettlementBillVo;

/**
 * 结算对账服务接口（T14.3 按账期生成对账单，金额可追溯到订单）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ISettlementService {

    /**
     * 对账单分页查询
     *
     * @param period   账期 yyyy-MM（可空）
     * @param tenantId 租户ID（可空；平台超管跨租户筛查）
     */
    TableDataInfo<SysSettlementBillVo> queryPageList(String period, Long tenantId, PageQuery pageQuery);

    /**
     * 对账单详情
     */
    SysSettlementBillVo queryById(Long id);

    /**
     * 生成（或刷新）指定租户指定账期的对账单：
     * 汇总该账期已支付订单（笔数/总额）与分账记录（平台服务费/租户应得合计），按 bill_no 幂等 upsert。
     *
     * @param tenantId 租户ID
     * @param period   账期 yyyy-MM
     * @return 对账单视图
     */
    SysSettlementBillVo generate(Long tenantId, String period);
}
