package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.SysBillingPlan;
import com.ruoyi.system.domain.bo.SysBillingPlanBo;
import com.ruoyi.system.domain.vo.SysBillingPlanVo;
import com.ruoyi.system.mapper.SysBillingPlanMapper;
import com.ruoyi.system.service.IBillingPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 计费套餐服务实现（T14.1）
 * <p>系统级共享表，无租户隔离；主键为套餐编码 plan_code（IdType.INPUT）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingPlanServiceImpl implements IBillingPlanService {

    private final SysBillingPlanMapper billingPlanMapper;

    @Override
    public TableDataInfo<SysBillingPlanVo> queryPageList(String planCode, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<SysBillingPlan> wrapper = Wrappers.<SysBillingPlan>lambdaQuery()
            .like(StrUtil.isNotBlank(planCode), SysBillingPlan::getPlanCode, planCode)
            .eq(StrUtil.isNotBlank(status), SysBillingPlan::getStatus, status)
            .orderByAsc(SysBillingPlan::getSort);
        Page<SysBillingPlanVo> page = billingPlanMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public List<SysBillingPlanVo> listEnabled() {
        return billingPlanMapper.selectVoList(Wrappers.<SysBillingPlan>lambdaQuery()
            .eq(SysBillingPlan::getStatus, "0")
            .orderByAsc(SysBillingPlan::getSort));
    }

    @Override
    public SysBillingPlanVo queryByCode(String planCode) {
        return billingPlanMapper.selectVoById(planCode);
    }

    @Override
    public int insert(SysBillingPlanBo bo) {
        if (billingPlanMapper.selectById(bo.getPlanCode()) != null) {
            throw new ServiceException("套餐编码已存在：" + bo.getPlanCode());
        }
        SysBillingPlan entity = BeanCopyUtils.copy(bo, SysBillingPlan.class);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        return billingPlanMapper.insert(entity);
    }

    @Override
    public int update(SysBillingPlanBo bo) {
        if (billingPlanMapper.selectById(bo.getPlanCode()) == null) {
            throw new ServiceException("套餐不存在：" + bo.getPlanCode());
        }
        SysBillingPlan entity = BeanCopyUtils.copy(bo, SysBillingPlan.class);
        return billingPlanMapper.updateById(entity);
    }
}
