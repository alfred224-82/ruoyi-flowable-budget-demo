package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.SysBillingOrder;
import com.ruoyi.system.domain.SysProfitSharingRecord;
import com.ruoyi.system.domain.SysSettlementBill;
import com.ruoyi.system.domain.vo.SysSettlementBillVo;
import com.ruoyi.system.mapper.SysBillingOrderMapper;
import com.ruoyi.system.mapper.SysProfitSharingRecordMapper;
import com.ruoyi.system.mapper.SysSettlementBillMapper;
import com.ruoyi.system.service.ISettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 结算对账服务实现（T14.3）
 * <p>按账期(yyyy-MM)汇总租户已支付订单与分账记录，生成对账单（bill_no = ST{tenantId}-{period} 幂等 upsert）。
 * <p>金额可追溯：grossAmount = Σ订单额；platformFee/tenantAmount = Σ分账记录（platform+tenant=gross）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements ISettlementService {

    private final SysSettlementBillMapper settlementMapper;
    private final SysBillingOrderMapper orderMapper;
    private final SysProfitSharingRecordMapper recordMapper;

    @Override
    public TableDataInfo<SysSettlementBillVo> queryPageList(String period, Long tenantId, PageQuery pageQuery) {
        LambdaQueryWrapper<SysSettlementBill> wrapper = Wrappers.<SysSettlementBill>lambdaQuery()
            .eq(StrUtil.isNotBlank(period), SysSettlementBill::getPeriod, period)
            .eq(tenantId != null, SysSettlementBill::getTenantId, tenantId)
            .orderByDesc(SysSettlementBill::getPeriod);
        Page<SysSettlementBillVo> page = settlementMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public SysSettlementBillVo queryById(Long id) {
        SysSettlementBillVo vo = settlementMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("对账单不存在");
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysSettlementBillVo generate(Long tenantId, String period) {
        if (tenantId == null) {
            throw new ServiceException("租户ID不能为空");
        }
        if (StrUtil.isBlank(period) || !period.matches("\\d{4}-\\d{2}")) {
            throw new ServiceException("账期格式须为 yyyy-MM");
        }
        Date start = DateUtil.parse(period, "yyyy-MM");
        Date end = DateUtil.offsetMonth(start, 1);

        // 1. 汇总该账期已支付订单
        List<SysBillingOrder> orders = orderMapper.selectList(Wrappers.<SysBillingOrder>lambdaQuery()
            .eq(SysBillingOrder::getTenantId, tenantId)
            .eq(SysBillingOrder::getPayStatus, "PAID")
            .isNotNull(SysBillingOrder::getPayTime)
            .ge(SysBillingOrder::getPayTime, start)
            .lt(SysBillingOrder::getPayTime, end));
        BigDecimal grossAmount = orders.stream()
            .map(o -> o.getAmount() == null ? BigDecimal.ZERO : o.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> orderNos = orders.stream().map(SysBillingOrder::getOrderNo).collect(Collectors.toList());

        // 2. 汇总对应分账记录（平台服务费 / 租户应得）
        List<SysProfitSharingRecord> records = CollUtil.isEmpty(orderNos) ? Collections.emptyList()
            : recordMapper.selectList(Wrappers.<SysProfitSharingRecord>lambdaQuery()
                .in(SysProfitSharingRecord::getOrderNo, orderNos));
        BigDecimal platformFee = records.stream()
            .map(r -> r.getPlatformAmount() == null ? BigDecimal.ZERO : r.getPlatformAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tenantAmount = records.stream()
            .map(r -> r.getTenantAmount() == null ? BigDecimal.ZERO : r.getTenantAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 幂等 upsert 对账单
        String billNo = "ST" + tenantId + "-" + period;
        SysSettlementBill exist = settlementMapper.selectOne(Wrappers.<SysSettlementBill>lambdaQuery()
            .eq(SysSettlementBill::getBillNo, billNo).last("limit 1"));
        SysSettlementBill bill = new SysSettlementBill();
        bill.setBillNo(billNo);
        bill.setTenantId(tenantId);
        bill.setPeriod(period);
        bill.setOrderCount(orders.size());
        bill.setGrossAmount(grossAmount);
        bill.setPlatformFee(platformFee);
        bill.setTenantAmount(tenantAmount);
        bill.setStatus("GENERATED");
        if (exist == null) {
            settlementMapper.insert(bill);
        } else {
            bill.setId(exist.getId());
            settlementMapper.updateById(bill);
        }
        log.info("[Settlement] 生成对账单 {} 订单{}笔 总额{} 平台费{} 租户应得{}", billNo, orders.size(), grossAmount, platformFee, tenantAmount);
        return BeanCopyUtils.copy(bill, SysSettlementBillVo.class);
    }
}
