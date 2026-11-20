package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.StaffContactPublic;
import com.ruoyi.system.mapper.StaffContactPublicMapper;
import com.ruoyi.system.service.IMicroStaffScheduleService;
import com.ruoyi.system.service.IStaffContactPublicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 员工联系方式公开开关服务实现（T31.5，设计方案 §八之二）
 * <p>staff_contact_public 为租户级表，多租户插件自动隔离 tenant_id。
 * <p>★默认全关：新入职员工无记录时视为「不公开手机号 + 不公开微信号」，
 *   查询接口返回默认对象（showPhone=0, showWechat=0）而非 null，避免前端空指针。
 * <p>★幂等：saveOrUpdate 内部按 (tenant_id, staff_id) 唯一键决定 insert/update。
 *
 * @author ruoyi
 * @date 2026-10-19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaffContactPublicServiceImpl implements IStaffContactPublicService {

    private final StaffContactPublicMapper contactPublicMapper;
    private final IMicroStaffScheduleService staffScheduleService;

    @Override
    public StaffContactPublic getByStaffId(Long staffId) {
        if (staffId == null) {
            throw new ServiceException("员工ID不能为空");
        }
        StaffContactPublic entity = contactPublicMapper.selectOne(
            Wrappers.<StaffContactPublic>lambdaQuery()
                .eq(StaffContactPublic::getStaffId, staffId)
                .last("LIMIT 1"));
        if (entity == null) {
            // 默认全关对象（不落库，仅用于返回给前端展示初始态）
            entity = new StaffContactPublic();
            entity.setStaffId(staffId);
            entity.setTenantId(LoginHelper.getTenantId());
            entity.setShowPhone(0);
            entity.setShowWechat(0);
        }
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveOrUpdate(StaffContactPublic entity) {
        if (entity == null || entity.getStaffId() == null) {
            throw new ServiceException("员工ID不能为空");
        }
        // 规范化开关值：null → 0（关）
        if (entity.getShowPhone() == null) {
            entity.setShowPhone(0);
        }
        if (entity.getShowWechat() == null) {
            entity.setShowWechat(0);
        }
        StaffContactPublic exist = contactPublicMapper.selectOne(
            Wrappers.<StaffContactPublic>lambdaQuery()
                .eq(StaffContactPublic::getStaffId, entity.getStaffId())
                .last("LIMIT 1"));
        if (exist == null) {
            entity.setId(null);
            return contactPublicMapper.insert(entity);
        }
        entity.setId(exist.getId());
        return contactPublicMapper.updateById(entity);
    }

    @Override
    public boolean isPhonePublic(Long staffId) {
        if (staffId == null) {
            return false;
        }
        StaffContactPublic cfg = contactPublicMapper.selectOne(
            Wrappers.<StaffContactPublic>lambdaQuery()
                .eq(StaffContactPublic::getStaffId, staffId)
                .last("LIMIT 1"));
        return cfg != null && Integer.valueOf(1).equals(cfg.getShowPhone());
    }

    @Override
    public boolean isWechatPublic(Long staffId) {
        if (staffId == null) {
            return false;
        }
        StaffContactPublic cfg = contactPublicMapper.selectOne(
            Wrappers.<StaffContactPublic>lambdaQuery()
                .eq(StaffContactPublic::getStaffId, staffId)
                .last("LIMIT 1"));
        return cfg != null && Integer.valueOf(1).equals(cfg.getShowWechat());
    }

    @Override
    public List<Map<String, Object>> listAllWithStaff() {
        // 本租户全部业务员（userId/nickName/avatar）
        List<Map<String, Object>> staffOptions = staffScheduleService.listStaffOptions();
        // 本租户已配置的开关记录，按 staffId 索引
        List<StaffContactPublic> configs = contactPublicMapper.selectList(Wrappers.emptyWrapper());
        Map<Long, StaffContactPublic> cfgMap = configs.stream()
            .filter(c -> c.getStaffId() != null)
            .collect(Collectors.toMap(StaffContactPublic::getStaffId, c -> c, (a, b) -> a));
        return staffOptions.stream().map(opt -> {
            Object uidObj = opt.get("userId");
            Long staffId = uidObj == null ? null : Long.valueOf(String.valueOf(uidObj));
            StaffContactPublic cfg = staffId == null ? null : cfgMap.get(staffId);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", staffId);
            row.put("nickName", opt.get("nickName"));
            row.put("avatar", opt.get("avatar"));
            // ★默认全关：无记录时 showPhone=0 / showWechat=0
            row.put("showPhone", cfg != null && Integer.valueOf(1).equals(cfg.getShowPhone()) ? 1 : 0);
            row.put("showWechat", cfg != null && Integer.valueOf(1).equals(cfg.getShowWechat()) ? 1 : 0);
            row.put("wechatQrcodeUrl", cfg != null ? cfg.getWechatQrcodeUrl() : null);
            return row;
        }).collect(Collectors.toList());
    }
}
