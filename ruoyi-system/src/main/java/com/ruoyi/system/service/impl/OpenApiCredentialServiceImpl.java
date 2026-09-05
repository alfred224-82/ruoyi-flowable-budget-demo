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
import com.ruoyi.system.domain.OpenApiCallLog;
import com.ruoyi.system.domain.OpenApiCredential;
import com.ruoyi.system.domain.bo.OpenApiCredentialBo;
import com.ruoyi.system.domain.vo.OpenApiCallLogVo;
import com.ruoyi.system.domain.vo.OpenApiCredentialVo;
import com.ruoyi.system.domain.vo.OpenApiVerifyResult;
import com.ruoyi.system.mapper.OpenApiCallLogMapper;
import com.ruoyi.system.mapper.OpenApiCredentialMapper;
import com.ruoyi.system.service.IOpenApiCredentialService;
import com.ruoyi.system.service.support.SecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放平台API服务实现（T7.8 / T13.5，专业版 FEAT_OPEN_API）
 * <p>appSecret 加密存储、返回脱敏，仅创建/重置时一次性明文返回；
 * verifyAndLog 在未鉴权上下文按 appKey 反查凭证（此时多租户插件不过滤），
 * 依次校验 状态/过期/时间戳防重放/IP白名单/日月配额/签名，无论通过与否均写全量调用审计。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiCredentialServiceImpl implements IOpenApiCredentialService {

    private final OpenApiCredentialMapper credentialMapper;
    private final OpenApiCallLogMapper callLogMapper;
    private final SecretCipher secretCipher;

    @Override
    public TableDataInfo<OpenApiCredentialVo> queryPageList(String appName, String status, PageQuery pageQuery) {
        LambdaQueryWrapper<OpenApiCredential> wrapper = Wrappers.<OpenApiCredential>lambdaQuery()
            .like(StrUtil.isNotBlank(appName), OpenApiCredential::getAppName, appName)
            .eq(StrUtil.isNotBlank(status), OpenApiCredential::getStatus, status)
            .orderByDesc(OpenApiCredential::getCreateTime);
        Page<OpenApiCredentialVo> page = credentialMapper.selectVoPage(pageQuery.build(), wrapper);
        page.getRecords().forEach(vo -> vo.setAppSecret(secretCipher.mask(vo.getAppSecret())));
        return TableDataInfo.build(page);
    }

    @Override
    public OpenApiCredentialVo queryById(Long id) {
        OpenApiCredentialVo vo = credentialMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("凭证不存在");
        }
        vo.setAppSecret(secretCipher.mask(vo.getAppSecret()));
        return vo;
    }

    @Override
    public Map<String, String> create(OpenApiCredentialBo bo) {
        String appKey = OpenApiSignUtils.generateAppKey();
        String plainSecret = OpenApiSignUtils.generateAppSecret();
        OpenApiCredential entity = BeanCopyUtils.copy(bo, OpenApiCredential.class);
        entity.setId(null);
        entity.setAppKey(appKey);
        entity.setAppSecret(secretCipher.encrypt(plainSecret));
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("ACTIVE");
        }
        if (entity.getQuotaDaily() == null) {
            entity.setQuotaDaily(10000);
        }
        if (entity.getQuotaMonthly() == null) {
            entity.setQuotaMonthly(200000);
        }
        credentialMapper.insert(entity);
        // appSecret 明文仅此一次返回，后续接口一律脱敏
        Map<String, String> result = new HashMap<>(4);
        result.put("appKey", appKey);
        result.put("appSecret", plainSecret);
        return result;
    }

    @Override
    public int update(OpenApiCredentialBo bo) {
        OpenApiCredential exist = credentialMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("凭证不存在");
        }
        OpenApiCredential entity = BeanCopyUtils.copy(bo, OpenApiCredential.class);
        // appKey/appSecret 不通过编辑接口变更
        entity.setAppKey(null);
        entity.setAppSecret(null);
        return credentialMapper.updateById(entity);
    }

    @Override
    public String resetSecret(Long id) {
        OpenApiCredential exist = credentialMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("凭证不存在");
        }
        String plainSecret = OpenApiSignUtils.generateAppSecret();
        OpenApiCredential entity = new OpenApiCredential();
        entity.setId(id);
        entity.setAppSecret(secretCipher.encrypt(plainSecret));
        credentialMapper.updateById(entity);
        return plainSecret;
    }

    @Override
    public int changeStatus(Long id, String status) {
        OpenApiCredential exist = credentialMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("凭证不存在");
        }
        OpenApiCredential entity = new OpenApiCredential();
        entity.setId(id);
        entity.setStatus(status);
        return credentialMapper.updateById(entity);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return credentialMapper.deleteBatchIds(ids);
    }

    @Override
    public OpenApiVerifyResult verifyAndLog(String appKey, String timestamp, String nonce, String signature,
                                            String apiPath, String httpMethod, String requestParam, String clientIp) {
        long start = System.currentTimeMillis();
        // 未鉴权上下文：多租户插件不追加 tenant_id，按全局唯一 appKey 反查凭证
        OpenApiCredential credential = StrUtil.isBlank(appKey) ? null : credentialMapper.selectOne(
            Wrappers.<OpenApiCredential>lambdaQuery().eq(OpenApiCredential::getAppKey, appKey).last("limit 1"));
        OpenApiVerifyResult result;
        if (credential == null) {
            result = OpenApiVerifyResult.fail(401, "无效的 AppKey");
        } else if ("DISABLED".equals(credential.getStatus())) {
            result = OpenApiVerifyResult.fail(403, "凭证已停用");
        } else if (credential.getExpireTime() != null && credential.getExpireTime().before(new Date())) {
            result = OpenApiVerifyResult.fail(403, "凭证已过期");
        } else if (!OpenApiSignUtils.isTimestampValid(timestamp, OpenApiSignUtils.DEFAULT_TIMEOUT_MS)) {
            result = OpenApiVerifyResult.fail(401, "请求时间戳无效或超出防重放窗口");
        } else if (!ipAllowed(credential.getIpWhitelist(), clientIp)) {
            result = OpenApiVerifyResult.fail(403, "来源IP不在白名单");
        } else if (overQuota(credential)) {
            result = OpenApiVerifyResult.fail(429, "调用超出配额限制");
        } else {
            String plainSecret = secretCipher.decrypt(credential.getAppSecret());
            boolean pass = OpenApiSignUtils.verify(plainSecret, credential.getAppKey(), timestamp, nonce, signature);
            result = pass ? OpenApiVerifyResult.ok(credential) : OpenApiVerifyResult.fail(401, "签名校验失败");
        }
        // 通过后刷新最近调用时间
        if (result.isPass()) {
            OpenApiCredential upd = new OpenApiCredential();
            upd.setId(credential.getId());
            upd.setLastCallTime(new Date());
            credentialMapper.updateById(upd);
        }
        writeCallLog(credential, appKey, apiPath, httpMethod, requestParam, clientIp, timestamp, nonce,
            result, System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public TableDataInfo<OpenApiCallLogVo> queryCallLogPageList(String appKey, PageQuery pageQuery) {
        LambdaQueryWrapper<OpenApiCallLog> wrapper = Wrappers.<OpenApiCallLog>lambdaQuery()
            .eq(StrUtil.isNotBlank(appKey), OpenApiCallLog::getAppKey, appKey)
            .orderByDesc(OpenApiCallLog::getCreateTime);
        Page<OpenApiCallLogVo> page = callLogMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    /**
     * IP 白名单校验（空=不限）
     */
    private boolean ipAllowed(String ipWhitelist, String clientIp) {
        if (StrUtil.isBlank(ipWhitelist)) {
            return true;
        }
        if (StrUtil.isBlank(clientIp)) {
            return false;
        }
        List<String> ips = StrUtil.split(ipWhitelist, ',');
        return ips.stream().map(String::trim).anyMatch(ip -> ip.equals(clientIp));
    }

    /**
     * 配额校验：日配额或月配额任一超限即拒绝（NULL 或 <=0 表示不限）
     */
    private boolean overQuota(OpenApiCredential credential) {
        return overLimit(credential.getAppKey(), credential.getQuotaDaily(), startOfToday())
            || overLimit(credential.getAppKey(), credential.getQuotaMonthly(), startOfMonth());
    }

    private boolean overLimit(String appKey, Integer quota, Date since) {
        if (quota == null || quota <= 0) {
            return false;
        }
        Long count = callLogMapper.selectCount(Wrappers.<OpenApiCallLog>lambdaQuery()
            .eq(OpenApiCallLog::getAppKey, appKey)
            .ge(OpenApiCallLog::getCreateTime, since));
        return count != null && count >= quota;
    }

    private Date startOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date startOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * 写全量调用审计（tenantId/credentialId 取自凭证；未鉴权上下文多租户插件不注入，故显式回填）
     */
    private void writeCallLog(OpenApiCredential credential, String appKey, String apiPath, String httpMethod,
                              String requestParam, String clientIp, String timestamp, String nonce,
                              OpenApiVerifyResult result, long costMs) {
        try {
            OpenApiCallLog entity = new OpenApiCallLog();
            if (credential != null) {
                entity.setTenantId(credential.getTenantId());
                entity.setCredentialId(credential.getId());
            }
            entity.setAppKey(appKey);
            entity.setApiPath(StrUtil.maxLength(apiPath, 190));
            entity.setHttpMethod(httpMethod);
            // 业务参数合并时间戳/nonce（便于防重放追溯），截断存储
            String param = StrUtil.format("timestamp={}&nonce={}&param={}",
                StrUtil.nullToEmpty(timestamp), StrUtil.nullToEmpty(nonce), StrUtil.nullToEmpty(requestParam));
            entity.setRequestParam(StrUtil.maxLength(param, 2000));
            entity.setResponseCode(result.getCode());
            entity.setCostMs(costMs);
            entity.setClientIp(clientIp);
            entity.setSuccess(result.isPass() ? 1 : 0);
            if (!result.isPass()) {
                entity.setErrorMsg(StrUtil.maxLength(result.getMessage(), 490));
            }
            callLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("[OpenApi] 调用审计写入失败 appKey={} path={}", appKey, apiPath, e);
        }
    }
}
