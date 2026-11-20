package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.TenantRoleMapping;
import com.ruoyi.system.domain.bo.TenantRoleMappingBo;
import com.ruoyi.system.domain.vo.TenantRoleMappingVo;
import com.ruoyi.system.domain.vo.IndustryTemplateVo;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.TenantRoleMappingMapper;
import com.ruoyi.system.service.IIndustryTemplateService;
import com.ruoyi.system.service.ITenantRoleMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户角色映射服务实现（T15.9 平台管理员角色映射管理）
 * <p>标准角色集来源 sys_role（全版本共用一张表，标准角色 tenant_id IS NULL）；映射持久化到 tenant_role_mapping（租户级自动隔离）。
 * ★平台管理员只能挑选/改名/启停/排序，不能新建角色 code（selectRoles 校验 role_code 必须在标准角色集内）、不能改角色权限。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantRoleMappingServiceImpl implements ITenantRoleMappingService {

    private final TenantRoleMappingMapper mappingMapper;
    private final SysRoleMapper roleMapper;
    private final SysTenantMapper tenantMapper;
    private final IIndustryTemplateService industryTemplateService;

    /** 角色产品线归属：双线共用（T15.25，与 M15_09 sys_role.tenant_type_scope 默认值一致） */
    private static final String SCOPE_BOTH = "both";

    @Override
    public TableDataInfo<TenantRoleMappingVo> queryPageList(TenantRoleMappingBo bo, PageQuery pageQuery) {
        // ★v1.30 自愈（v1.45 增强）：本租户缺失行业角色映射时，按 sys_tenant.industry 从 admin 行业角色映射（industry_role_map）补录落库
        selfHealFromIndustry();
        LambdaQueryWrapper<TenantRoleMapping> wrapper = Wrappers.<TenantRoleMapping>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getRoleCode()), TenantRoleMapping::getRoleCode, bo.getRoleCode())
            .like(StrUtil.isNotBlank(bo.getDisplayName()), TenantRoleMapping::getDisplayName, bo.getDisplayName())
            .eq(StrUtil.isNotBlank(bo.getStatus()), TenantRoleMapping::getStatus, bo.getStatus())
            .orderByAsc(TenantRoleMapping::getSortOrder)
            .orderByAsc(TenantRoleMapping::getId);
        Page<TenantRoleMappingVo> page = mappingMapper.selectVoPage(pageQuery.build(), wrapper);
        fillRoleName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<TenantRoleMappingVo> listStandardRoles() {
        return listOptionalRoles();
    }

    @Override
    public List<TenantRoleMappingVo> listOptionalRoles() {
        // ★T15.25 硬隔离：数据源层按本租户 tenant_type 过滤 scope，小微/企业租户互不可见对方产品线角色
        String tenantType = currentTenantType();
        // 全版本共享标准角色（sys_role.tenant_id IS NULL 且启用；del_flag 由 @TableLogic 自动过滤）
        List<SysRole> standardRoles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
            .isNull(SysRole::getTenantId)
            .eq(SysRole::getStatus, "0")
            .orderByAsc(SysRole::getRoleSort));
        // 本租户已挑选映射（tenant_role_mapping 自动按当前租户隔离）
        // ★同 role_code 多业务名（导游/司机）时按 (sort_order,id) 升序，toMap 保留首位（主业务名）
        Map<String, TenantRoleMapping> mappingMap = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery()
                .orderByAsc(TenantRoleMapping::getSortOrder)
                .orderByAsc(TenantRoleMapping::getId))
            .stream().collect(Collectors.toMap(TenantRoleMapping::getRoleCode, m -> m, (a, b) -> a));
        List<TenantRoleMappingVo> selected = new ArrayList<>();
        List<TenantRoleMappingVo> unselected = new ArrayList<>();
        for (SysRole role : standardRoles) {
            // scope 不匹配本租户产品线的标准角色，直接不出现在清单里（非前端隐藏，是数据源层剔除）
            if (!scopeMatch(role.getTenantTypeScope(), tenantType)) {
                continue;
            }
            TenantRoleMappingVo vo = new TenantRoleMappingVo();
            vo.setRoleCode(role.getRoleKey());
            vo.setRoleName(role.getRoleName());
            vo.setTenantTypeScope(StrUtil.blankToDefault(role.getTenantTypeScope(), SCOPE_BOTH));
            TenantRoleMapping m = mappingMap.get(role.getRoleKey());
            if (m != null) {
                vo.setId(m.getId());
                vo.setTenantId(m.getTenantId());
                vo.setDisplayName(m.getDisplayName());
                vo.setSortOrder(m.getSortOrder());
                vo.setStatus(m.getStatus());
                vo.setRemark(m.getRemark());
                vo.setCreateTime(m.getCreateTime());
                vo.setUpdateTime(m.getUpdateTime());
                selected.add(vo);
            } else {
                // 未挑选：id=null，默认显示名 = 标准角色名（供前端预填）
                vo.setDisplayName(role.getRoleName());
                unselected.add(vo);
            }
        }
        selected.sort(Comparator.comparing(v -> v.getSortOrder() == null ? Integer.MAX_VALUE : v.getSortOrder()));
        List<TenantRoleMappingVo> result = new ArrayList<>(selected.size() + unselected.size());
        result.addAll(selected);
        result.addAll(unselected);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int selectRoles(List<String> roleCodes) {
        if (CollUtil.isEmpty(roleCodes)) {
            return 0;
        }
        // 校验：必须落在标准角色集内（平台管理员不能新建角色 code）
        Map<String, SysRole> standardRoleMap = standardRoleMap(roleCodes);
        // ★T15.25 写入校验：scope 与本租户 tenant_type 越线的角色禁止映射（前端过滤 + 后端兜底双保险）
        String tenantType = currentTenantType();
        // 当前租户全部映射：用于幂等跳过 + 计算排序基准
        List<TenantRoleMapping> all = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery());
        Set<String> existingCodes = all.stream().map(TenantRoleMapping::getRoleCode).collect(Collectors.toSet());
        int baseSort = all.stream()
            .map(m -> m.getSortOrder() == null ? 0 : m.getSortOrder())
            .max(Integer::compareTo).orElse(0);
        int rows = 0;
        int seq = 0;
        for (String code : roleCodes) {
            if (StrUtil.isBlank(code) || existingCodes.contains(code)) {
                continue;
            }
            SysRole role = standardRoleMap.get(code);
            if (role == null) {
                throw new ServiceException("角色编码 " + code + " 不在标准角色集内，平台管理员不能新建角色");
            }
            if (!scopeMatch(role.getTenantTypeScope(), tenantType)) {
                throw new ServiceException("角色 " + role.getRoleName() + "（" + code + "）属 "
                    + StrUtil.blankToDefault(role.getTenantTypeScope(), SCOPE_BOTH)
                    + " 产品线，与本租户 " + tenantType + " 不匹配，禁止跨线映射");
            }
            TenantRoleMapping entity = new TenantRoleMapping();
            entity.setRoleCode(code);
            entity.setDisplayName(role.getRoleName());
            entity.setSortOrder(baseSort + (++seq));
            entity.setStatus("0");
            rows += mappingMapper.insert(entity);
        }
        return rows;
    }

    @Override
    public int rename(TenantRoleMappingBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("角色映射ID不能为空");
        }
        if (StrUtil.isBlank(bo.getDisplayName())) {
            throw new ServiceException("角色显示名不能为空");
        }
        TenantRoleMapping update = new TenantRoleMapping();
        update.setId(bo.getId());
        update.setDisplayName(bo.getDisplayName());
        return mappingMapper.updateById(update);
    }

    @Override
    public int toggle(Long id, String status) {
        if (id == null) {
            throw new ServiceException("角色映射ID不能为空");
        }
        TenantRoleMapping update = new TenantRoleMapping();
        update.setId(id);
        update.setStatus(StrUtil.isBlank(status) ? "0" : status);
        return mappingMapper.updateById(update);
    }

    @Override
    public int updateSort(TenantRoleMappingBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("角色映射ID不能为空");
        }
        TenantRoleMapping update = new TenantRoleMapping();
        update.setId(bo.getId());
        update.setSortOrder(bo.getSortOrder());
        return mappingMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return mappingMapper.deleteBatchIds(ids);
    }

    @Override
    public Map<String, String> roleDisplayNameMap() {
        // tenant_role_mapping 由 MyBatis 租户插件自动按当前租户隔离；仅取启用且显示名非空的映射
        // ★同 role_code 多业务名（导游/司机）时按 (sort_order,id) 升序，toMap 保留首位（主业务名）
        Map<String, String> map = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery()
            .eq(TenantRoleMapping::getStatus, "0")
            .orderByAsc(TenantRoleMapping::getSortOrder)
            .orderByAsc(TenantRoleMapping::getId))
            .stream()
            .filter(m -> StrUtil.isNotBlank(m.getRoleCode()) && StrUtil.isNotBlank(m.getDisplayName()))
            .collect(Collectors.toMap(TenantRoleMapping::getRoleCode, TenantRoleMapping::getDisplayName, (a, b) -> a,
                java.util.LinkedHashMap::new));
        // ★BUG-03 附加空间资源行业显示名（“_space_”→房间/车辆/场地）：
        //   供 Boss 首页卡片、房间状态设置面板等行业术语动态化，避免写死“房间/助教”。
        //   使用保留键 “_space_”，与业务 role_code（boss/salesman/customer 等）不冲突。
        map.put(SPACE_DISPLAY_KEY, resolveSpaceDisplayName());
        return map;
    }

    /** ★BUG-03 附加到 displayNames 返回 Map 的保留键，用于前端读取空间资源行业显示名 */
    public static final String SPACE_DISPLAY_KEY = "_space_";

    /**
     * ★BUG-03 按租户行业派生空间资源显示名（“房间”/“车辆”/“场地”）。
     * <p>旅游(travel)→车辆；桌游/剧本杀/棋牌/KTV/电竞/轰趴→房间；
     * 足浴/推拿/SPA/美容美发/美甲美睫/洗衣洗鞋→场地；未知/未配置→房间（默认）。
     * <p>兼容租户端“系统设置”中行业为自由输入的情况（可能存行业名而非 code）。
     */
    private String resolveSpaceDisplayName() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return "房间";
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || StrUtil.isBlank(tenant.getIndustry())) {
            return "房间";
        }
        String key = tenant.getIndustry().trim();
        switch (key) {
            // 旅游类→车辆（旅游大巴/中型客车/商务车）
            case "travel": case "tourism": case "旅游": case "旅游出行":
                return "车辆";
            // 生活服务类→场地
            case "foot_bath": case "massage": case "spa":
            case "beauty_salon": case "nail_lash": case "laundry":
            case "足浴": case "推拿": case "按摩": case "SPA":
            case "美容美发": case "美甲美睫": case "洗衣洗鞋":
                return "场地";
            // 休闲娱乐类 + 未知行业→房间（默认）
            case "board_game": case "script_kill": case "chess_room":
            case "ktv": case "esports": case "party_room":
            case "桌游": case "剧本杀": case "棋牌室": case "KTV":
            case "电竞馆": case "轰趴馆":
            default:
                return "房间";
        }
    }

    /**
     * ★v1.30 角色映射自愈：租户端角色映射页的数据源为 tenant_role_mapping，
     * 存量租户（创建时未级联/行业角色映射后补）该表为空时页面将永远空白且无挑选入口。
     * <p>规则：按 {@code sys_tenant.industry} 从 admin 维护的行业角色映射（industry_role_map）
     * **按缺失 role_code 幂等补录**（复用 {@link IIndustryTemplateService#initTenantByIndustry}，
     * 内部跳过目标租户已存在的 role_code 且含 T15.25 scope 过滤）；
     * 已有映射不触碰（遵守 V3.5 §16.4 脱钩原则：admin 后续改行业映射不影响已初始化租户）。
     * <p>★v1.45 修复：原仅当本租户映射**全空**才触发；若租户已有部分映射（典型路径：新增人员时
     * {@code UserTenantRoleServiceImpl#assign} 针对性自愈仅补录了 boss），salesman/customer 等缺失角色
     * 将永远无法补录（页面只见老板，员工/顾客无法分配与改名）。现改为每次进入页面按缺失角色对账补录。
     */
    private void selfHealFromIndustry() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || StrUtil.isBlank(tenant.getIndustry())) {
            return;
        }
        try {
            // 幂等：initTenantByIndustry 按 role_code 跳过已有映射，仅插入缺失项（全齐时 0 插入）
            int rows = industryTemplateService.initTenantByIndustry(tenantId, tenant.getIndustry());
            if (rows == 0) {
                // 兼容：租户端「系统设置」行业为自由输入（可能存行业名而非 code）→ 按 industry_name 反查 code 重试
                String codeByName = industryTemplateService.listEnabled().stream()
                    .filter(t -> tenant.getIndustry().equals(t.getIndustryName()))
                    .map(IndustryTemplateVo::getIndustryCode).findFirst().orElse(null);
                if (StrUtil.isNotBlank(codeByName)) {
                    rows = industryTemplateService.initTenantByIndustry(tenantId, codeByName);
                }
            }
            if (rows > 0) {
                log.info("[RoleMapping] 自愈：已从 admin 行业角色映射补录缺失角色：tenantId={}, industry={}, 新增 {} 条",
                    tenantId, tenant.getIndustry(), rows);
            }
        } catch (Exception e) {
            // 自愈失败不阻断列表查询（fail-soft），仅告警留痕
            log.warn("[RoleMapping] 行业角色映射自愈失败（不阻断查询）：tenantId={}, industry={}, err={}",
                tenantId, tenant.getIndustry(), e.getMessage());
        }
    }

    /**
     * 批量回填标准角色名（sys_role.role_name）
     */
    private void fillRoleName(List<TenantRoleMappingVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<String> codes = list.stream().map(TenantRoleMappingVo::getRoleCode)
            .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Map<String, String> nameMap = standardRoleNameMap(codes);
        list.forEach(vo -> vo.setRoleName(nameMap.get(vo.getRoleCode())));
    }

    /**
     * 查询标准角色集（tenant_id IS NULL）role_key → role_name 映射
     */
    private Map<String, String> standardRoleNameMap(Collection<String> roleCodes) {
        return standardRoleMap(roleCodes).entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getRoleName(), (a, b) -> a));
    }

    /**
     * 查询标准角色集（tenant_id IS NULL）role_key → SysRole 映射（含 tenant_type_scope，供 T15.25 校验）
     */
    private Map<String, SysRole> standardRoleMap(Collection<String> roleCodes) {
        if (CollUtil.isEmpty(roleCodes)) {
            return Collections.emptyMap();
        }
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
            .isNull(SysRole::getTenantId)
            .in(SysRole::getRoleKey, roleCodes));
        return roles.stream().collect(Collectors.toMap(SysRole::getRoleKey, r -> r, (a, b) -> a));
    }

    /**
     * ★T15.25 当前登录租户的产品线（sys_tenant.tenant_type：micro/enterprise）。
     * <p>无法定位租户（如超管/无上下文）时返回 {@code null}，此时 {@link #scopeMatch} 不做过滤（全量可见）。
     */
    private String currentTenantType() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return null;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        return tenant == null ? null : tenant.getTenantType();
    }

    /**
     * ★T15.25 scope 匹配判定：角色产品线归属是否允许出现在当前租户。
     * <p>规则：tenantType 为空 → 不过滤（超管/无上下文，全量可见）；scope 为空视为 both；
     * scope == 'both' 恒可见；否则仅当 scope 等于本租户 tenant_type 时可见（小微看不到企业线角色，反之亦然）。
     */
    private boolean scopeMatch(String scope, String tenantType) {
        if (StrUtil.isBlank(tenantType)) {
            return true;
        }
        String effectiveScope = StrUtil.blankToDefault(scope, SCOPE_BOTH);
        return SCOPE_BOTH.equals(effectiveScope) || effectiveScope.equals(tenantType);
    }
}
