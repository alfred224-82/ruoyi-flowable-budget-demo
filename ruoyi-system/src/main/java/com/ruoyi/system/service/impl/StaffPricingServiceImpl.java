package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.StaffPricing;
import com.ruoyi.system.domain.UserTenantRole;
import com.ruoyi.system.domain.bo.StaffPricingBo;
import com.ruoyi.system.domain.vo.StaffPricingVo;
import com.ruoyi.system.mapper.StaffPricingMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.UserTenantRoleMapper;
import com.ruoyi.system.service.IStaffPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 助教价格服务实现（T21.7，小微版，支持 4 种计价模式）
 * <p>★房间价格复用 service_item.price（已有字段，无需新建表）。
 * <p>租户级表，多租户插件自动隔离。同一助教 UNIQUE(tenant_id, user_id) 仅一条记录。
 * <p>计价模式：duration(计时)/package(套餐)/times(计量)/fixed(一口价)。
 *
 * @author ruoyi
 * @date 2026-10-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffPricingServiceImpl implements IStaffPricingService {

    private final StaffPricingMapper pricingMapper;
    private final SysUserMapper userMapper;
    private final UserTenantRoleMapper userTenantRoleMapper;

    /** salesman 标准角色编码 */
    private static final String ROLE_KEY_SALESMAN = "salesman";

    @Override
    public List<StaffPricingVo> listAll() {
        // ① ★从 user_tenant_role 查询本租户 salesman 角色用户（而非 sys_user_role，
        //   避免 sys_user_role 中存在未通过人员管理分配的脏数据导致多出员工）
        List<UserTenantRole> utrList = userTenantRoleMapper.selectList(
            Wrappers.<UserTenantRole>lambdaQuery()
                .eq(UserTenantRole::getRoleCode, ROLE_KEY_SALESMAN)
                .eq(UserTenantRole::getStatus, "0"));
        if (CollUtil.isEmpty(utrList)) {
            return Collections.emptyList();
        }
        List<Long> staffIds = utrList.stream()
            .map(UserTenantRole::getUserId).distinct().collect(Collectors.toList());
        // ③ 查 sys_user 获取昵称/头像
        List<SysUser> users = userMapper.selectBatchIds(staffIds);
        // ④ 批量查询已设置的价格（hourlyRate）
        Map<Long, BigDecimal> priceMap = batchGetHourlyRates(staffIds);
        // ④b 批量查询 pricingType + extraConfig
        Map<Long, StaffPricing> pricingMap = pricingMapper.selectList(
            Wrappers.<StaffPricing>lambdaQuery()
                .in(StaffPricing::getUserId, staffIds)
                .eq(StaffPricing::getEnabled, 1)).stream()
            .collect(Collectors.toMap(StaffPricing::getUserId, p -> p, (a, b) -> a));
        // ⑤ 组装结果（所有助教都展示，未设置价格的 hourlyRate 为 null）
        return users.stream()
            .filter(u -> "0".equals(u.getStatus()))
            .map(u -> {
                StaffPricingVo vo = new StaffPricingVo();
                vo.setUserId(u.getUserId());
                vo.setNickName(u.getNickName());
                vo.setAvatar(u.getAvatar());
                BigDecimal rate = priceMap.get(u.getUserId());
                vo.setHourlyRate(rate);
                StaffPricing pricing = pricingMap.get(u.getUserId());
                if (pricing != null) {
                    vo.setPricingType(pricing.getPricingType());
                    vo.setExtraConfig(pricing.getExtraConfig());
                    vo.setEnabled(pricing.getEnabled());
                } else {
                    vo.setEnabled(1);
                }
                return vo;
            })
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdatePrice(StaffPricingBo bo) {
        if (bo.getUserId() == null) {
            throw new ServiceException("助教不能为空");
        }
        if (bo.getHourlyRate() == null || bo.getHourlyRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("价格不能为负数");
        }
        // 校验助教存在
        SysUser user = userMapper.selectUserById(bo.getUserId());
        if (user == null) {
            throw new ServiceException("助教用户不存在");
        }
        // 计价模式默认 duration
        String pricingType = StrUtil.isNotBlank(bo.getPricingType()) ? bo.getPricingType() : "duration";
        // upsert：查已有记录
        StaffPricing exist = pricingMapper.selectOne(
            Wrappers.<StaffPricing>lambdaQuery()
                .eq(StaffPricing::getUserId, bo.getUserId())
                .last("LIMIT 1"));
        if (exist != null) {
            // 更新
            StaffPricing update = new StaffPricing();
            update.setId(exist.getId());
            update.setHourlyRate(bo.getHourlyRate());
            update.setPricingType(pricingType);
            update.setExtraConfig(bo.getExtraConfig());
            if (bo.getEnabled() != null) update.setEnabled(bo.getEnabled());
            if (bo.getRemark() != null) update.setRemark(bo.getRemark());
            pricingMapper.updateById(update);
            return exist.getId();
        } else {
            // 新增
            StaffPricing entity = new StaffPricing();
            entity.setUserId(bo.getUserId());
            entity.setHourlyRate(bo.getHourlyRate());
            entity.setPricingType(pricingType);
            entity.setExtraConfig(bo.getExtraConfig());
            entity.setEnabled(bo.getEnabled() != null ? bo.getEnabled() : 1);
            entity.setRemark(bo.getRemark());
            pricingMapper.insert(entity);
            return entity.getId();
        }
    }

    @Override
    public BigDecimal getHourlyRate(Long userId) {
        if (userId == null) {
            return null;
        }
        StaffPricing pricing = pricingMapper.selectOne(
            Wrappers.<StaffPricing>lambdaQuery()
                .eq(StaffPricing::getUserId, userId)
                .eq(StaffPricing::getEnabled, 1)
                .last("LIMIT 1"));
        return pricing != null ? pricing.getHourlyRate() : null;
    }

    @Override
    public Map<Long, BigDecimal> batchGetHourlyRates(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        List<StaffPricing> list = pricingMapper.selectList(
            Wrappers.<StaffPricing>lambdaQuery()
                .in(StaffPricing::getUserId, userIds)
                .eq(StaffPricing::getEnabled, 1));
        return list.stream().collect(Collectors.toMap(
            StaffPricing::getUserId, StaffPricing::getHourlyRate, (a, b) -> a));
    }
}
