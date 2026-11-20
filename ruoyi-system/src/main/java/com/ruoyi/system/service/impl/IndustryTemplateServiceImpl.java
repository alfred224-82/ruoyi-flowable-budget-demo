package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.BeanCopyUtils;
import com.ruoyi.system.domain.IndustryRoleMap;
import com.ruoyi.system.domain.IndustryTemplate;
import com.ruoyi.system.domain.ServiceCategory;
import com.ruoyi.system.domain.ServiceItem;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.TenantRoleMapping;
import com.ruoyi.system.domain.bo.IndustryRoleMapBo;
import com.ruoyi.system.domain.bo.IndustryTemplateBo;
import com.ruoyi.system.domain.enums.PricingType;
import com.ruoyi.system.domain.vo.IndustryRoleMapVo;
import com.ruoyi.system.domain.vo.IndustryTemplateVo;
import com.ruoyi.system.mapper.IndustryRoleMapMapper;
import com.ruoyi.system.mapper.IndustryTemplateMapper;
import com.ruoyi.system.mapper.ServiceCategoryMapper;
import com.ruoyi.system.mapper.ServiceItemMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.TenantRoleMappingMapper;
import com.ruoyi.system.service.IIndustryTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 行业模板服务实现（T15.2/T15.10）
 * <p>industry_template / industry_role_map 均系统级共享（IGNORE_TENANT_TABLES，无租户过滤）；
 * 租户初始化 initTenantByIndustry 跨租户写 tenant_role_mapping：Service 层显式 setTenantId + 手动按 tenantId 过滤
 * （admin 上下文多租户插件忽略过滤，见 CreateAndUpdateMetaObjectHandler 注释与 MybatisPlusConfig.ignoreTable）。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndustryTemplateServiceImpl implements IIndustryTemplateService {

    private final IndustryTemplateMapper templateMapper;
    private final IndustryRoleMapMapper roleMapMapper;
    private final TenantRoleMappingMapper mappingMapper;
    private final SysRoleMapper roleMapper;
    private final SysTenantMapper tenantMapper;
    private final ServiceCategoryMapper serviceCategoryMapper;
    private final ServiceItemMapper serviceItemMapper;

    /** 角色产品线归属：双线共用（T15.25，与 M15_09 sys_role.tenant_type_scope 默认值一致） */
    private static final String SCOPE_BOTH = "both";

    /** ★V3.7 原子资源（T15.38）：默认空间分类名（房间/空间原子资源容器） */
    private static final String SPACE_CATEGORY_NAME = "房间/空间";
    /** ★preset_config 空间分类类型（与 ServiceCategoryServiceImpl.CATEGORY_TYPE_SPACE 同口径） */
    private static final String CATEGORY_TYPE_SPACE = "space";
    /** ★preset_config 服务分类类型（与 ServiceCategoryServiceImpl.CATEGORY_TYPE_SERVICE 同口径） */
    private static final String CATEGORY_TYPE_SERVICE = "service";

    /** ★重复房间修复：租户级初始化锁——自愈/建租户并发「判空→补录」时串行化。
     * <p>无锁时两个线程（如「房间资源管理」+「房间布局配置」两个页签同时加载）都判空、
     * 各插一遍 preset 全集 → 房间同名×2。synchronized 可重入：selfHeal 持锁内调用 initTenantResources 不死锁。 */
    private static final ConcurrentHashMap<Long, Object> TENANT_INIT_LOCKS = new ConcurrentHashMap<>();

    private Object tenantInitLock(Long tenantId) {
        return TENANT_INIT_LOCKS.computeIfAbsent(tenantId, k -> new Object());
    }

    @Override
    public TableDataInfo<IndustryTemplateVo> queryPageList(IndustryTemplateBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<IndustryTemplate> wrapper = Wrappers.<IndustryTemplate>lambdaQuery()
            .eq(StrUtil.isNotBlank(bo.getIndustryCode()), IndustryTemplate::getIndustryCode, bo.getIndustryCode())
            .like(StrUtil.isNotBlank(bo.getIndustryName()), IndustryTemplate::getIndustryName, bo.getIndustryName())
            .eq(StrUtil.isNotBlank(bo.getStatus()), IndustryTemplate::getStatus, bo.getStatus())
            .orderByAsc(IndustryTemplate::getSortOrder)
            .orderByAsc(IndustryTemplate::getId);
        Page<IndustryTemplateVo> page = templateMapper.selectVoPage(pageQuery.build(), wrapper);
        return TableDataInfo.build(page);
    }

    @Override
    public IndustryTemplateVo queryById(Long id) {
        IndustryTemplateVo vo = templateMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("行业模板不存在");
        }
        return vo;
    }

    @Override
    public List<IndustryTemplateVo> listEnabled() {
        return templateMapper.selectVoList(Wrappers.<IndustryTemplate>lambdaQuery()
            .eq(IndustryTemplate::getStatus, "0")
            .orderByAsc(IndustryTemplate::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insert(IndustryTemplateBo bo) {
        IndustryTemplate exist = templateMapper.selectOne(Wrappers.<IndustryTemplate>lambdaQuery()
            .eq(IndustryTemplate::getIndustryCode, bo.getIndustryCode()));
        if (exist != null) {
            throw new ServiceException("行业编码已存在：" + bo.getIndustryCode());
        }
        IndustryTemplate entity = BeanCopyUtils.copy(bo, IndustryTemplate.class);
        entity.setId(null);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("0");
        }
        if (entity.getVoiceEnabled() == null) {
            entity.setVoiceEnabled(1);
        }
        templateMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(IndustryTemplateBo bo) {
        IndustryTemplate exist = templateMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("行业模板不存在");
        }
        IndustryTemplate update = BeanCopyUtils.copy(bo, IndustryTemplate.class);
        // 身份字段 industry_code 不可改（industry_role_map 以其为外键）
        update.setIndustryCode(null);
        return templateMapper.updateById(update);
    }

    @Override
    public int deleteByIds(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }
        return templateMapper.deleteBatchIds(ids);
    }

    @Override
    public List<IndustryRoleMapVo> listRoleMaps(String industryCode) {
        if (StrUtil.isBlank(industryCode)) {
            return new ArrayList<>();
        }
        return roleMapMapper.selectVoList(Wrappers.<IndustryRoleMap>lambdaQuery()
            .eq(IndustryRoleMap::getIndustryCode, industryCode)
            .orderByAsc(IndustryRoleMap::getSortOrder)
            .orderByAsc(IndustryRoleMap::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveRoleMaps(String industryCode, List<IndustryRoleMapBo> roleMaps) {
        if (StrUtil.isBlank(industryCode)) {
            throw new ServiceException("行业编码不能为空");
        }
        // 整表替换：先物理删除该行业全部映射（industry_role_map 无 del_flag）
        roleMapMapper.delete(Wrappers.<IndustryRoleMap>lambdaQuery()
            .eq(IndustryRoleMap::getIndustryCode, industryCode));
        if (CollUtil.isEmpty(roleMaps)) {
            return 0;
        }
        List<IndustryRoleMap> entities = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int seq = 0;
        for (IndustryRoleMapBo bo : roleMaps) {
            if (StrUtil.isBlank(bo.getBizRoleName()) || StrUtil.isBlank(bo.getRoleCode())) {
                continue;
            }
            // uk_industry_bizrole：同行业业务角色名去重
            if (!seen.add(bo.getBizRoleName())) {
                continue;
            }
            seq++;
            IndustryRoleMap e = new IndustryRoleMap();
            e.setIndustryCode(industryCode);
            e.setBizRoleName(bo.getBizRoleName());
            e.setRoleCode(bo.getRoleCode());
            e.setDefaultFlag(bo.getDefaultFlag() == null ? 0 : bo.getDefaultFlag());
            e.setSortOrder(bo.getSortOrder() == null ? seq : bo.getSortOrder());
            entities.add(e);
        }
        if (entities.isEmpty()) {
            return 0;
        }
        roleMapMapper.insertBatch(entities);
        return entities.size();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initTenantByIndustry(Long tenantId, String industryCode) {
        if (tenantId == null || StrUtil.isBlank(industryCode)) {
            throw new ServiceException("租户ID与行业编码不能为空");
        }
        // 行业角色映射（系统级，无租户过滤），按 (sort_order,id) 升序——位序对账依赖确定性排序
        List<IndustryRoleMap> roleMaps = roleMapMapper.selectList(Wrappers.<IndustryRoleMap>lambdaQuery()
            .eq(IndustryRoleMap::getIndustryCode, industryCode)
            .orderByAsc(IndustryRoleMap::getSortOrder)
            .orderByAsc(IndustryRoleMap::getId));
        if (CollUtil.isEmpty(roleMaps)) {
            return 0;
        }
        // ★T15.25 硬隔离：禁止跨线映射——按目标租户 tenant_type 过滤标准角色 scope
        // （与 TenantRoleMappingServiceImpl#selectRoles 同口径；企业线租户不接收 salesman/customer 等 micro 角色）
        SysTenant target = tenantMapper.selectById(tenantId);
        String tenantType = target == null ? null : target.getTenantType();
        Set<String> roleCodes = roleMaps.stream().map(IndustryRoleMap::getRoleCode)
            .filter(StrUtil::isNotBlank).collect(Collectors.toSet());
        Map<String, SysRole> standardRoleMap = roleCodes.isEmpty() ? Collections.emptyMap()
            : roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                .isNull(SysRole::getTenantId)
                .in(SysRole::getRoleKey, roleCodes))
            .stream().collect(Collectors.toMap(SysRole::getRoleKey, r -> r, (a, b) -> a));
        // 幂等 + §16.4 改名脱钩：按 (role_code, display_name) pair 级对账（与 M15_31 uk_tenant_role_code_name 同口径）。
        //   同 role_code 允许多个行业业务名（如旅游 salesman 的「导游」「司机」）：
        //   存量租户该 code 已有 M 行时，仅补行业位序 pos>M 的附加业务名；已改名/已存在的业务名不复活、不重复。
        //   ★admin 上下文插件忽略过滤，须手动按 tenantId 过滤
        List<TenantRoleMapping> existingMappings = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery()
            .eq(TenantRoleMapping::getTenantId, tenantId));
        // 各 role_code 现有行数（位序守卫基准）+ 已存在 (role_code, display_name) pair（pair 级幂等）
        Map<String, Integer> codeCount = new HashMap<>();
        Set<String> existingPairs = new HashSet<>();
        for (TenantRoleMapping m : existingMappings) {
            if (StrUtil.isBlank(m.getRoleCode())) {
                continue;
            }
            codeCount.merge(m.getRoleCode(), 1, Integer::sum);
            existingPairs.add(pairKey(m.getRoleCode(), m.getDisplayName()));
        }
        int rows = 0;
        // 同 role_code 行业位序计数器（1 基，按 roleMaps 的 (sort_order,id) 顺序）
        Map<String, Integer> posCounter = new HashMap<>();
        for (IndustryRoleMap rm : roleMaps) {
            String roleCode = rm.getRoleCode();
            if (StrUtil.isBlank(roleCode)) {
                continue;
            }
            // 行业位序 pos：同 role_code 按 (sort_order,id) 升序的 1 基序号
            int pos = posCounter.merge(roleCode, 1, Integer::sum);
            // 跳过：标准角色不存在 / 产品线 scope 与目标租户不匹配（T15.25 禁止跨线映射）
            SysRole role = standardRoleMap.get(roleCode);
            if (role == null || !scopeMatch(role.getTenantTypeScope(), tenantType)) {
                continue;
            }
            String displayName = StrUtil.isNotBlank(rm.getBizRoleName()) ? rm.getBizRoleName() : roleCode;
            // pair 级幂等：同 (role_code, display_name) 已存在则跳过
            if (existingPairs.contains(pairKey(roleCode, displayName))) {
                continue;
            }
            // 位序守卫：租户该 code 现有行数 >= 本行位序 → 跳过（§16.4 改名不复活旧名）
            if (codeCount.getOrDefault(roleCode, 0) >= pos) {
                continue;
            }
            TenantRoleMapping entity = new TenantRoleMapping();
            // ★跨租户写入：显式指定目标租户（不依赖登录上下文/插件填充）
            entity.setTenantId(tenantId);
            entity.setRoleCode(roleCode);
            entity.setDisplayName(displayName);
            entity.setSortOrder(rm.getSortOrder() == null ? 0 : rm.getSortOrder());
            // default_flag=1 → 启用(0)，否则禁用(1)
            entity.setStatus(rm.getDefaultFlag() != null && rm.getDefaultFlag() == 1 ? "0" : "1");
            rows += mappingMapper.insert(entity);
            // 同步本批次计数/pair，避免同 code 多业务名重复插入
            codeCount.merge(roleCode, 1, Integer::sum);
            existingPairs.add(pairKey(roleCode, displayName));
        }
        return rows;
    }

    /**
     * (role_code, display_name) pair 键——同 role_code 多业务角色名（导游/司机）的 pair 级幂等去重口径，
     * 与 M15_31 唯一键 uk_tenant_role_code_name 一致。用 \u0000 分隔避免拼接歧义。
     */
    private static String pairKey(String roleCode, String displayName) {
        return roleCode + '\u0000' + StrUtil.nullToEmpty(displayName);
    }

    /**
     * ★T15.25 scope 匹配判定（与 TenantRoleMappingServiceImpl#scopeMatch 同口径）：
     * tenantType 为空不过滤（超管/无上下文）；scope 为空视为 both；scope == 'both' 恒可见；否则须等于目标租户 tenant_type。
     */
    private boolean scopeMatch(String scope, String tenantType) {
        if (StrUtil.isBlank(tenantType)) {
            return true;
        }
        String effectiveScope = StrUtil.blankToDefault(scope, SCOPE_BOTH);
        return SCOPE_BOTH.equals(effectiveScope) || effectiveScope.equals(tenantType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int initTenantResources(Long tenantId, String industryCode) {
        if (tenantId == null || StrUtil.isBlank(industryCode)) {
            throw new ServiceException("租户ID与行业编码不能为空");
        }
        // ★重复房间修复：按租户串行化，杜绝并发双插（锁可重入，selfHeal 路径已持锁）
        synchronized (tenantInitLock(tenantId)) {
            return doInitTenantResources(tenantId, industryCode);
        }
    }

    private int doInitTenantResources(Long tenantId, String industryCode) {
        IndustryTemplate template = templateMapper.selectOne(Wrappers.<IndustryTemplate>lambdaQuery()
            .eq(IndustryTemplate::getIndustryCode, industryCode));
        if (template == null || StrUtil.isBlank(template.getPresetConfig())
            || !JSONUtil.isTypeJSON(template.getPresetConfig())) {
            return 0;
        }
        JSONObject preset;
        try {
            preset = JSONUtil.parseObj(template.getPresetConfig());
        } catch (Exception e) {
            log.warn("[AtomicResource] preset_config 解析失败（跳过资源初始化）：industryCode={}, err={}",
                industryCode, e.getMessage());
            return 0;
        }
        JSONArray serviceCategories = preset.getJSONArray("service_categories");
        JSONArray projects = preset.getJSONArray("projects");
        JSONArray rooms = preset.getJSONArray("rooms");
        JSONArray units = preset.getJSONArray("units");
        String defaultUnit = (units == null || units.isEmpty()) ? null : units.getStr(0);

        int rows = 0;
        // ① 服务分类：目标租户无 service 分类时按 preset 初始化（★admin 上下文插件忽略过滤，须手动按 tenantId 过滤）
        Long serviceCatCount = serviceCategoryMapper.selectCount(Wrappers.<ServiceCategory>lambdaQuery()
            .eq(ServiceCategory::getTenantId, tenantId)
            .eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SERVICE));
        if ((serviceCatCount == null || serviceCatCount == 0) && serviceCategories != null) {
            // ★重复房间修复：名称级幂等——预载本租户已有分类名，同名跳过（兼防模板自身重名）
            Set<String> existingCatNames = serviceCategoryMapper.selectList(Wrappers.<ServiceCategory>lambdaQuery()
                    .select(ServiceCategory::getName)
                    .eq(ServiceCategory::getTenantId, tenantId))
                .stream().map(ServiceCategory::getName).collect(Collectors.toCollection(HashSet::new));
            int catSeq = 0;
            for (int i = 0; i < serviceCategories.size(); i++) {
                String categoryName = serviceCategories.getStr(i);
                if (StrUtil.isBlank(categoryName) || !existingCatNames.add(categoryName)) {
                    continue;
                }
                rows += serviceCategoryMapper.insert(
                    buildCategory(tenantId, industryCode, categoryName, CATEGORY_TYPE_SERVICE, ++catSeq));
            }
        }

        // ② 空间分类：独立幂等——无 space 分类且 preset 有 rooms 时补建
        //    ★修复：原逻辑将空间分类创建嵌套在「无任何分类」分支内，租户先建过 service 分类后房间永远无法初始化
        Long spaceCategoryId = null;
        if (rooms != null && !rooms.isEmpty()) {
            ServiceCategory space = serviceCategoryMapper.selectOne(Wrappers.<ServiceCategory>lambdaQuery()
                .eq(ServiceCategory::getTenantId, tenantId)
                .eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SPACE)
                .orderByAsc(ServiceCategory::getId)
                .last("LIMIT 1"));
            if (space == null) {
                space = buildCategory(tenantId, industryCode, SPACE_CATEGORY_NAME, CATEGORY_TYPE_SPACE, 0);
                serviceCategoryMapper.insert(space);
                rows++;
            }
            spaceCategoryId = space.getId();
        }

        // ③ 项目：按「非空间分类」维度独立幂等
        //    ★修复：原按 service_item 总数判定，租户手工建过任意 1 条房间后项目补录被永久阻塞（V3.5 §16.4 脱钩原则细化到分维度）
        Long projectCount = countProjectItems(tenantId, spaceCategoryId);
        if ((projectCount == null || projectCount == 0) && projects != null) {
            // ★重复房间修复：名称级幂等——预载已有项目名，同名跳过（并发窗口残留/模板自身重名防御）
            Set<String> existingProjectNames = loadProjectNames(tenantId, spaceCategoryId);
            int seq = 0;
            for (int i = 0; i < projects.size(); i++) {
                JSONObject p = projects.getJSONObject(i);
                if (p == null) {
                    continue;
                }
                String name = p.getStr("name");
                if (StrUtil.isBlank(name) || !existingProjectNames.add(name)) {
                    continue;
                }
                ServiceItem item = new ServiceItem();
                // ★跨租户写入：显式指定目标租户（不依赖登录上下文/插件填充）
                item.setTenantId(tenantId);
                // ★v1.40 修复：项目不属于任何分类，但 category_id 列 NOT NULL，用 0 作哨兵值
                item.setCategoryId(0L);
                item.setName(name);
                item.setPrice(p.getBigDecimal("reference_price", BigDecimal.ZERO));
                item.setUnit(defaultUnit);
                item.setPricingType(PricingType.TIMES.getCode());
                String duration = p.getStr("duration");
                item.setDescription(StrUtil.isNotBlank(duration) ? "参考时长：" + duration : null);
                item.setRemark(buildProjectRemark(p.getStr("sample_merchant"), p.getStr("feature")));
                item.setSortOrder(++seq);
                item.setStatus("0");
                rows += serviceItemMapper.insert(item);
            }
        }

        // ④ 房间：按「空间分类」维度独立幂等（★修复：不因租户已建项目/手工房间而阻塞 preset 房间补录）
        if (spaceCategoryId != null && rooms != null) {
            final Long scId = spaceCategoryId;
            Long roomCount = serviceItemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery()
                .eq(ServiceItem::getTenantId, tenantId)
                .eq(ServiceItem::getCategoryId, scId));
            if (roomCount == null || roomCount == 0) {
                // ★重复房间修复：名称级幂等——预载本空间分类下已有房间名，同名跳过；
                //   批内按名去重（模板自身重名防御）。与租户级锁配合彻底杜绝同名房间×2
                Set<String> existingRoomNames = serviceItemMapper.selectList(Wrappers.<ServiceItem>lambdaQuery()
                        .select(ServiceItem::getName)
                        .eq(ServiceItem::getTenantId, tenantId)
                        .eq(ServiceItem::getCategoryId, scId))
                    .stream().map(ServiceItem::getName).collect(Collectors.toCollection(HashSet::new));
                int roomSeq = 0;
                for (int i = 0; i < rooms.size(); i++) {
                    JSONObject r = rooms.getJSONObject(i);
                    if (r == null) {
                        continue;
                    }
                    String name = r.getStr("name");
                    if (StrUtil.isBlank(name) || !existingRoomNames.add(name)) {
                        continue;
                    }
                    ServiceItem item = new ServiceItem();
                    item.setTenantId(tenantId);
                    item.setCategoryId(spaceCategoryId);
                    item.setName(name);
                    item.setPrice(r.getBigDecimal("reference_price", BigDecimal.ZERO));
                    // 房间/空间默认按时长计价（元/小时）
                    item.setUnit("小时");
                    item.setPricingType(PricingType.DURATION.getCode());
                    String capacity = r.getStr("capacity");
                    item.setDescription(StrUtil.isNotBlank(capacity) ? "可容纳：" + capacity : null);
                    // ★v1.40 解析 capacity 数字（"2-4人"→4, "6人"→6, "每位"→null），存入 capacity 列便于统计
                    Integer capacityNumber = parseCapacityNumber(capacity);
                    item.setCapacity(capacityNumber);
                    // ★T26.2 房间二态：has_seat_layout 由行业模板 preset_config.rooms[].has_seat_layout 决定（缺省 false 优雅降级）
                    boolean hasSeatLayout = r.getBool("has_seat_layout", false);
                    item.setHasSeatLayout(hasSeatLayout ? 1 : 0);
                    // ★T26.2 seat_capacity 优先取模板显式配置，未配则回退 capacity（有位置时才有意义）
                    Integer seatCapacity = r.getInt("seat_capacity", null);
                    item.setSeatCapacity(seatCapacity != null ? seatCapacity : (hasSeatLayout ? capacityNumber : null));
                    item.setSortOrder(++roomSeq);
                    item.setStatus("0");
                    rows += serviceItemMapper.insert(item);
                }
            }
        }
        return rows;
    }

    @Override
    public void selfHealCurrentTenantResources() {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            log.warn("[AtomicResource] 自愈跳过：tenantId 为空（未登录或非租户用户）");
            return;
        }
        // 快路径：本租户「项目」与「房间」两个维度均已初始化 → 不自愈（V3.5 §16.4 脱钩原则）
        // ★修复：原按 service_item 总数单维度判定，租户手工建过任意 1 条 item（如仅建房间）后
        //        两个维度都被永久阻塞（项目页永远空白）。改为分维度判定，任一维度为空即进入
        //        initTenantResources，由其内部按维度独立幂等补录（不会覆盖已有数据）。
        // ★v1.39 修复：计数查询显式按 tenantId 过滤（与 initTenantResources 同口径），
        //        不再纯依赖多租户插件自动追加。避免插件边界场景下跨租户数据误判"已初始化"，
        //        导致当前租户首次进入列表时自愈被跳过、项目/房间页永远空白。
        // ★重复房间修复：判空→补录全程持租户级锁，与并发请求串行（计数在锁内读取，杜绝旧值窗口）
        synchronized (tenantInitLock(tenantId)) {
            List<Long> spaceCatIds = serviceCategoryMapper.selectList(Wrappers.<ServiceCategory>lambdaQuery()
                    .eq(ServiceCategory::getTenantId, tenantId)
                    .eq(ServiceCategory::getCategoryType, CATEGORY_TYPE_SPACE))
                .stream().map(ServiceCategory::getId).collect(Collectors.toList());
            Long projectCount;
            Long roomCount;
            if (spaceCatIds.isEmpty()) {
                projectCount = serviceItemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery()
                    .eq(ServiceItem::getTenantId, tenantId));
                roomCount = 0L;
            } else {
                // ★v1.40 兼容：categoryId=0 为项目哨兵值（等同 IS NULL）
                projectCount = serviceItemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery()
                    .eq(ServiceItem::getTenantId, tenantId)
                    .and(w -> w.isNull(ServiceItem::getCategoryId).or().eq(ServiceItem::getCategoryId, 0L)
                        .or().notIn(ServiceItem::getCategoryId, spaceCatIds)));
                roomCount = serviceItemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery()
                    .eq(ServiceItem::getTenantId, tenantId)
                    .in(ServiceItem::getCategoryId, spaceCatIds));
            }
            if ((projectCount != null && projectCount > 0) && (roomCount != null && roomCount > 0)) {
                return;
            }
            SysTenant tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || StrUtil.isBlank(tenant.getIndustry())) {
                return;
            }
            try {
                int rows = initTenantResources(tenantId, tenant.getIndustry());
                if (rows == 0) {
                    // 兼容：租户端「系统设置」行业为自由输入（可能存行业名而非 code）→ 按 industry_name 反查 code 重试
                    String codeByName = listEnabled().stream()
                        .filter(t -> tenant.getIndustry().equals(t.getIndustryName()))
                        .map(IndustryTemplateVo::getIndustryCode).findFirst().orElse(null);
                    if (StrUtil.isNotBlank(codeByName)) {
                        rows = initTenantResources(tenantId, codeByName);
                    }
                }
                if (rows > 0) {
                    log.info("[AtomicResource] 自愈：已从 admin 行业模板预置项目/房间拉取落库：tenantId={}, industry={}, 新增 {} 条",
                        tenantId, tenant.getIndustry(), rows);
                }
            } catch (Exception e) {
                // 自愈失败不阻断列表查询（fail-soft），仅告警留痕
                log.warn("[AtomicResource] 行业预置项目/房间自愈失败（不阻断查询）：tenantId={}, industry={}, err={}",
                    tenantId, tenant.getIndustry(), e.getMessage());
            }
        }
    }

    /**
     * ★T15.38 修复：按「非空间分类」维度统计项目数（分维度幂等判定用，admin 上下文须显式按 tenantId 过滤）
     */
    private Long countProjectItems(Long tenantId, Long spaceCategoryId) {
        if (spaceCategoryId == null) {
            return serviceItemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery()
                .eq(ServiceItem::getTenantId, tenantId));
        }
        final Long scId = spaceCategoryId;
        // ★v1.40 兼容：categoryId=0 为项目哨兵值（等同 IS NULL）
        return serviceItemMapper.selectCount(Wrappers.<ServiceItem>lambdaQuery()
            .eq(ServiceItem::getTenantId, tenantId)
            .and(w -> w.isNull(ServiceItem::getCategoryId).or().eq(ServiceItem::getCategoryId, 0L)
                .or().ne(ServiceItem::getCategoryId, scId)));
    }

    /**
     * ★重复房间修复：预载「非空间分类」维度已有项目名（名称级幂等去重用，与 countProjectItems 同口径）
     */
    private Set<String> loadProjectNames(Long tenantId, Long spaceCategoryId) {
        LambdaQueryWrapper<ServiceItem> wrapper = Wrappers.<ServiceItem>lambdaQuery()
            .select(ServiceItem::getName)
            .eq(ServiceItem::getTenantId, tenantId);
        if (spaceCategoryId != null) {
            final Long scId = spaceCategoryId;
            wrapper.and(w -> w.isNull(ServiceItem::getCategoryId).or().eq(ServiceItem::getCategoryId, 0L)
                .or().ne(ServiceItem::getCategoryId, scId));
        }
        return serviceItemMapper.selectList(wrapper).stream()
            .map(ServiceItem::getName).collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * ★T15.38 构建租户级服务分类（跨租户写入显式 setTenantId，与 initTenantByIndustry 同模式）
     */
    private ServiceCategory buildCategory(Long tenantId, String industryCode, String name, String categoryType, int sortOrder) {
        ServiceCategory category = new ServiceCategory();
        category.setTenantId(tenantId);
        category.setName(name);
        category.setIndustryType(industryCode);
        category.setCategoryType(categoryType);
        category.setSortOrder(sortOrder);
        category.setStatus("0");
        return category;
    }

    /**
     * ★v1.40 解析 capacity 字符串为数字（取最大值，便于统计）
     * <p>"2-4人" → 4, "6人" → 6, "每位" → null, null → null
     */
    private Integer parseCapacityNumber(String capacity) {
        if (StrUtil.isBlank(capacity)) {
            return null;
        }
        // 去掉"人"字，取最后一段数字（范围取最大值）
        String cleaned = capacity.replace("人", "").trim();
        // 处理范围 "2-4" → 取 4
        if (cleaned.contains("-")) {
            String[] parts = cleaned.split("-");
            try {
                return Integer.parseInt(parts[parts.length - 1].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        // 处理单个数字 "6" → 6
        try {
            return Integer.parseInt(cleaned.trim());
        } catch (NumberFormatException e) {
            // "每位" 等非数字描述 → null
            return null;
        }
    }

    /**
     * ★T15.38 项目备注：样例商户 + 特色拼接（均可空）
     */
    private String buildProjectRemark(String sampleMerchant, String feature) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(sampleMerchant)) {
            sb.append("样例商户：").append(sampleMerchant);
        }
        if (StrUtil.isNotBlank(feature)) {
            if (sb.length() > 0) {
                sb.append("；");
            }
            sb.append(feature);
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
