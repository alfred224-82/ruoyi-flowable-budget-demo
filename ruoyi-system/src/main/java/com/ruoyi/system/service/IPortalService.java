package com.ruoyi.system.service;

import com.ruoyi.system.domain.vo.MicroSceneVo;
import com.ruoyi.system.domain.vo.PortalStoreVo;
import com.ruoyi.system.domain.vo.PortalTenantVo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 引导态门户服务接口（T13.1 公众号总入口 / T13.4 端适配 / T8.8 审计 / T8.10 门店搜索）
 * <p>双阶段租户识别之【引导态】：仅按 openid / 公众号 appId / 关键字反查可选门店，绝不写 TenantContext；
 * 用户显式选定门店后进入【鉴权态】，由登录流程签发携带 tenant_id 的 JWT。
 * <p>所有反查均写入 portal_access_log 审计（T8.8），只返回公开展示字段。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IPortalService {

    /**
     * 按 openid 反查该用户可进入的门店（租户）列表（仅状态正常的租户）
     *
     * @param openid 微信 openid
     * @return 门店列表（默认门店排前）
     */
    List<PortalTenantVo> listTenantsByOpenid(String openid);

    /**
     * 按公众号 AppID 解析归属租户（公众号总入口：一个公众号对应一个主体/租户）
     *
     * @param mpAppId 微信公众号 AppID
     * @return 门店信息，无匹配返回 null
     */
    PortalTenantVo resolveByMpAppId(String mpAppId);

    /**
     * 门店搜索（T8.10 门店选择/搜索首页）：关键字（企业名/租户编码）+ 城市过滤，仅返回状态正常租户的公开展示字段；
     * 提供用户经纬度时按距离就近排序并回填 distanceKm（定位授权），否则按名称排序（定位拒绝时降级为城市选择）。
     *
     * @param keyword 关键字（可空，模糊匹配企业名/租户编码）
     * @param city    城市（可空，定位授权拒绝时的降级过滤）
     * @param userLng 用户当前经度（可空）
     * @param userLat 用户当前纬度（可空）
     * @param limit   返回条数上限（&lt;=0 取默认 50，最大 200）
     * @return 门店公开列表
     */
    List<PortalStoreVo> searchStores(String keyword, String city, BigDecimal userLng, BigDecimal userLat, int limit);

    /**
     * T29.2 按租户号查询门店列表（Boss 手动输租户号登录，匿名端点）：
     * 校验租户号合法性（存在/未停用/未过期）后返回其门店公开展示信息；非法租户号抛 {@code ServiceException}。
     * <p>本系统租户即门店（sys_tenant 含企业名/城市/地址），故通常返回单元素列表；
     * 前端门店列表页需主动点选，不做自动跳转。
     *
     * @param tenantNo 平台提供的租户号（sys_tenant.tenant_code）
     * @return 门店公开列表
     */
    List<PortalStoreVo> listStoresByTenantNo(String tenantNo);

    /**
     * T29.2 租户号合法性校验（匿名端点）：命中且状态正常/未到期返回门店公开信息，
     * 未命中/停用/到期抛 {@code ServiceException}（供前端提示「租户不存在或已停用」）。
     *
     * @param tenantNo 平台提供的租户号（sys_tenant.tenant_code）
     * @return 门店公开信息
     */
    PortalStoreVo resolveTenantNo(String tenantNo);

    /**
     * T29.3 扫码 scene 解析 + 校验（员工/顾客扫码带租户直接登录，匿名端点）：
     * scene 编码 {@code t={tenantId}&s={storeId}&r={role}}；校验租户存在/未停用/未过期、门店归属该租户，
     * 非法抛 {@code ServiceException}（前端提示「二维码无效」并回退租户号输入页）。
     *
     * @param scene 二维码 scene 字符串（前端已 decodeURIComponent）
     * @return scene 解析结果（tenantId/storeId/role + 门店公开信息）
     */
    MicroSceneVo resolveScene(String scene);
}
