package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.OpenApiCredentialBo;
import com.ruoyi.system.domain.vo.OpenApiCallLogVo;
import com.ruoyi.system.domain.vo.OpenApiCredentialVo;
import com.ruoyi.system.domain.vo.OpenApiVerifyResult;

import java.util.Collection;
import java.util.Map;

/**
 * 开放平台API服务接口（T13.5 AppKey+签名鉴权 / 配额 / 全量审计）
 * <p>appSecret 加密存储、接口返回脱敏，仅创建时一次性返回明文；调用审计写 open_api_call_log。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IOpenApiCredentialService {

    /**
     * 凭证分页查询（appSecret 脱敏）
     */
    TableDataInfo<OpenApiCredentialVo> queryPageList(String appName, String status, PageQuery pageQuery);

    /**
     * 凭证详情（appSecret 脱敏）
     */
    OpenApiCredentialVo queryById(Long id);

    /**
     * 新建凭证：生成 appKey + appSecret（加密存储）
     *
     * @return {appKey, appSecret} —— appSecret 仅此一次明文返回
     */
    Map<String, String> create(OpenApiCredentialBo bo);

    /**
     * 编辑凭证（不含 appSecret 重置）
     */
    int update(OpenApiCredentialBo bo);

    /**
     * 重置 appSecret（加密存储）
     *
     * @return 新的明文 appSecret（仅此一次返回）
     */
    String resetSecret(Long id);

    /**
     * 启停凭证
     */
    int changeStatus(Long id, String status);

    /**
     * 逻辑删除（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * 校验签名（HMAC-SHA256）+ 时间戳防重放 + 状态/过期/IP白名单/日月配额校验；无论通过与否均写调用审计
     *
     * @param appKey       AppKey
     * @param timestamp    请求时间戳(毫秒)
     * @param nonce        随机串（防重放）
     * @param signature    客户端签名
     * @param apiPath      接口路径
     * @param httpMethod   HTTP方法
     * @param requestParam 业务请求参数(脱敏，写入审计)
     * @param clientIp     来源IP
     * @return 鉴权结果（pass/code/message/credential）
     */
    OpenApiVerifyResult verifyAndLog(String appKey, String timestamp, String nonce, String signature,
                                     String apiPath, String httpMethod, String requestParam, String clientIp);

    /**
     * 调用审计分页查询
     */
    TableDataInfo<OpenApiCallLogVo> queryCallLogPageList(String appKey, PageQuery pageQuery);
}
