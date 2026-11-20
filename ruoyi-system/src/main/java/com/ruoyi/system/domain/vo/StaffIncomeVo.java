package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 业务员收入汇总视图对象（T15.18，micro:staff:income）
 * <p>本周/本月应发金额 = 本人被指派且已完成(COMPLETED)订单金额汇总（复用 {@code IStaffSettlementService.preview} 口径，
 * staffUserId 强制取当前登录人，仅见本人收入）；★V1 只记账不打款，金额为「应发」非「已到账」。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Data
public class StaffIncomeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 本周完成单量 */
    private Integer weekOrders;

    /** 本周应发金额 */
    private BigDecimal weekAmount;

    /** 本月完成单量 */
    private Integer monthOrders;

    /** 本月应发金额 */
    private BigDecimal monthAmount;
}
