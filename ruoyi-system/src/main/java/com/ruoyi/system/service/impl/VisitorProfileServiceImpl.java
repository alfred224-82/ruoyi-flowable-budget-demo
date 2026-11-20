package com.ruoyi.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.TempVisitorAccount;
import com.ruoyi.system.domain.VisitorProfile;
import com.ruoyi.system.domain.bo.VisitorProfileBo;
import com.ruoyi.system.domain.vo.VisitorProfileVo;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.TempVisitorAccountMapper;
import com.ruoyi.system.mapper.VisitorProfileMapper;
import com.ruoyi.system.service.IVisitorProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 游客信息补全服务实现（M31 T31.6/T31.7/T31.8）
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorProfileServiceImpl implements IVisitorProfileService {

    private final TempVisitorAccountMapper tempVisitorAccountMapper;
    private final VisitorProfileMapper visitorProfileMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<VisitorProfileVo> listTempVisitors(int pageNum, int pageSize) {
        Long tenantId = LoginHelper.getTenantId();
        // 查询有效/已补全的临时游客账号
        LambdaQueryWrapper<TempVisitorAccount> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TempVisitorAccount::getTenantId, tenantId)
            .in(TempVisitorAccount::getStatus, TempVisitorAccount.STATUS_ACTIVE, TempVisitorAccount.STATUS_COMPLETED)
            .orderByDesc(TempVisitorAccount::getCreateTime);
        Page<TempVisitorAccount> page = new Page<>(pageNum, pageSize);
        Page<TempVisitorAccount> result = tempVisitorAccountMapper.selectPage(page, wrapper);

        return result.getRecords().stream().map(account -> {
            VisitorProfileVo vo = new VisitorProfileVo();
            vo.setUserId(account.getUserId());
            vo.setPhoneLast4(account.getPhoneLast4());
            vo.setVisitorStatus(account.getStatus());
            vo.setExpireTime(account.getExpireTime());
            vo.setCreateTime(account.getCreateTime());
            // 回填 sys_user 信息
            SysUser user = sysUserMapper.selectById(account.getUserId());
            if (user != null) {
                vo.setNickName(user.getNickName());
                vo.setPhone(user.getPhonenumber());
            }
            // 回填 visitor_profile 信息
            VisitorProfile profile = visitorProfileMapper.selectById(account.getUserId());
            if (profile != null) {
                vo.setRealName(profile.getRealName());
                vo.setIdCard(profile.getIdCard());
                vo.setBirthday(profile.getBirthday());
                vo.setGender(profile.getGender());
                vo.setAddress(profile.getAddress());
                vo.setIsComplete(profile.getIsComplete());
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisitorProfile(VisitorProfileBo bo) {
        Long userId = bo.getUserId();
        Long tenantId = LoginHelper.getTenantId();

        // 校验临时账号存在且有效
        TempVisitorAccount account = tempVisitorAccountMapper.selectOne(
            Wrappers.<TempVisitorAccount>lambdaQuery()
                .eq(TempVisitorAccount::getTenantId, tenantId)
                .eq(TempVisitorAccount::getUserId, userId));
        if (account == null) {
            throw new ServiceException("临时游客账号不存在");
        }

        // 插入/更新 visitor_profile
        VisitorProfile profile = visitorProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new VisitorProfile();
            profile.setUserId(userId);
            profile.setTenantId(tenantId);
        }
        BeanUtil.copyProperties(bo, profile, "userId");
        profile.setIsComplete(1);
        if (profile.getUserId() != null && visitorProfileMapper.selectById(profile.getUserId()) != null) {
            visitorProfileMapper.updateById(profile);
        } else {
            visitorProfileMapper.insert(profile);
        }

        // 更新 temp_visitor_account.status = 已补全
        account.setStatus(TempVisitorAccount.STATUS_COMPLETED);
        tempVisitorAccountMapper.updateById(account);

        // 更新 sys_user.nick_name
        if (StrUtil.isNotBlank(bo.getRealName())) {
            SysUser user = new SysUser();
            user.setUserId(userId);
            user.setNickName(bo.getRealName());
            sysUserMapper.updateById(user);
        }
        log.info("[VisitorProfile] Boss 补全游客信息 userId={} realName={}", userId, bo.getRealName());
    }

    @Override
    public VisitorProfileVo getMyProfile(Long userId) {
        VisitorProfileVo vo = new VisitorProfileVo();
        vo.setUserId(userId);
        // sys_user 信息
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            vo.setNickName(user.getNickName());
            vo.setPhone(user.getPhonenumber());
        }
        // visitor_profile 信息
        VisitorProfile profile = visitorProfileMapper.selectById(userId);
        if (profile != null) {
            vo.setRealName(profile.getRealName());
            vo.setIdCard(profile.getIdCard());
            vo.setBirthday(profile.getBirthday());
            vo.setGender(profile.getGender());
            vo.setAddress(profile.getAddress());
            vo.setWechatOpenid(profile.getWechatOpenid());
            vo.setAlipayUserId(profile.getAlipayUserId());
            vo.setIsComplete(profile.getIsComplete());
        }
        // 临时账号状态
        TempVisitorAccount account = tempVisitorAccountMapper.selectOne(
            Wrappers.<TempVisitorAccount>lambdaQuery()
                .eq(TempVisitorAccount::getUserId, userId));
        if (account != null) {
            vo.setVisitorStatus(account.getStatus());
            vo.setExpireTime(account.getExpireTime());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMyProfile(Long userId, VisitorProfileBo bo) {
        Long tenantId = LoginHelper.getTenantId();
        VisitorProfile profile = visitorProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new VisitorProfile();
            profile.setUserId(userId);
            profile.setTenantId(tenantId);
        }
        BeanUtil.copyProperties(bo, profile, "userId");
        profile.setIsComplete(1);
        if (visitorProfileMapper.selectById(userId) != null) {
            visitorProfileMapper.updateById(profile);
        } else {
            visitorProfileMapper.insert(profile);
        }

        // 更新临时账号状态为已补全
        TempVisitorAccount account = tempVisitorAccountMapper.selectOne(
            Wrappers.<TempVisitorAccount>lambdaQuery()
                .eq(TempVisitorAccount::getUserId, userId));
        if (account != null) {
            account.setStatus(TempVisitorAccount.STATUS_COMPLETED);
            tempVisitorAccountMapper.updateById(account);
        }

        // 同步 sys_user.nick_name
        if (StrUtil.isNotBlank(bo.getRealName())) {
            SysUser user = new SysUser();
            user.setUserId(userId);
            user.setNickName(bo.getRealName());
            sysUserMapper.updateById(user);
        }
        log.info("[VisitorProfile] 游客自助完善 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void upgradeToFormal(Long userId, VisitorProfileBo bo) {
        Long tenantId = LoginHelper.getTenantId();

        // 校验身份证同租户唯一性
        if (StrUtil.isNotBlank(bo.getIdCard())) {
            LambdaQueryWrapper<VisitorProfile> idCardCheck = Wrappers.lambdaQuery();
            idCardCheck.eq(VisitorProfile::getTenantId, tenantId)
                .eq(VisitorProfile::getIdCard, bo.getIdCard())
                .ne(VisitorProfile::getUserId, userId);
            Long count = visitorProfileMapper.selectCount(idCardCheck);
            if (count > 0) {
                throw new ServiceException("该身份证号在同租户下已被使用");
            }
        }

        // 更新 visitor_profile
        VisitorProfile profile = visitorProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new VisitorProfile();
            profile.setUserId(userId);
            profile.setTenantId(tenantId);
        }
        BeanUtil.copyProperties(bo, profile, "userId");
        profile.setIsComplete(1);
        if (visitorProfileMapper.selectById(userId) != null) {
            visitorProfileMapper.updateById(profile);
        } else {
            visitorProfileMapper.insert(profile);
        }

        // 更新 sys_user.status = ACTIVE（从 TEMPORARY 改为 ACTIVE）
        SysUser user = sysUserMapper.selectById(userId);
        if (user != null) {
            user.setStatus("0"); // 0=正常（ACTIVE）
            sysUserMapper.updateById(user);
        }

        // 更新 temp_visitor_account.status = 已补全
        TempVisitorAccount account = tempVisitorAccountMapper.selectOne(
            Wrappers.<TempVisitorAccount>lambdaQuery()
                .eq(TempVisitorAccount::getUserId, userId));
        if (account != null) {
            account.setStatus(TempVisitorAccount.STATUS_COMPLETED);
            tempVisitorAccountMapper.updateById(account);
        }
        log.info("[VisitorProfile] 游客升级为正式顾客 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchUpgradeToFormal(List<Long> userIds) {
        Long tenantId = LoginHelper.getTenantId();
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                // 校验信息完整性
                VisitorProfile profile = visitorProfileMapper.selectById(userId);
                if (profile == null || StrUtil.isBlank(profile.getRealName()) || StrUtil.isBlank(profile.getIdCard())) {
                    failCount++;
                    failReasons.add("用户ID " + userId + "：信息不完整（缺少姓名/身份证）");
                    continue;
                }
                // 校验身份证唯一性
                LambdaQueryWrapper<VisitorProfile> idCardCheck = Wrappers.lambdaQuery();
                idCardCheck.eq(VisitorProfile::getTenantId, tenantId)
                    .eq(VisitorProfile::getIdCard, profile.getIdCard())
                    .ne(VisitorProfile::getUserId, userId);
                Long dupCount = visitorProfileMapper.selectCount(idCardCheck);
                if (dupCount > 0) {
                    failCount++;
                    failReasons.add("用户ID " + userId + "：身份证重复");
                    continue;
                }
                // 更新 sys_user.status = 正常
                SysUser user = sysUserMapper.selectById(userId);
                if (user != null) {
                    user.setStatus("0");
                    sysUserMapper.updateById(user);
                }
                // 更新 temp_visitor_account.status = 已补全
                TempVisitorAccount account = tempVisitorAccountMapper.selectOne(
                    Wrappers.<TempVisitorAccount>lambdaQuery()
                        .eq(TempVisitorAccount::getUserId, userId));
                if (account != null) {
                    account.setStatus(TempVisitorAccount.STATUS_COMPLETED);
                    tempVisitorAccountMapper.updateById(account);
                }
                successCount++;
            } catch (Exception e) {
                failCount++;
                failReasons.add("用户ID " + userId + "：" + e.getMessage());
                log.error("[VisitorProfile] 批量转正失败 userId={}", userId, e);
            }
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failReasons", failReasons);
        log.info("[VisitorProfile] 批量转正完成 total={} success={} fail={}", userIds.size(), successCount, failCount);
        return result;
    }
}
