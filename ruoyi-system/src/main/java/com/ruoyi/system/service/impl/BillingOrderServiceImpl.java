package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.SysBillingOrder;
import com.ruoyi.system.domain.SysBillingPlan;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.bo.BillingOrderCreateBo;
import com.ruoyi.system.domain.vo.SysBillingOrderVo;
import com.ruoyi.system.mapper.SysBillingOrderMapper;
import com.ruoyi.system.mapper.SysBillingPlanMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IBillingOrderService;
import com.ruoyi.system.service.IProfitSharingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 计费订单服务实现（T14.1）
 * <p>下单换算金额/到期时间；付费套餐经离线(offline/manual)即时开通或第三方确认后 markPaid，
 * 支付成功回写 sys_tenant（license_type/edition/expire_date/status=NORMAL）并按规则分账入账虚拟账户。
 * <p>续费到期日顺延（现到期未过则从其起算）；首月特惠 first_only 同租户仅一次；【严禁自动扣款】。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingOrderServiceImpl implements IBillingOrderService {

    private final SysBillingOrderMapper orderMapper;
    private final SysBillingPlanMapper planMapper;
    private final SysTenantMapper tenantMapper;
    private final IProfitSharingService profitSharingService;

    @Override
    public TableDataInfo<SysBillingOrderVo> queryPageList(String orderNo, String payStatus, Long tenantId, PageQuery pageQuery) {
        LambdaQueryWrapper<SysBillingOrder> wrapper = Wrappers.<SysBillingOrder>lambdaQuery()
            .like(StrUtil.isNotBlank(orderNo), SysBillingOrder::getOrderNo, orderNo)
            .eq(StrUtil.isNotBlank(payStatus), SysBillingOrder::getPayStatus, payStatus)
            .eq(tenantId != null, SysBillingOrder::getTenantId, tenantId)
            .orderByDesc(SysBillingOrder::getCreateTime);
        Page<SysBillingOrderVo> page = orderMapper.selectVoPage(pageQuery.build(), wrapper);
        fillPlanName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public SysBillingOrderVo queryById(Long id) {
        SysBillingOrderVo vo = orderMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("订单不存在");
        }
        fillPlanName(Collections.singletonList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysBillingOrderVo createOrder(BillingOrderCreateBo bo) {
        SysBillingPlan plan = planMapper.selectById(bo.getPlanCode());
        if (plan == null) {
            throw new ServiceException("套餐不存在：" + bo.getPlanCode());
        }
        if (!"0".equals(plan.getStatus())) {
            throw new ServiceException("套餐已下架，无法下单");
        }
        Long tenantId = bo.getTenantId();
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在：" + tenantId);
        }
        // 首月特惠资格校验（同租户仅一次；排除已取消订单）
        if (plan.getFirstOnly() != null && plan.getFirstOnly() == 1) {
            Long used = orderMapper.selectCount(Wrappers.<SysBillingOrder>lambdaQuery()
                .eq(SysBillingOrder::getTenantId, tenantId)
                .eq(SysBillingOrder::getPlanCode, plan.getPlanCode())
                .ne(SysBillingOrder::getPayStatus, "CANCELLED"));
            if (used != null && used > 0) {
                throw new ServiceException("首月特惠每个租户仅可享受一次");
            }
        }

        Date now = new Date();
        int days = plan.getDays() == null ? 0 : plan.getDays();
        // 续费叠加：现有到期未过则从其顺延，否则从当前时间起算
        Date base = (tenant.getExpireDate() != null && tenant.getExpireDate().after(now)) ? tenant.getExpireDate() : now;
        Date periodEnd = DateUtil.offsetDay(base, days);

        SysBillingOrder order = new SysBillingOrder();
        order.setOrderNo(genOrderNo());
        // 平台代下单：显式指定目标租户（超管上下文多租户插件不注入）
        order.setTenantId(tenantId);
        order.setPlanCode(plan.getPlanCode());
        order.setAmount(plan.getPrice() == null ? BigDecimal.ZERO : plan.getPrice());
        order.setLicenseType(plan.getLicenseType());
        order.setEdition(plan.getEdition());
        order.setPeriodStart(base);
        order.setPeriodEnd(periodEnd);
        order.setPayChannel(StrUtil.isBlank(bo.getPayChannel()) ? "manual" : bo.getPayChannel());
        order.setTransactionId(bo.getTransactionId());
        order.setRemark(bo.getRemark());

        boolean needPay = plan.getNeedPay() != null && plan.getNeedPay() == 1;
        // 试用(need_pay=0)或平台离线/手动收款即时开通；在线渠道置 PENDING 待确认收款（不自动扣款）
        boolean paidNow = !needPay || "offline".equals(order.getPayChannel()) || "manual".equals(order.getPayChannel());
        order.setPayStatus(paidNow ? "PAID" : "PENDING");
        if (paidNow) {
            order.setPayTime(now);
        }
        orderMapper.insert(order);

        if (paidNow) {
            activateTenant(tenantId, plan, periodEnd);
            profitSharingService.shareForOrder(order.getOrderNo(), tenantId, order.getAmount());
        }
        SysBillingOrderVo vo = BeanCopyUtils.copy(order, SysBillingOrderVo.class);
        vo.setPlanName(plan.getName());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markPaid(Long id, String transactionId) {
        SysBillingOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (!"PENDING".equals(order.getPayStatus())) {
            throw new ServiceException("仅待支付订单可确认收款");
        }
        SysBillingPlan plan = planMapper.selectById(order.getPlanCode());
        if (plan == null) {
            throw new ServiceException("套餐不存在：" + order.getPlanCode());
        }
        SysBillingOrder update = new SysBillingOrder();
        update.setId(id);
        update.setPayStatus("PAID");
        update.setPayTime(new Date());
        if (StrUtil.isNotBlank(transactionId)) {
            update.setTransactionId(transactionId);
        }
        int rows = orderMapper.updateById(update);
        activateTenant(order.getTenantId(), plan, order.getPeriodEnd());
        profitSharingService.shareForOrder(order.getOrderNo(), order.getTenantId(), order.getAmount());
        return rows;
    }

    @Override
    public int cancel(Long id) {
        SysBillingOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (!"PENDING".equals(order.getPayStatus())) {
            throw new ServiceException("仅待支付订单可取消");
        }
        SysBillingOrder update = new SysBillingOrder();
        update.setId(id);
        update.setPayStatus("CANCELLED");
        return orderMapper.updateById(update);
    }

    /**
     * 开通/续费回写租户：license_type/edition/expire_date/status=NORMAL
     * <p>★T15.27 分档计费联动：套餐携带 version_type 时同步回写（micro_pro 套餐→租户升至 micro_pro 档），
     * 与菜单刷新同事务；micro_lite 免费档 need_pay=0/days=0 不激活计费周期，仅开通不写到期日。
     */
    private void activateTenant(Long tenantId, SysBillingPlan plan, Date periodEnd) {
        SysTenant update = new SysTenant();
        update.setId(tenantId);
        update.setLicenseType(plan.getLicenseType());
        update.setEdition(plan.getEdition());
        // ★T15.27：套餐档位回写（NULL=通用套餐不改档位）
        if (StrUtil.isNotBlank(plan.getVersionType())) {
            update.setVersionType(plan.getVersionType());
        }
        update.setExpireDate(periodEnd);
        update.setStatus("NORMAL");
        update.setUpdateBy("billing");
        tenantMapper.updateById(update);
        log.info("[Billing] 租户 {} 开通/续费至 {}（licenseType={} edition={} versionType={}）",
            tenantId, DateUtil.formatDateTime(periodEnd), plan.getLicenseType(), plan.getEdition(), plan.getVersionType());
    }

    private void fillPlanName(List<SysBillingOrderVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<String, String> nameMap = planMapper.selectList().stream()
            .collect(Collectors.toMap(SysBillingPlan::getPlanCode, SysBillingPlan::getName, (a, b) -> a));
        list.forEach(vo -> vo.setPlanName(nameMap.get(vo.getPlanCode())));
    }

    private String genOrderNo() {
        return "BO" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(4);
    }
}
