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
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.CashFlow;
import com.ruoyi.system.domain.bo.CashFlowBo;
import com.ruoyi.system.domain.enums.CashFlowType;
import com.ruoyi.system.domain.vo.CashFlowVo;
import com.ruoyi.system.mapper.CashFlowMapper;
import com.ruoyi.system.service.ICashFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 收支流水服务实现（T15.5/T15.14，纯记账口径）
 * <p>cash_flow 租户级表，多租户插件自动隔离；tenant_id 由 insertFill 从登录上下文填充。
 * <p>★V1 零支付合规：本服务仅记账，record 供预约「标记已收款」（income）/结算「标记已线下发放」（expense）内部调用，
 * 全流程无任何支付/转账接口调用。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashFlowServiceImpl implements ICashFlowService {

    private final CashFlowMapper cashFlowMapper;

    @Override
    public TableDataInfo<CashFlowVo> queryPageList(CashFlowBo bo, PageQuery pageQuery) {
        Page<CashFlowVo> page = cashFlowMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        return TableDataInfo.build(page);
    }

    @Override
    public List<CashFlowVo> queryList(CashFlowBo bo) {
        return cashFlowMapper.selectVoList(buildWrapper(bo));
    }

    @Override
    public Long insert(CashFlowBo bo) {
        if (!CashFlowType.isValid(bo.getFlowType())) {
            throw new ServiceException("非法流水类型：" + bo.getFlowType());
        }
        if (bo.getAmount() == null || bo.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("金额必须大于 0");
        }
        CashFlow entity = BeanCopyUtils.copy(bo, CashFlow.class);
        entity.setId(null);
        if (entity.getOccurDate() == null) {
            entity.setOccurDate(new Date());
        }
        if (StrUtil.isBlank(entity.getRelatedType())) {
            entity.setRelatedType(CashFlowType.RELATED_MANUAL);
        }
        entity.setCreateBy(safeUsername());
        cashFlowMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return cashFlowMapper.deleteBatchIds(ids);
    }

    @Override
    public Long record(String flowType, BigDecimal amount, String category, Long relatedOrderId,
                       String relatedType, Date occurDate, String remark) {
        if (!CashFlowType.isValid(flowType)) {
            throw new ServiceException("非法流水类型：" + flowType);
        }
        CashFlow entity = new CashFlow();
        entity.setFlowType(flowType);
        entity.setAmount(amount == null ? BigDecimal.ZERO : amount);
        entity.setCategory(category);
        entity.setRelatedOrderId(relatedOrderId);
        entity.setRelatedType(relatedType);
        entity.setOccurDate(occurDate != null ? occurDate : new Date());
        entity.setRemark(remark);
        entity.setCreateBy(safeUsername());
        cashFlowMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 构建查询条件（flowType/category/relatedType 精确 + occurDate 范围，occur_date 倒序）
     */
    private LambdaQueryWrapper<CashFlow> buildWrapper(CashFlowBo bo) {
        return Wrappers.<CashFlow>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getFlowType()), CashFlow::getFlowType, bo.getFlowType())
            .eq(StrUtil.isNotBlank(bo.getCategory()), CashFlow::getCategory, bo.getCategory())
            .eq(StrUtil.isNotBlank(bo.getRelatedType()), CashFlow::getRelatedType, bo.getRelatedType())
            .ge(bo.getBeginTime() != null, CashFlow::getOccurDate, bo.getBeginTime() == null ? null : DateUtil.beginOfDay(bo.getBeginTime()))
            .le(bo.getEndTime() != null, CashFlow::getOccurDate, bo.getEndTime() == null ? null : DateUtil.endOfDay(bo.getEndTime()))
            .orderByDesc(CashFlow::getOccurDate)
            .orderByDesc(CashFlow::getId);
    }

    /**
     * 安全获取当前登录用户名（无登录态返回 null，供 Job/系统调用兜底）
     */
    private String safeUsername() {
        try {
            return LoginHelper.getUsername();
        } catch (Exception e) {
            return null;
        }
    }
}
