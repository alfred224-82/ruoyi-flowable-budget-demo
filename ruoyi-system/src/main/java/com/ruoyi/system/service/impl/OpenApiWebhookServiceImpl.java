package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.common.utils.OpenApiSignUtils;
import com.ruoyi.system.domain.OpenApiCredential;
import com.ruoyi.system.domain.OpenApiWebhook;
import com.ruoyi.system.domain.bo.OpenApiWebhookBo;
import com.ruoyi.system.domain.vo.OpenApiWebhookVo;
import com.ruoyi.system.mapper.OpenApiCredentialMapper;
import com.ruoyi.system.mapper.OpenApiWebhookMapper;
import com.ruoyi.system.service.IOpenApiWebhookService;
import com.ruoyi.system.service.support.SecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放平台Webhook回调注册Service实现（T7.8 / FEAT_OPEN_API）
 * <p>secret 加密存储、返回脱敏，仅创建/重置时一次性明文返回；
 * listEnabledForEvent 供事件派发器按租户+事件显式取用（异步派发无登录上下文，不依赖多租户插件）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiWebhookServiceImpl implements IOpenApiWebhookService {

    private final OpenApiWebhookMapper webhookMapper;
    private final OpenApiCredentialMapper credentialMapper;
    private final SecretCipher secretCipher;

    @Override
    public TableDataInfo<OpenApiWebhookVo> queryPageList(Long credentialId, String eventType, PageQuery pageQuery) {
        LambdaQueryWrapper<OpenApiWebhook> wrapper = Wrappers.<OpenApiWebhook>lambdaQuery()
            .eq(credentialId != null, OpenApiWebhook::getCredentialId, credentialId)
            .eq(StrUtil.isNotBlank(eventType), OpenApiWebhook::getEventType, eventType)
            .orderByDesc(OpenApiWebhook::getCreateTime);
        Page<OpenApiWebhookVo> page = webhookMapper.selectVoPage(pageQuery.build(), wrapper);
        page.getRecords().forEach(this::desensitizeAndFill);
        return TableDataInfo.build(page);
    }

    @Override
    public List<OpenApiWebhookVo> listByCredential(Long credentialId) {
        List<OpenApiWebhookVo> list = webhookMapper.selectVoList(Wrappers.<OpenApiWebhook>lambdaQuery()
            .eq(OpenApiWebhook::getCredentialId, credentialId)
            .orderByDesc(OpenApiWebhook::getCreateTime));
        list.forEach(this::desensitizeAndFill);
        return list;
    }

    @Override
    public OpenApiWebhookVo queryById(Long id) {
        OpenApiWebhookVo vo = webhookMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("Webhook不存在");
        }
        desensitizeAndFill(vo);
        return vo;
    }

    @Override
    public Map<String, String> create(OpenApiWebhookBo bo) {
        OpenApiCredential credential = credentialMapper.selectById(bo.getCredentialId());
        if (credential == null) {
            throw new ServiceException("关联凭证不存在");
        }
        String plainSecret = OpenApiSignUtils.generateAppSecret();
        OpenApiWebhook entity = BeanCopyUtils.copy(bo, OpenApiWebhook.class);
        entity.setId(null);
        entity.setSecret(secretCipher.encrypt(plainSecret));
        if (entity.getIsEnabled() == null) {
            entity.setIsEnabled(1);
        }
        if (entity.getRetryTimes() == null) {
            entity.setRetryTimes(3);
        }
        webhookMapper.insert(entity);
        Map<String, String> result = new HashMap<>(4);
        result.put("id", String.valueOf(entity.getId()));
        result.put("secret", plainSecret);
        return result;
    }

    @Override
    public int update(OpenApiWebhookBo bo) {
        OpenApiWebhook exist = webhookMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("Webhook不存在");
        }
        OpenApiWebhook entity = BeanCopyUtils.copy(bo, OpenApiWebhook.class);
        // secret 不通过编辑接口变更
        entity.setSecret(null);
        return webhookMapper.updateById(entity);
    }

    @Override
    public String resetSecret(Long id) {
        OpenApiWebhook exist = webhookMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("Webhook不存在");
        }
        String plainSecret = OpenApiSignUtils.generateAppSecret();
        OpenApiWebhook entity = new OpenApiWebhook();
        entity.setId(id);
        entity.setSecret(secretCipher.encrypt(plainSecret));
        webhookMapper.updateById(entity);
        return plainSecret;
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return webhookMapper.deleteBatchIds(ids);
    }

    @Override
    public List<OpenApiWebhook> listEnabledForEvent(Long tenantId, String eventType) {
        return webhookMapper.selectList(Wrappers.<OpenApiWebhook>lambdaQuery()
            .eq(OpenApiWebhook::getTenantId, tenantId)
            .eq(OpenApiWebhook::getEventType, eventType)
            .eq(OpenApiWebhook::getIsEnabled, 1));
    }

    /**
     * secret 脱敏 + 填充关联凭证 AppKey/应用名
     */
    private void desensitizeAndFill(OpenApiWebhookVo vo) {
        vo.setSecret(secretCipher.mask(vo.getSecret()));
        if (vo.getCredentialId() != null) {
            OpenApiCredential credential = credentialMapper.selectById(vo.getCredentialId());
            if (credential != null) {
                vo.setAppKey(credential.getAppKey());
                vo.setAppName(credential.getAppName());
            }
        }
    }
}
