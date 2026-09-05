package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.TenantPaymentConfig;
import com.ruoyi.system.domain.bo.TenantPaymentConfigBo;
import com.ruoyi.system.domain.vo.TenantPaymentConfigVo;
import com.ruoyi.system.mapper.TenantPaymentConfigMapper;
import com.ruoyi.system.service.ITenantPaymentConfigService;
import com.ruoyi.system.service.support.SecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 租户收款配置服务实现（T13.6）
 * <p>tenant_payment_config 含 tenant_id，租户隔离由多租户插件自动处理；每租户唯一（uk_tenant）。
 * apiKey 加密存储，接口返回一律脱敏。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantPaymentConfigServiceImpl implements ITenantPaymentConfigService {

    private final TenantPaymentConfigMapper paymentConfigMapper;
    private final SecretCipher secretCipher;

    @Override
    public TenantPaymentConfigVo getConfig() {
        TenantPaymentConfigVo vo = paymentConfigMapper.selectVoOne(
            Wrappers.<TenantPaymentConfig>lambdaQuery().last("limit 1"));
        if (vo != null) {
            vo.setApiKey(secretCipher.mask(vo.getApiKey()));
        }
        return vo;
    }

    @Override
    public int saveConfig(TenantPaymentConfigBo bo) {
        TenantPaymentConfig exist = paymentConfigMapper.selectOne(
            Wrappers.<TenantPaymentConfig>lambdaQuery().last("limit 1"));
        TenantPaymentConfig entity = BeanCopyUtils.copy(bo, TenantPaymentConfig.class);
        // apiKey 明文入参 → 加密存储；编辑未传则保留原密文
        if (StrUtil.isNotBlank(bo.getApiKey())) {
            entity.setApiKey(secretCipher.encrypt(bo.getApiKey()));
        } else {
            entity.setApiKey(exist == null ? null : exist.getApiKey());
        }
        if (exist == null) {
            entity.setId(null);
            if (StrUtil.isBlank(entity.getStatus())) {
                entity.setStatus("0");
            }
            return paymentConfigMapper.insert(entity);
        }
        entity.setId(exist.getId());
        return paymentConfigMapper.updateById(entity);
    }

    @Override
    public String getMode() {
        TenantPaymentConfig config = paymentConfigMapper.selectOne(
            Wrappers.<TenantPaymentConfig>lambdaQuery().last("limit 1"));
        if (config != null && StrUtil.isNotBlank(config.getMode())) {
            return config.getMode();
        }
        return MODE_PLATFORM_PROXY;
    }
}
