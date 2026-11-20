package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.system.domain.PageBlockConfig;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.bo.PageBlockConfigBo;
import com.ruoyi.system.domain.bo.PageDecorateBo;
import com.ruoyi.system.domain.vo.PageBlockConfigVo;
import com.ruoyi.system.domain.vo.RoleTemplateVo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import com.ruoyi.system.mapper.PageBlockConfigMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IPageBlockConfigService;
import com.ruoyi.system.service.IRoleTemplateService;
import com.ruoyi.system.service.IUserTenantRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 页面区块配置服务实现（T15.16 模块化页面架构 + T15.17 页面装修 + T15.30 三层控制模型）
 * <p>page_block_config 租户级表（多租户插件自动隔离），tenant_id=0 为「系统默认模板」。
 * <p>★T15.30 三层控制模型（天花板 ∩ 排版 = 最终渲染）：
 * <ol>
 *   <li>第①层【权限天花板】role_template（version_type × role_code → block_keys）决定“理论上能有哪些区块”，
 *       lite 档天花板低、pro 档天花板高，由 admin 维护；</li>
 *   <li>第②层【页面装修】page_block_config 由老板在天花板范围内勾选/排序/显隐（T15.17），只改排版不造权限；</li>
 *   <li>第③层【最终渲染】{@link #getPageConfig} 返回 = 天花板 block_keys ∩ page_block_config 配置 ∩ 该角色 perms。</li>
 * </ol>
 * <p>★核心安全底线：老板装修勾选了但天花板未授权的区块（如 lite 档顾客勾选“预约”区块）一律剔除，
 * 「个性化排版」永不突破「权限天花板」；升级 lite→pro 后天花板抬高，装修区自动多出新区块可勾选，历史配置保留。
 * <p>跨租户读取 tenant_id=0 模板用 {@link TenantHelper#supply}（临时置租户上下文），
 * 并对超管（免租户过滤）显式 eq tenant_id 双重兜底。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PageBlockConfigServiceImpl implements IPageBlockConfigService {

    /** 系统默认模板租户号（tenant_id=0） */
    private static final Long TEMPLATE_TENANT = 0L;
    /** 小微版区块来源 */
    private static final String MODULE_MICRO = "micro";
    /** 显示标志：1 显示 */
    private static final int SHOW = 1;
    /** block_params 中声明区块所需权限的键 */
    private static final String PARAM_PERM = "perm";

    private final PageBlockConfigMapper pageBlockConfigMapper;
    private final IUserTenantRoleService userTenantRoleService;
    /** T15.30 第①层天花板数据源：按 version_type + role_code 取 role_template.block_keys */
    private final IRoleTemplateService roleTemplateService;
    /** 解析本租户 version_type（天花板高低取决于分档），用 Mapper 避免与租户初始化服务链构成循环依赖 */
    private final SysTenantMapper tenantMapper;

    @Override
    public List<PageBlockConfigVo> getPageConfig(String pageKey) {
        if (StrUtil.isBlank(pageKey)) {
            return new ArrayList<>();
        }
        Long userId = LoginHelper.getUserId();
        Long tenantId = LoginHelper.getTenantId();
        // 解析当前用户在本租户下的主标准角色（boss/salesman/customer）
        String roleCode = resolvePrimaryRoleCode(userId);
        if (StrUtil.isBlank(roleCode)) {
            return new ArrayList<>();
        }
        // 本租户该角色该页面的显示区块（micro、show_flag=1、有序），租户隔离由插件自动完成
        List<PageBlockConfigVo> blocks = pageBlockConfigMapper.selectVoList(runtimeWrapper(roleCode, pageKey));
        // 本租户尚未装修（未克隆模板）→ 回落 tenant_id=0 系统默认模板（跨租户读取）
        if (CollUtil.isEmpty(blocks) && tenantId != null && !tenantId.equals(TEMPLATE_TENANT)) {
            blocks = TenantHelper.supply(TEMPLATE_TENANT, () -> pageBlockConfigMapper.selectVoList(
                runtimeWrapper(roleCode, pageKey).eq(PageBlockConfig::getTenantId, TEMPLATE_TENANT)));
        }
        // ★T15.30 第①层天花板 ∩ 第②层排版：剔除老板装修勾选了但 role_template 未授权（超天花板）的区块
        blocks = filterByCeiling(blocks, resolveBlockCeiling(tenantId, roleCode));
        // 配置 ∩ 权限：剔除声明了 perm 但当前用户无权限的区块
        return filterByPerms(blocks);
    }

    @Override
    public List<PageBlockConfigVo> listByRolePage(String roleCode, String pageKey, Long tenantId) {
        LambdaQueryWrapper<PageBlockConfig> w = Wrappers.<PageBlockConfig>lambdaQuery()
            .eq(StrUtil.isNotBlank(roleCode), PageBlockConfig::getRoleCode, roleCode)
            .eq(StrUtil.isNotBlank(pageKey), PageBlockConfig::getPageKey, pageKey)
            .orderByAsc(PageBlockConfig::getSortOrder)
            .orderByAsc(PageBlockConfig::getId);
        // 超管免租户过滤，须显式限定租户（默认系统模板 tenant_id=0）；非超管由插件自动隔离本租户
        if (LoginHelper.isAdmin()) {
            w.eq(PageBlockConfig::getTenantId, tenantId != null ? tenantId : TEMPLATE_TENANT);
        }
        return pageBlockConfigMapper.selectVoList(w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveConfig(PageDecorateBo bo) {
        String roleCode = bo.getRoleCode();
        String pageKey = bo.getPageKey();
        String moduleType = StrUtil.isNotBlank(bo.getModuleType()) ? bo.getModuleType() : MODULE_MICRO;
        // 目标租户：超管可指定（默认系统模板0），非超管=null（交给 insertFill + 插件隔离本租户）
        Long targetTenant = resolveTargetTenant(bo.getTenantId());
        // 全量覆盖：先删该 (租户,角色,页面,来源) 的旧区块
        LambdaQueryWrapper<PageBlockConfig> del = Wrappers.<PageBlockConfig>lambdaQuery()
            .eq(PageBlockConfig::getRoleCode, roleCode)
            .eq(PageBlockConfig::getPageKey, pageKey)
            .eq(PageBlockConfig::getModuleType, moduleType);
        if (targetTenant != null) {
            del.eq(PageBlockConfig::getTenantId, targetTenant);
        }
        pageBlockConfigMapper.delete(del);
        // 后插新清单（按提交顺序落 sort_order）
        List<PageBlockConfigBo> blocks = bo.getBlocks();
        if (CollUtil.isEmpty(blocks)) {
            return 0;
        }
        int order = 0;
        for (PageBlockConfigBo b : blocks) {
            if (StrUtil.isBlank(b.getBlockKey())) {
                continue;
            }
            PageBlockConfig e = new PageBlockConfig();
            e.setRoleCode(roleCode);
            e.setPageKey(pageKey);
            e.setBlockKey(b.getBlockKey());
            e.setModuleType(moduleType);
            e.setSortOrder(b.getSortOrder() != null ? b.getSortOrder() : order);
            e.setShowFlag(b.getShowFlag() != null ? b.getShowFlag() : SHOW);
            e.setBlockParams(b.getBlockParams());
            if (targetTenant != null) {
                // 超管跨租户写入：显式指定 tenantId，绕过 insertFill（登录上下文无该租户）
                e.setTenantId(targetTenant);
            }
            pageBlockConfigMapper.insert(e);
            order++;
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetDefault(String roleCode, String pageKey, Long tenantId) {
        if (StrUtil.isBlank(roleCode) || StrUtil.isBlank(pageKey)) {
            throw new ServiceException("角色编码与页面标识不能为空");
        }
        // 目标租户：超管可指定，否则取当前登录租户
        Long targetTenant = (LoginHelper.isAdmin() && tenantId != null) ? tenantId : LoginHelper.getTenantId();
        if (targetTenant == null) {
            throw new ServiceException("无法确定目标租户，恢复默认失败");
        }
        if (targetTenant.equals(TEMPLATE_TENANT)) {
            throw new ServiceException("系统默认模板（tenant_id=0）无需恢复");
        }
        // 1. 跨租户读取系统默认模板（tenant_id=0），超管免过滤故显式 eq tenant_id=0
        List<PageBlockConfig> templates = TenantHelper.supply(TEMPLATE_TENANT, () ->
            pageBlockConfigMapper.selectList(Wrappers.<PageBlockConfig>lambdaQuery()
                .eq(PageBlockConfig::getTenantId, TEMPLATE_TENANT)
                .eq(PageBlockConfig::getRoleCode, roleCode)
                .eq(PageBlockConfig::getPageKey, pageKey)
                .orderByAsc(PageBlockConfig::getSortOrder)));
        final Long tt = targetTenant;
        // 2. 目标租户上下文内：先删该角色该页面配置，再克隆模板副本（tenantId 显式置目标租户）
        return TenantHelper.supply(tt, () -> {
            pageBlockConfigMapper.delete(Wrappers.<PageBlockConfig>lambdaQuery()
                .eq(PageBlockConfig::getTenantId, tt)
                .eq(PageBlockConfig::getRoleCode, roleCode)
                .eq(PageBlockConfig::getPageKey, pageKey));
            int count = 0;
            for (PageBlockConfig t : templates) {
                PageBlockConfig e = new PageBlockConfig();
                e.setTenantId(tt);
                e.setRoleCode(roleCode);
                e.setPageKey(pageKey);
                e.setBlockKey(t.getBlockKey());
                e.setModuleType(t.getModuleType());
                e.setSortOrder(t.getSortOrder());
                e.setShowFlag(t.getShowFlag());
                e.setBlockParams(t.getBlockParams());
                pageBlockConfigMapper.insert(e);
                count++;
            }
            return count;
        });
    }

    @Override
    public Long insert(PageBlockConfigBo bo) {
        PageBlockConfig e = BeanCopyUtils.copy(bo, PageBlockConfig.class);
        e.setId(null);
        if (StrUtil.isBlank(e.getModuleType())) {
            e.setModuleType(MODULE_MICRO);
        }
        if (e.getShowFlag() == null) {
            e.setShowFlag(SHOW);
        }
        if (e.getSortOrder() == null) {
            e.setSortOrder(0);
        }
        // 超管登录上下文无租户（getTenantId()=null）→ insertFill 不填 → DB DEFAULT 0 落系统模板；老板 → 自动填本租户
        pageBlockConfigMapper.insert(e);
        return e.getId();
    }

    @Override
    public int update(PageBlockConfigBo bo) {
        PageBlockConfig e = BeanCopyUtils.copy(bo, PageBlockConfig.class);
        return pageBlockConfigMapper.updateById(e);
    }

    @Override
    public PageBlockConfigVo queryById(Long id) {
        return pageBlockConfigMapper.selectVoById(id);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return pageBlockConfigMapper.deleteBatchIds(ids);
    }

    /**
     * 运行时区块查询条件：指定角色 + 页面 + micro 来源 + 显示，按 sort_order 升序
     */
    private LambdaQueryWrapper<PageBlockConfig> runtimeWrapper(String roleCode, String pageKey) {
        return Wrappers.<PageBlockConfig>lambdaQuery()
            .eq(PageBlockConfig::getRoleCode, roleCode)
            .eq(PageBlockConfig::getPageKey, pageKey)
            .eq(PageBlockConfig::getModuleType, MODULE_MICRO)
            .eq(PageBlockConfig::getShowFlag, SHOW)
            .orderByAsc(PageBlockConfig::getSortOrder)
            .orderByAsc(PageBlockConfig::getId);
    }

    /**
     * ★T15.30 第①层权限天花板：按本租户 version_type + 主标准角色取 role_template.block_keys 授权的区块集。
     * <p>role_template 为系统级共享配方，version_type 决定天花板高低（lite 低、pro 高）；
     * 未配置模板或 block_keys 为空 → 返回空集，表示不设天花板限制（向后兼容，仅靠第②层 perms 过滤）。
     */
    private Set<String> resolveBlockCeiling(Long tenantId, String roleCode) {
        if (tenantId == null || StrUtil.isBlank(roleCode)) {
            return Collections.emptySet();
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || StrUtil.isBlank(tenant.getVersionType())) {
            return Collections.emptySet();
        }
        RoleTemplateVo template = roleTemplateService.getByVersionAndRole(tenant.getVersionType(), roleCode);
        if (template == null || StrUtil.isBlank(template.getBlockKeys())) {
            return Collections.emptySet();
        }
        try {
            List<String> keys = JsonUtils.parseArray(template.getBlockKeys(), String.class);
            return CollUtil.isEmpty(keys) ? Collections.emptySet() : new HashSet<>(keys);
        } catch (Exception e) {
            log.warn("role_template.block_keys 解析失败，跳过天花板过滤：versionType={}, roleCode={}",
                tenant.getVersionType(), roleCode, e);
            return Collections.emptySet();
        }
    }

    /**
     * ★T15.30 天花板过滤（第①层 ∩ 第②层）：天花板非空时，仅保留 role_template 授权的区块；
     * 老板装修勾选了但天花板未授权的区块（如 lite 档顾客勾选“预约”区块）一律剔除。天花板为空则不过滤（向后兼容）。
     */
    private List<PageBlockConfigVo> filterByCeiling(List<PageBlockConfigVo> blocks, Set<String> ceiling) {
        if (CollUtil.isEmpty(blocks) || CollUtil.isEmpty(ceiling)) {
            return blocks;
        }
        List<PageBlockConfigVo> result = new ArrayList<>(blocks.size());
        for (PageBlockConfigVo b : blocks) {
            if (ceiling.contains(b.getBlockKey())) {
                result.add(b);
            }
        }
        return result;
    }

    /**
     * 配置 ∩ 权限二次过滤：block_params 声明了 perm 且当前用户无该权限 → 剔除；未声明 perm 的区块一律可见。
     * 超管拥有全部权限（*:*:*），不过滤。
     */
    private List<PageBlockConfigVo> filterByPerms(List<PageBlockConfigVo> blocks) {
        if (CollUtil.isEmpty(blocks) || LoginHelper.isAdmin()) {
            return blocks;
        }
        Set<String> perms = LoginHelper.getLoginUser().getMenuPermission();
        if (perms == null) {
            perms = Collections.emptySet();
        }
        List<PageBlockConfigVo> result = new ArrayList<>(blocks.size());
        for (PageBlockConfigVo b : blocks) {
            String requiredPerm = extractPerm(b.getBlockParams());
            if (StrUtil.isBlank(requiredPerm) || perms.contains(requiredPerm)) {
                result.add(b);
            }
        }
        return result;
    }

    /**
     * 从 block_params(JSON) 解析区块所需权限标识 perm（无则返回 null）
     */
    private String extractPerm(String blockParams) {
        if (StrUtil.isBlank(blockParams)) {
            return null;
        }
        Dict dict = JsonUtils.parseMap(blockParams);
        if (dict == null) {
            return null;
        }
        Object perm = dict.get(PARAM_PERM);
        return perm == null ? null : perm.toString();
    }

    /**
     * 解析当前用户在本租户下的主标准角色编码：优先 is_primary=1，否则取第一个启用（status!=1）角色
     */
    private String resolvePrimaryRoleCode(Long userId) {
        if (userId == null) {
            return null;
        }
        List<UserTenantRoleVo> roles = userTenantRoleService.listByUser(userId);
        if (CollUtil.isEmpty(roles)) {
            return null;
        }
        List<UserTenantRoleVo> enabled = new ArrayList<>();
        for (UserTenantRoleVo r : roles) {
            if (!"1".equals(r.getStatus())) {
                enabled.add(r);
            }
        }
        if (CollUtil.isEmpty(enabled)) {
            return null;
        }
        for (UserTenantRoleVo r : enabled) {
            if (Integer.valueOf(1).equals(r.getIsPrimary())) {
                return r.getRoleCode();
            }
        }
        return enabled.get(0).getRoleCode();
    }

    /**
     * 目标租户解析：仅超管可跨租户指定（默认系统模板0）；非超管返回 null（强制本租户，交给 insertFill + 插件隔离）
     */
    private Long resolveTargetTenant(Long requested) {
        if (LoginHelper.isAdmin()) {
            return requested != null ? requested : TEMPLATE_TENANT;
        }
        return null;
    }
}
