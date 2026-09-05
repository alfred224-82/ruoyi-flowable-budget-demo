package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.SysProfitSharingRecord;
import com.ruoyi.system.domain.SysProfitSharingRule;
import com.ruoyi.system.domain.bo.ProfitSharingRuleBo;
import com.ruoyi.system.domain.vo.SysProfitSharingRecordVo;
import com.ruoyi.system.domain.vo.SysProfitSharingRuleVo;
import com.ruoyi.system.mapper.SysProfitSharingRecordMapper;
import com.ruoyi.system.mapper.SysProfitSharingRuleMapper;
import com.ruoyi.system.service.IProfitSharingService;
import com.ruoyi.system.service.IVirtualAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 分账服务实现（T14.3）
 * <p>sys_profit_sharing_rule 继承 TenantEntity；平台超管跨租户维护时显式写入 tenantId（多租户插件不注入）。
 * <p>分账金额采用「平台额四舍五入、租户额 = 订单额 - 平台额」，确保合计严格等于订单金额。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitSharingServiceImpl implements IProfitSharingService {

    private final SysProfitSharingRuleMapper ruleMapper;
    private final SysProfitSharingRecordMapper recordMapper;
    private final IVirtualAccountService virtualAccountService;

    @Override
    public SysProfitSharingRuleVo getRule(Long tenantId) {
        SysProfitSharingRule rule = selectRule(tenantId);
        if (rule == null) {
            // 未配置：默认平台 0、租户全额
            SysProfitSharingRuleVo def = new SysProfitSharingRuleVo();
            def.setTenantId(tenantId);
            def.setPlatformRate(BigDecimal.ZERO);
            def.setTenantRate(BigDecimal.ONE);
            def.setStatus("0");
            return def;
        }
        return BeanCopyUtils.copy(rule, SysProfitSharingRuleVo.class);
    }

    @Override
    public int saveRule(Long tenantId, ProfitSharingRuleBo bo) {
        BigDecimal platformRate = bo.getPlatformRate() == null ? BigDecimal.ZERO : bo.getPlatformRate();
        SysProfitSharingRule exist = selectRule(tenantId);
        SysProfitSharingRule entity = new SysProfitSharingRule();
        entity.setPlatformRate(platformRate);
        entity.setTenantRate(BigDecimal.ONE.subtract(platformRate));
        entity.setEffectiveTime(new Date());
        entity.setStatus(StrUtil.isBlank(bo.getStatus()) ? "0" : bo.getStatus());
        if (exist == null) {
            entity.setTenantId(tenantId);
            return ruleMapper.insert(entity);
        }
        entity.setId(exist.getId());
        return ruleMapper.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysProfitSharingRecordVo shareForOrder(String orderNo, Long tenantId, BigDecimal orderAmount) {
        BigDecimal amount = orderAmount == null ? BigDecimal.ZERO : orderAmount;
        SysProfitSharingRule rule = selectRule(tenantId);
        BigDecimal platformRate = (rule == null || rule.getPlatformRate() == null) ? BigDecimal.ZERO : rule.getPlatformRate();
        // 平台服务费四舍五入到分；租户应得 = 订单额 - 平台额（合计严格=订单金额）
        BigDecimal platformAmount = amount.multiply(platformRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tenantAmount = amount.subtract(platformAmount);

        // 租户应得入账虚拟账户（0 元订单 credit 内部忽略）
        virtualAccountService.credit(tenantId, tenantAmount);

        SysProfitSharingRecord record = new SysProfitSharingRecord();
        record.setTenantId(tenantId);
        record.setOrderNo(orderNo);
        record.setOrderAmount(amount);
        record.setPlatformAmount(platformAmount);
        record.setTenantAmount(tenantAmount);
        record.setPlatformRate(platformRate);
        record.setStatus("SUCCESS");
        recordMapper.insert(record);
        log.info("[ProfitSharing] 订单 {} 分账完成 tenantId={} 平台={} 租户={}", orderNo, tenantId, platformAmount, tenantAmount);
        return BeanCopyUtils.copy(record, SysProfitSharingRecordVo.class);
    }

    @Override
    public TableDataInfo<SysProfitSharingRecordVo> queryRecordPageList(String orderNo, Long tenantId, PageQuery pageQuery) {
        LambdaQueryWrapper<SysProfitSharingRecord> wrapper = Wrappers.<SysProfitSharingRecord>lambdaQuery()
            .eq(StrUtil.isNotBlank(orderNo), SysProfitSharingRecord::getOrderNo, orderNo)
            .eq(tenantId != null, SysProfitSharingRecord::getTenantId, tenantId)
            .orderByDesc(SysProfitSharingRecord::getCreateTime);
        Page<SysProfitSharingRecordVo> page = recordMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    private SysProfitSharingRule selectRule(Long tenantId) {
        return ruleMapper.selectOne(Wrappers.<SysProfitSharingRule>lambdaQuery()
            .eq(SysProfitSharingRule::getTenantId, tenantId)
            .last("limit 1"));
    }
}
