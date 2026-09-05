package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.OpenApiWebhook;
import com.ruoyi.system.domain.bo.OpenApiWebhookBo;
import com.ruoyi.system.domain.vo.OpenApiWebhookVo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 开放平台Webhook回调注册Service接口（T7.8 / FEAT_OPEN_API）
 * <p>Webhook 配置管理：CRUD + secret 加密/脱敏 + 启停；secret 仅创建/重置时一次性明文返回。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IOpenApiWebhookService {

    /**
     * Webhook 分页查询（secret 脱敏，填充关联凭证 AppKey/应用名）
     */
    TableDataInfo<OpenApiWebhookVo> queryPageList(Long credentialId, String eventType, PageQuery pageQuery);

    /**
     * 按凭证查询 Webhook 列表（secret 脱敏）
     */
    List<OpenApiWebhookVo> listByCredential(Long credentialId);

    /**
     * Webhook 详情（secret 脱敏）
     */
    OpenApiWebhookVo queryById(Long id);

    /**
     * 新建 Webhook：生成 secret（加密存储）
     *
     * @return {id, secret} —— secret 仅此一次明文返回
     */
    Map<String, String> create(OpenApiWebhookBo bo);

    /**
     * 编辑 Webhook（不含 secret 重置）
     */
    int update(OpenApiWebhookBo bo);

    /**
     * 重置 secret（加密存储）
     *
     * @return 新的明文 secret（仅此一次返回）
     */
    String resetSecret(Long id);

    /**
     * 删除（可批量）
     */
    int deleteByIds(Collection<Long> ids);

    /**
     * 查询指定租户、指定事件下已启用的 Webhook（供事件派发器调用）
     * <p>显式按 tenantId 过滤，不依赖登录态多租户插件（异步派发无登录上下文）。
     *
     * @param tenantId  租户ID
     * @param eventType 事件类型
     * @return 已启用的 Webhook 实体列表（secret 为密文，由派发器解密后验签）
     */
    List<OpenApiWebhook> listEnabledForEvent(Long tenantId, String eventType);
}
