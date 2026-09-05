package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.system.domain.UserTenantBind;
import com.ruoyi.system.domain.vo.UserTenantBindVo;
import com.ruoyi.system.mapper.UserTenantBindMapper;
import com.ruoyi.system.service.IUserTenantBindService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 用户租户绑定服务实现（T10.1 / T13.1 双阶段租户识别 - 引导态）
 * <p>引导态按 openid 反查门店列表（此时未登录，TenantLineInnerInterceptor 因 tenantId=null 不过滤），
 * 绝不自动猜测租户；用户显式选定后进入鉴权态。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTenantBindServiceImpl implements IUserTenantBindService {

    private final UserTenantBindMapper userTenantBindMapper;

    @Override
    public List<UserTenantBindVo> listByOpenid(String openid) {
        if (StrUtil.isBlank(openid)) {
            return Collections.emptyList();
        }
        return userTenantBindMapper.selectVoList(
            Wrappers.<UserTenantBind>lambdaQuery()
                .eq(UserTenantBind::getOpenid, openid)
                .orderByDesc(UserTenantBind::getIsDefault)
                .orderByAsc(UserTenantBind::getId));
    }

    @Override
    public int bind(String openid, Long tenantId, Long userId, Integer isDefault) {
        if (StrUtil.isBlank(openid) || tenantId == null) {
            throw new IllegalArgumentException("openid 与 tenantId 不能为空");
        }
        UserTenantBind exist = userTenantBindMapper.selectOne(
            Wrappers.<UserTenantBind>lambdaQuery()
                .eq(UserTenantBind::getOpenid, openid)
                .eq(UserTenantBind::getTenantId, tenantId));
        int def = isDefault == null ? 0 : isDefault;
        if (exist == null) {
            UserTenantBind entity = new UserTenantBind();
            entity.setOpenid(openid);
            entity.setTenantId(tenantId);
            entity.setUserId(userId);
            entity.setIsDefault(def);
            return userTenantBindMapper.insert(entity);
        }
        exist.setUserId(userId != null ? userId : exist.getUserId());
        exist.setIsDefault(def);
        return userTenantBindMapper.updateById(exist);
    }

    @Override
    public int fillUserId(String openid, Long tenantId, Long userId) {
        if (StrUtil.isBlank(openid) || tenantId == null || userId == null) {
            return 0;
        }
        UserTenantBind update = new UserTenantBind();
        update.setUserId(userId);
        return userTenantBindMapper.update(update,
            Wrappers.<UserTenantBind>lambdaUpdate()
                .eq(UserTenantBind::getOpenid, openid)
                .eq(UserTenantBind::getTenantId, tenantId));
    }
}
