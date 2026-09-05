package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.UserTenantBindVo;

import java.util.List;

/**
 * 用户租户绑定服务接口（T10.1 / T13.1 双阶段租户识别 - 引导态）
 * <p>引导态（/portal/**）仅按 openid 反查可选门店列表，绝不写 TenantContext；
 * 用户显式选定门店后进入鉴权态，由 JWT 携带 tenant_id。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IUserTenantBindService {

    /**
     * 按 openid 查询该用户可进入的门店（租户）绑定列表，供登录页显式选定
     *
     * @param openid 微信 openid
     * @return 绑定列表（含 tenantId、isDefault）
     */
    List<UserTenantBindVo> listByOpenid(String openid);

    /**
     * 绑定 openid 到指定租户（幂等：已存在则更新 userId/isDefault）
     *
     * @param openid    微信 openid
     * @param tenantId  租户ID
     * @param userId    系统用户ID（可空，登录后回填）
     * @param isDefault 是否默认门店（0否 1是）
     * @return 影响行数
     */
    int bind(String openid, Long tenantId, Long userId, Integer isDefault);

    /**
     * 登录后回填 userId（引导态仅绑定 openid+tenantId，鉴权成功后补齐 userId）
     *
     * @param openid   微信 openid
     * @param tenantId 租户ID
     * @param userId   系统用户ID
     * @return 影响行数
     */
    int fillUserId(String openid, Long tenantId, Long userId);
}
