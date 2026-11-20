package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.domain.PortalAccessLog;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.UserTenantBind;
import com.ruoyi.system.domain.vo.MicroSceneVo;
import com.ruoyi.system.domain.vo.PortalStoreVo;
import com.ruoyi.system.domain.vo.PortalTenantVo;
import com.ruoyi.system.mapper.PortalAccessLogMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.UserTenantBindMapper;
import com.ruoyi.system.service.IPortalService;
import com.ruoyi.system.service.TenantStatusChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 引导态门户服务实现（T13.1 / T13.4 / T8.8 审计 / T8.10 门店搜索）
 * <p>引导态处于未登录上下文（tenantId=null），多租户插件不过滤，故可跨租户按 openid/mpAppId/关键字反查；
 * 全程只读，绝不写 TenantContext。sys_tenant 本身在租户忽略表内。
 * <p>每次反查写入 portal_access_log（系统级共享表，无 tenant_id）：来源IP/端点/入参/openid/命中租户/结果，
 * 供安全审查（检测篡改 ?tenant= 探测、批量爬取公开展示数据）。审计写入失败不影响主流程。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortalServiceImpl implements IPortalService {

    private final UserTenantBindMapper userTenantBindMapper;
    private final SysTenantMapper tenantMapper;
    private final PortalAccessLogMapper accessLogMapper;
    /** 租户状态校验器（T29.2/T29.3 复用登录同源的存在/停用/到期拦截逻辑） */
    private final TenantStatusChecker tenantStatusChecker;

    /** 正常状态租户 */
    private static final String STATUS_NORMAL = "NORMAL";
    /** 门店搜索默认返回条数 */
    private static final int DEFAULT_SEARCH_LIMIT = 50;
    /** 门店搜索最大返回条数 */
    private static final int MAX_SEARCH_LIMIT = 200;
    /** 就近排序时候选集上限（内存排序后截断） */
    private static final int GEO_CANDIDATE_CAP = 500;
    /** 地球平均半径（公里），haversine 距离计算 */
    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public List<PortalTenantVo> listTenantsByOpenid(String openid) {
        if (StrUtil.isBlank(openid)) {
            recordAccess(null, null, false, "openid 为空");
            return Collections.emptyList();
        }
        List<UserTenantBind> binds = userTenantBindMapper.selectList(
            Wrappers.<UserTenantBind>lambdaQuery()
                .eq(UserTenantBind::getOpenid, openid)
                .orderByDesc(UserTenantBind::getIsDefault)
                .orderByAsc(UserTenantBind::getId));
        if (CollUtil.isEmpty(binds)) {
            recordAccess(openid, null, false, "openid 无绑定门店");
            return Collections.emptyList();
        }
        List<Long> tenantIds = binds.stream().map(UserTenantBind::getTenantId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(tenantIds)) {
            recordAccess(openid, null, false, "绑定记录无有效租户");
            return Collections.emptyList();
        }
        Map<Long, SysTenant> tenantMap = tenantMapper.selectBatchIds(tenantIds).stream()
            .collect(Collectors.toMap(SysTenant::getId, Function.identity(), (a, b) -> a));
        List<PortalTenantVo> result = new ArrayList<>();
        for (UserTenantBind bind : binds) {
            SysTenant tenant = tenantMap.get(bind.getTenantId());
            if (tenant == null || !STATUS_NORMAL.equals(tenant.getStatus())) {
                continue;
            }
            result.add(toVo(tenant, bind.getIsDefault()));
        }
        recordAccess(openid, null, !result.isEmpty(), result.isEmpty() ? "无状态正常门店" : null);
        return result;
    }

    @Override
    public PortalTenantVo resolveByMpAppId(String mpAppId) {
        if (StrUtil.isBlank(mpAppId)) {
            recordAccess(null, null, false, "mpAppId 为空");
            return null;
        }
        SysTenant tenant = tenantMapper.selectOne(
            Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getMpAppId, mpAppId)
                .last("limit 1"));
        if (tenant == null || !STATUS_NORMAL.equals(tenant.getStatus())) {
            recordAccess(null, null, false, "mpAppId 未命中正常门店");
            return null;
        }
        recordAccess(null, tenant.getId(), true, null);
        return toVo(tenant, 1);
    }

    @Override
    public List<PortalStoreVo> searchStores(String keyword, String city, BigDecimal userLng, BigDecimal userLat, int limit) {
        int max = limit > 0 ? Math.min(limit, MAX_SEARCH_LIMIT) : DEFAULT_SEARCH_LIMIT;
        boolean geo = userLng != null && userLat != null;
        LambdaQueryWrapper<SysTenant> wrapper = Wrappers.<SysTenant>lambdaQuery()
            .eq(SysTenant::getStatus, STATUS_NORMAL)
            .eq(StrUtil.isNotBlank(city), SysTenant::getCity, city);
        if (StrUtil.isNotBlank(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(SysTenant::getCompanyName, kw).or().like(SysTenant::getTenantCode, kw));
        }
        // 就近排序需在内存按距离排序后截断，故放宽候选集；无定位时按名称排序并直接限量
        if (geo) {
            wrapper.last("limit " + GEO_CANDIDATE_CAP);
        } else {
            wrapper.orderByAsc(SysTenant::getCompanyName).last("limit " + max);
        }
        List<SysTenant> tenants = tenantMapper.selectList(wrapper);
        List<PortalStoreVo> list = tenants.stream().map(this::toStoreVo).collect(Collectors.toList());
        if (geo) {
            double uLat = userLat.doubleValue();
            double uLng = userLng.doubleValue();
            for (PortalStoreVo vo : list) {
                vo.setDistanceKm(haversineKm(uLat, uLng, vo.getLatitude(), vo.getLongitude()));
            }
            // 无坐标的门店排最后
            list.sort(Comparator.comparing(v -> v.getDistanceKm() == null
                ? BigDecimal.valueOf(Double.MAX_VALUE) : v.getDistanceKm()));
            if (list.size() > max) {
                list = new ArrayList<>(list.subList(0, max));
            }
        }
        recordAccess(null, null, true, null);
        return list;
    }

    @Override
    public List<PortalStoreVo> listStoresByTenantNo(String tenantNo) {
        // 校验通过后返回门店公开信息；本系统租户即门店，故为单元素列表（前端仍需主动点选）
        return Collections.singletonList(resolveTenantNo(tenantNo));
    }

    @Override
    public PortalStoreVo resolveTenantNo(String tenantNo) {
        if (StrUtil.isBlank(tenantNo)) {
            recordAccess(null, null, false, "tenantNo 为空");
            throw new ServiceException("请输入租户号");
        }
        String no = tenantNo.trim();
        // ★优先按 tenant_code 精确匹配（如 T杰杰旅游社-7324）
        SysTenant tenant = tenantMapper.selectOne(
            Wrappers.<SysTenant>lambdaQuery()
                .eq(SysTenant::getTenantCode, no)
                .last("limit 1"));
        // ★降级：纯数字时按 tenant_id（主键）查找，兼容用户输入数字 ID 的场景
        if (tenant == null && no.matches("\\d+")) {
            try {
                tenant = tenantMapper.selectById(Long.parseLong(no));
            } catch (NumberFormatException e) {
                // 数值溢出，忽略
            }
        }
        // ★降级：按租户名称（company_name）精确匹配，Boss 可直接输门店名（如「杰杰旅行社」）代替数字租户号；页面不做可输名称的提示
        if (tenant == null) {
            tenant = tenantMapper.selectOne(
                Wrappers.<SysTenant>lambdaQuery()
                    .eq(SysTenant::getCompanyName, no)
                    .last("limit 1"));
        }
        // ★再降级：租户名称模糊匹配且仅唯一命中时采用，容忍「杰杰旅行社」vs「杰杰旅行社有限公司」类差异；
        //   命中多条视为未命中（走下方统一报错），避免相似名门店误选
        if (tenant == null) {
            List<SysTenant> hits = tenantMapper.selectList(
                Wrappers.<SysTenant>lambdaQuery()
                    .like(SysTenant::getCompanyName, no)
                    .last("limit 2"));
            if (hits.size() == 1) {
                tenant = hits.get(0);
            }
        }
        if (tenant == null) {
            recordAccess(null, null, false, "租户号未命中：" + StrUtil.maxLength(no, 100));
            throw new ServiceException("租户不存在或已停用，请确认租户号是否正确");
        }
        // 状态/到期校验（复用登录同源拦截：停用/到期/待启用抛业务异常）
        tenantStatusChecker.checkTenant(tenant.getId());
        recordAccess(null, tenant.getId(), true, null);
        return toStoreVo(tenant);
    }

    @Override
    public MicroSceneVo resolveScene(String scene) {
        if (StrUtil.isBlank(scene)) {
            recordAccess(null, null, false, "scene 为空");
            throw new ServiceException("二维码无效");
        }
        Map<String, String> params = parseScene(scene);
        Long tenantId = parseLong(params.get("t"));
        if (tenantId == null) {
            recordAccess(null, null, false, "scene 缺少合法租户标识");
            throw new ServiceException("二维码无效");
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            recordAccess(null, tenantId, false, "scene 租户不存在");
            throw new ServiceException("二维码无效：租户不存在");
        }
        // 状态/到期校验（复用登录同源拦截，非法抛业务异常）
        tenantStatusChecker.checkTenant(tenantId);
        // 门店归属校验：本系统租户即门店，s 存在时必须与 t 一致，否则视为跨租户伪造
        Long storeId = tenantId;
        String s = params.get("s");
        if (StrUtil.isNotBlank(s)) {
            Long parsedStore = parseLong(s);
            if (parsedStore == null || !parsedStore.equals(tenantId)) {
                recordAccess(null, tenantId, false, "scene 门店与租户不匹配");
                throw new ServiceException("二维码无效：门店不属于该租户");
            }
            storeId = parsedStore;
        }
        MicroSceneVo vo = new MicroSceneVo();
        vo.setTenantId(tenantId);
        vo.setTenantCode(tenant.getTenantCode());
        vo.setTenantName(tenant.getCompanyName());
        vo.setStoreId(storeId);
        vo.setRole(params.get("r"));
        vo.setLogoUrl(tenant.getLogoUrl());
        vo.setCity(tenant.getCity());
        recordAccess(null, tenantId, true, null);
        return vo;
    }

    /**
     * 解析 scene 字符串（{@code t=1&s=1&r=customer}）为键值对；空段/无等号段忽略。
     */
    private Map<String, String> parseScene(String scene) {
        Map<String, String> map = new HashMap<>(4);
        for (String seg : scene.split("&")) {
            if (StrUtil.isBlank(seg)) {
                continue;
            }
            int idx = seg.indexOf('=');
            if (idx > 0) {
                map.put(seg.substring(0, idx).trim(), seg.substring(idx + 1).trim());
            }
        }
        return map;
    }

    /**
     * 安全解析 Long（非数字/空返回 null，避免 NumberFormatException 泄漏为系统异常）。
     */
    private Long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private PortalTenantVo toVo(SysTenant tenant, Integer isDefault) {
        PortalTenantVo vo = new PortalTenantVo();
        vo.setTenantId(tenant.getId());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setCompanyName(tenant.getCompanyName());
        vo.setAccessChannel(tenant.getAccessChannel());
        vo.setBizCode(tenant.getBizCode());
        vo.setIsDefault(isDefault);
        vo.setStatus(tenant.getStatus());
        return vo;
    }

    /**
     * 门店公开展示视图（仅公开字段，绝不含 contact_phone / wecom_secret / mp_app_id 等敏感凭据）
     */
    private PortalStoreVo toStoreVo(SysTenant tenant) {
        PortalStoreVo vo = new PortalStoreVo();
        vo.setTenantId(tenant.getId());
        vo.setTenantCode(tenant.getTenantCode());
        vo.setCompanyName(tenant.getCompanyName());
        vo.setCity(tenant.getCity());
        vo.setAddress(tenant.getAddress());
        vo.setLongitude(tenant.getLongitude());
        vo.setLatitude(tenant.getLatitude());
        vo.setLogoUrl(tenant.getLogoUrl());
        vo.setStoreIntro(tenant.getStoreIntro());
        vo.setBizCode(tenant.getBizCode());
        vo.setAccessChannel(tenant.getAccessChannel());
        return vo;
    }

    /**
     * haversine 球面距离（公里，保留 2 位）；门店无坐标返回 null（排序时置末）
     */
    private BigDecimal haversineKm(double userLat, double userLng, BigDecimal storeLat, BigDecimal storeLng) {
        if (storeLat == null || storeLng == null) {
            return null;
        }
        double sLat = storeLat.doubleValue();
        double sLng = storeLng.doubleValue();
        double dLat = Math.toRadians(sLat - userLat);
        double dLng = Math.toRadians(sLng - userLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(sLat))
            * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(EARTH_RADIUS_KM * c).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 写引导态访问审计（T8.8）：来源IP/端点/入参取自当前请求上下文；写入失败仅告警，不影响主流程
     */
    private void recordAccess(String openid, Long resolvedTenantId, boolean success, String errorMsg) {
        try {
            PortalAccessLog entity = new PortalAccessLog();
            HttpServletRequest request = ServletUtils.getRequest();
            if (request != null) {
                entity.setClientIp(ServletUtils.getClientIP());
                entity.setEndpoint(StrUtil.maxLength(request.getRequestURI(), 190));
                entity.setRequestParam(StrUtil.maxLength(request.getQueryString(), 1900));
            }
            entity.setOpenid(StrUtil.maxLength(openid, 100));
            entity.setResolvedTenantId(resolvedTenantId);
            entity.setSuccess(success ? 1 : 0);
            entity.setErrorMsg(StrUtil.maxLength(errorMsg, 490));
            accessLogMapper.insert(entity);
        } catch (Exception e) {
            log.warn("[Portal] 引导态访问审计写入失败 openid={} tenantId={}", openid, resolvedTenantId, e);
        }
    }
}
