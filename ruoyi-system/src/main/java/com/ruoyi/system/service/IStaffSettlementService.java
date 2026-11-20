package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.StaffSettlementBo;
import com.ruoyi.system.domain.vo.SettlementOrderDetailVo;
import com.ruoyi.system.domain.vo.StaffSettlementVo;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 人员结算服务接口（T15.5/T15.13，老板 PC，★V1 只记账不打款）
 * <p>流程：① 选择员工+周期→自动汇总单量→算应发金额（{@link #preview}）；② 生成「待发放」结算单（{@link #generate}）；
 * ③ 老板线下转账后「标记已线下发放」→改状态 + 记 cash_flow 支出流水（{@link #markPaidOffline}）；④ 结算历史（{@link #queryPageList}）。
 * <p>★零支付合规：全流程无任何支付/转账/代付接口调用，支出流水为纯记账口径。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
public interface IStaffSettlementService {

    /**
     * 分页查询结算单（结算历史，含「已发/待发」状态）
     */
    TableDataInfo<StaffSettlementVo> queryPageList(StaffSettlementBo bo, PageQuery pageQuery);

    /**
     * 结算单详情（micro:settle:detail）
     */
    StaffSettlementVo queryById(Long id);

    /**
     * ① 预览：按员工 + 周期汇总已完成订单单量与应发金额（不落库，供老板确认后生成）
     */
    StaffSettlementVo preview(Long staffUserId, Date periodStart, Date periodEnd);

    /**
     * ② 生成「待发放」结算单（落库，自动写入汇总的单量/金额/结算单号，pay_status=PENDING）
     *
     * @return 结算单ID
     */
    Long generate(StaffSettlementBo bo);

    /**
     * ③ 标记「已线下发放」：pay_status=PAID_OFFLINE + marked_by/marked_time，并自动写 cash_flow 支出流水（不调支付接口）
     */
    int markPaidOffline(Long id);

    /**
     * 逻辑删除结算单（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * 查询结算单关联的已完成订单明细（含顾客、活动、时长、起止时间）
     *
     * @param settlementId 结算单ID
     * @return 订单明细列表
     */
    List<SettlementOrderDetailVo> listSettlementOrders(Long settlementId);
}
