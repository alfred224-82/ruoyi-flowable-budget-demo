package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysSubscribeQuota;
import com.ruoyi.system.domain.vo.SysSubscribeQuotaVo;
import com.ruoyi.system.mapper.SysSubscribeQuotaMapper;
import com.ruoyi.system.service.ISubscribeQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * 订阅消息授权次数服务实现（T14.4）
 * <p>sys_subscribe_quota 仅含 create_time/update_time，故实体不继承 BaseEntity，tenantId 由服务层显式写入。
 * <p>授权/消费均以原子条件 UPDATE（setSql 自增自减 + remaining&gt;0 守卫）执行，杜绝并发超发；
 * 消费失败（授权不足）返回 false，调用方跳过发送且不报错。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeQuotaServiceImpl implements ISubscribeQuotaService {

    private final SysSubscribeQuotaMapper quotaMapper;

    @Override
    public int grant(String openid, String templateId) {
        if (StrUtil.isBlank(openid) || StrUtil.isBlank(templateId)) {
            return 0;
        }
        Long tenantId = LoginHelper.getTenantId();
        SysSubscribeQuota exist = selectByKey(tenantId, openid, templateId);
        if (exist == null) {
            SysSubscribeQuota entity = new SysSubscribeQuota();
            entity.setTenantId(tenantId);
            entity.setOpenid(openid);
            entity.setTemplateId(templateId);
            entity.setRemainingCount(1);
            entity.setTotalGranted(1);
            entity.setTotalSent(0);
            try {
                return quotaMapper.insert(entity);
            } catch (DuplicateKeyException e) {
                // 并发首次授权：退化为原子自增
                return incrGrant(tenantId, openid, templateId);
            }
        }
        return incrGrant(tenantId, openid, templateId);
    }

    @Override
    public boolean checkAndConsume(String openid, String templateId) {
        if (StrUtil.isBlank(openid) || StrUtil.isBlank(templateId)) {
            return false;
        }
        Long tenantId = LoginHelper.getTenantId();
        // 原子条件消费：仅当 remaining_count>0 时 -1，否则 0 行 → 授权不足，不发送
        int updated = quotaMapper.update(null, Wrappers.<SysSubscribeQuota>lambdaUpdate()
            .eq(tenantId != null, SysSubscribeQuota::getTenantId, tenantId)
            .eq(SysSubscribeQuota::getOpenid, openid)
            .eq(SysSubscribeQuota::getTemplateId, templateId)
            .gt(SysSubscribeQuota::getRemainingCount, 0)
            .setSql("remaining_count = remaining_count - 1")
            .setSql("total_sent = total_sent + 1"));
        if (updated <= 0) {
            log.debug("[SubscribeQuota] 授权次数不足，跳过发送 openid={} tpl={}", openid, templateId);
            return false;
        }
        return true;
    }

    @Override
    public int remaining(String openid, String templateId) {
        SysSubscribeQuota q = selectByKey(LoginHelper.getTenantId(), openid, templateId);
        return (q == null || q.getRemainingCount() == null) ? 0 : q.getRemainingCount();
    }

    @Override
    public TableDataInfo<SysSubscribeQuotaVo> queryPageList(String openid, PageQuery pageQuery) {
        LambdaQueryWrapper<SysSubscribeQuota> wrapper = Wrappers.<SysSubscribeQuota>lambdaQuery()
            .eq(StrUtil.isNotBlank(openid), SysSubscribeQuota::getOpenid, openid)
            .orderByDesc(SysSubscribeQuota::getUpdateTime);
        Page<SysSubscribeQuotaVo> page = quotaMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    private int incrGrant(Long tenantId, String openid, String templateId) {
        return quotaMapper.update(null, Wrappers.<SysSubscribeQuota>lambdaUpdate()
            .eq(tenantId != null, SysSubscribeQuota::getTenantId, tenantId)
            .eq(SysSubscribeQuota::getOpenid, openid)
            .eq(SysSubscribeQuota::getTemplateId, templateId)
            .setSql("remaining_count = remaining_count + 1")
            .setSql("total_granted = total_granted + 1"));
    }

    private SysSubscribeQuota selectByKey(Long tenantId, String openid, String templateId) {
        return quotaMapper.selectOne(Wrappers.<SysSubscribeQuota>lambdaQuery()
            .eq(tenantId != null, SysSubscribeQuota::getTenantId, tenantId)
            .eq(SysSubscribeQuota::getOpenid, openid)
            .eq(SysSubscribeQuota::getTemplateId, templateId)
            .last("limit 1"));
    }
}
