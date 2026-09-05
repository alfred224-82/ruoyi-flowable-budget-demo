package com.ruoyi.system.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.constant.EditionFeatureConfig;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.EncryptUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.SysTenantConfig;
import com.ruoyi.system.domain.SysTenantVersionLog;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.domain.bo.SysTenantBo;
import com.ruoyi.system.domain.bo.SysTenantConfigBo;
import com.ruoyi.system.domain.vo.SysTenantConfigVo;
import com.ruoyi.system.domain.vo.SysTenantVersionLogVo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysTenantConfigMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysTenantVersionLogMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.ISysTenantService;
import com.ruoyi.system.service.TenantStatusChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.dev33.satoken.secure.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 租户管理Service实现
 * 设计文档 Phase 16 Task 16.5
 *
 * @author ruoyi
 * @date 2026-08-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysTenantServiceImpl implements ISysTenantService {

    private final SysTenantMapper tenantMapper;
    private final SysTenantVersionLogMapper versionLogMapper;
    private final SysTenantConfigMapper tenantConfigMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final TenantStatusChecker tenantStatusChecker;

    /**
     * 企微凭据 AES 加密密钥（T10.1：wecom_secret 加密存储）；必须为 16/24/32 位，生产环境务必通过配置覆盖
     */
    @Value("${tenant.wecom.aes-key:RuoYiFlowableTenantCryptoKey0001}")
    private String wecomAesKey;

    /** 企微密钥脱敏占位符：接口返回该值表示“不变更”，前端回填时不覆盖真实密文 */
    private static final String SECRET_MASK = "******";

    /** 预设角色模板 ID（M2_01 全局共享角色 9201~9205，仅作为租户级角色的克隆源，不再直接授权给用户，T2.5） */
    private static final long[] PRESET_ROLE_IDS = {9201L, 9202L, 9203L, 9204L, 9205L};

    /** 预设角色标识（与 M2_01 一致） */
    private static final String[] PRESET_ROLE_KEYS = {"boss", "finance", "manager", "employee", "platform_admin"};

    /**
     * Redis 通道：租户功能开关刷新
     */
    private static final String TENANT_FEATURE_REFRESH_CHANNEL = "tenant:feature:refresh";

    @Override
    public TableDataInfo<SysTenantVo> queryPageList(SysTenantBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SysTenant> wrapper = Wrappers.lambdaQuery();
        wrapper.like(StrUtil.isNotBlank(bo.getCompanyName()), SysTenant::getCompanyName, bo.getCompanyName())
               .eq(StrUtil.isNotBlank(bo.getTenantCode()), SysTenant::getTenantCode, bo.getTenantCode())
               .eq(StrUtil.isNotBlank(bo.getStatus()), SysTenant::getStatus, bo.getStatus())
               .eq(StrUtil.isNotBlank(bo.getEdition()), SysTenant::getEdition, bo.getEdition())
               .eq(StrUtil.isNotBlank(bo.getBizCode()), SysTenant::getBizCode, bo.getBizCode())
               .eq(StrUtil.isNotBlank(bo.getAccessChannel()), SysTenant::getAccessChannel, bo.getAccessChannel())
               .orderByDesc(SysTenant::getCreateTime);
        Page<SysTenantVo> page = tenantMapper.selectVoPage(pageQuery.build(), wrapper);
        // T10.1：企微密钥返回脱敏
        if (page.getRecords() != null) {
            page.getRecords().forEach(this::desensitizeSecret);
        }
        return TableDataInfo.build(page);
    }

    @Override
    public SysTenantVo queryById(Long id) {
        SysTenantVo vo = tenantMapper.selectVoById(id);
        // T10.1：企微密钥返回脱敏
        desensitizeSecret(vo);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertTenant(SysTenantBo bo) {
        // 1. 自动生成租户编码（T + 企业名称 + - + 时间戳后4位），保证唯一性
        String tenantCode = generateTenantCode(bo.getCompanyName());

        // 2. 插入租户记录（status=PENDING）
        SysTenant tenant = new SysTenant();
        tenant.setTenantCode(tenantCode);
        tenant.setCompanyName(bo.getCompanyName());
        tenant.setContactName(bo.getContactName());
        tenant.setContactPhone(bo.getContactPhone());
        tenant.setIndustry(bo.getIndustry());
        tenant.setScale(bo.getScale());
        tenant.setEdition(StrUtil.isNotBlank(bo.getEdition()) ? bo.getEdition() : EditionFeatureConfig.EDITION_BASIC);
        // T10.1：业态包与访问通道（默认 fund_control / wechat）
        tenant.setBizCode(StrUtil.isNotBlank(bo.getBizCode()) ? bo.getBizCode() : BizBundleServiceImpl.DEFAULT_BIZ_CODE);
        tenant.setAccessChannel(StrUtil.isNotBlank(bo.getAccessChannel()) ? bo.getAccessChannel() : "wechat");
        tenant.setWecomCorpId(bo.getWecomCorpId());
        tenant.setWecomAgentId(bo.getWecomAgentId());
        // 企微密钥明文入参 → AES 加密落库
        tenant.setWecomSecret(encryptSecret(bo.getWecomSecret()));
        tenant.setMpAppId(bo.getMpAppId());
        tenant.setLicenseType(StrUtil.isNotBlank(bo.getLicenseType()) ? bo.getLicenseType() : "TRIAL");
        tenant.setExpireDate(bo.getExpireDate());
        tenant.setMaxUsers(bo.getMaxUsers());
        tenant.setFeatures(convertFeaturesToJson(tenant.getEdition()));
        tenant.setStatus("PENDING");
        tenant.setRemark(bo.getRemark());
        int rows = tenantMapper.insert(tenant);

        // 3. 创建租户管理员账号（如果传入了 username）
        Long adminUserId = null;
        if (StrUtil.isNotBlank(bo.getUsername())) {
            // 检查用户名是否已存在
            Long existCount = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserName, bo.getUsername()));
            if (existCount != null && existCount > 0) {
                throw new ServiceException("用户名 '" + bo.getUsername() + "' 已存在");
            }

            SysUser adminUser = new SysUser();
            adminUser.setUserName(bo.getUsername());
            adminUser.setNickName(StrUtil.isNotBlank(bo.getContactName()) ? bo.getContactName() : bo.getUsername());
            adminUser.setPassword(BCrypt.hashpw(
                StrUtil.isNotBlank(bo.getPassword()) ? bo.getPassword() : "123456"));
            adminUser.setTenantId(tenant.getId());
            adminUser.setUserType("sys_user");
            adminUser.setStatus("0");
            adminUser.setCreateBy(LoginHelper.getUsername());
            userMapper.insert(adminUser);
            adminUserId = adminUser.getUserId();

            log.info("[Tenant] 租户管理员已创建：userId={}, username={}, tenantId={}",
                adminUser.getUserId(), adminUser.getUserName(), tenant.getId());
        }

        // 4. 克隆租户级预设角色（boss/finance/manager/employee/platform_admin，绑定 tenant_id），
        //    并按“角色×版本”模板授权菜单，版本变更仅影响本租户（T2.5）
        Map<String, Long> tenantRoleIds = clonePresetRoles(tenant.getId(), tenant.getEdition());

        // 5. 管理员分配本租户的 platform_admin 克隆角色，回写租户记录并置为 NORMAL
        if (adminUserId != null) {
            Long paRoleId = tenantRoleIds.get("platform_admin");
            if (paRoleId != null) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(adminUserId);
                userRole.setRoleId(paRoleId);
                userRoleMapper.insert(userRole);
            }
            tenant.setAdminUserId(adminUserId);
            tenant.setStatus("NORMAL");
            tenantMapper.updateById(tenant);
        }

        // 6. 初始化租户默认数据
        initTenantDefaultData(tenant.getId());

        // 7. 记录版本日志（changeType=INIT）
        SysTenantVersionLog versionLog = new SysTenantVersionLog();
        versionLog.setTenantId(tenant.getId());
        versionLog.setFromEdition(null);
        versionLog.setToEdition(tenant.getEdition());
        versionLog.setChangeType("INIT");
        versionLog.setOperator(LoginHelper.getUsername());
        versionLog.setRemark("租户创建初始化");
        versionLogMapper.insert(versionLog);

        log.info("[Tenant] 创建租户成功：{} - {}", tenant.getTenantCode(), tenant.getCompanyName());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateTenant(SysTenantBo bo) {
        if (bo.getId() == null) {
            throw new ServiceException("租户ID不能为空");
        }
        SysTenant tenant = tenantMapper.selectById(bo.getId());
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }

        // 更新字段
        tenant.setCompanyName(bo.getCompanyName());
        tenant.setContactName(bo.getContactName());
        tenant.setContactPhone(bo.getContactPhone());
        tenant.setIndustry(bo.getIndustry());
        tenant.setScale(bo.getScale());
        // T10.1：业态包与访问通道（platform_admin 分配，T3.2 联动）
        if (StrUtil.isNotBlank(bo.getBizCode())) {
            tenant.setBizCode(bo.getBizCode());
        }
        if (StrUtil.isNotBlank(bo.getAccessChannel())) {
            tenant.setAccessChannel(bo.getAccessChannel());
        }
        tenant.setWecomCorpId(bo.getWecomCorpId());
        tenant.setWecomAgentId(bo.getWecomAgentId());
        tenant.setMpAppId(bo.getMpAppId());
        // 企微密钥：仅当传入非空且非脱敏占位符时才加密更新（避免回填 "******" 覆盖真实密文）
        if (StrUtil.isNotBlank(bo.getWecomSecret()) && !SECRET_MASK.equals(bo.getWecomSecret())) {
            tenant.setWecomSecret(encryptSecret(bo.getWecomSecret()));
        }
        tenant.setExpireDate(bo.getExpireDate());
        tenant.setMaxUsers(bo.getMaxUsers());
        tenant.setRemark(bo.getRemark());

        // T3.4 到期日延长（或设为永久）时，自动将 EXPIRED 恢复为 NORMAL
        if ("EXPIRED".equals(tenant.getStatus())) {
            Date expireDate = bo.getExpireDate();
            if (expireDate == null || expireDate.after(new Date())) {
                tenant.setStatus("NORMAL");
                log.info("[Tenant] 租户 {} 到期日已修正，状态从 EXPIRED 恢复为 NORMAL", bo.getId());
            }
        }

        int rows = tenantMapper.updateById(tenant);
        // T3.4 到期日可能变更，清除租户状态缓存，使新到期日立即生效
        tenantStatusChecker.evictStatusCache(bo.getId());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateStatus(Long id, String status) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }
        tenant.setStatus(status);
        int rows = tenantMapper.updateById(tenant);

        // T3.4 状态变更立即清除缓存（到期/停用即时生效，续费恢复即时放行）
        tenantStatusChecker.evictStatusCache(id);
        // 发布缓存刷新通知
        RedisUtils.publish(TENANT_FEATURE_REFRESH_CHANNEL, id.toString());
        log.info("[Tenant] 租户状态变更：{} -> {}", tenant.getTenantCode(), status);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int assignVersion(Long id, String edition) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }

        String fromEdition = tenant.getEdition();

        // 1. 更新租户 edition 和 features
        tenant.setEdition(edition);
        tenant.setFeatures(convertFeaturesToJson(edition));
        int rows = tenantMapper.updateById(tenant);

        // 2. 记录版本变更日志
        SysTenantVersionLog versionLog = new SysTenantVersionLog();
        versionLog.setTenantId(id);
        versionLog.setFromEdition(fromEdition);
        versionLog.setToEdition(edition);
        versionLog.setChangeType(compareEdition(fromEdition, edition) > 0 ? "UPGRADE" : "DOWNGRADE");
        versionLog.setOperator(LoginHelper.getUsername());
        versionLog.setRemark("版本变更：" + fromEdition + " -> " + edition);
        versionLogMapper.insert(versionLog);

        // 3. 按版本模板重新授权菜单（仅本租户克隆的预设角色，不影响其他租户，T2.5）
        List<SysRole> tenantRoles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
            .eq(SysRole::getTenantId, id)
            .in(SysRole::getRoleKey, Arrays.asList(PRESET_ROLE_KEYS)));
        for (SysRole role : tenantRoles) {
            reCopyMenuTemplate(getRoleTemplateKey(role.getRoleKey(), edition), new long[]{role.getRoleId()});
        }
        if (tenantRoles.isEmpty()) {
            // 存量租户未克隆角色时兼容补建（含 platform_admin，菜单随版本模板）
            clonePresetRoles(id, edition);
        }

        // 4. 发布 Redis 消息通知刷新功能开关缓存
        RedisUtils.publish(TENANT_FEATURE_REFRESH_CHANNEL, id.toString());

        log.info("[Tenant] 租户版本分配：{} {} -> {}", tenant.getTenantCode(), fromEdition, edition);
        return rows;
    }

    @Override
    public List<SysTenantVersionLogVo> getVersionLogs(Long tenantId) {
        return versionLogMapper.selectVoList(
            Wrappers.<SysTenantVersionLog>lambdaQuery()
                .eq(SysTenantVersionLog::getTenantId, tenantId)
                .orderByDesc(SysTenantVersionLog::getCreateTime));
    }

    @Override
    public SysTenantConfigVo getConfig(Long tenantId, String configKey) {
        return tenantConfigMapper.selectVoOne(
            Wrappers.<SysTenantConfig>lambdaQuery()
                .eq(SysTenantConfig::getTenantId, tenantId)
                .eq(SysTenantConfig::getConfigKey, configKey));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int saveConfig(Long tenantId, SysTenantConfigBo bo) {
        // 查询是否已存在
        SysTenantConfig existing = tenantConfigMapper.selectOne(
            Wrappers.<SysTenantConfig>lambdaQuery()
                .eq(SysTenantConfig::getTenantId, tenantId)
                .eq(SysTenantConfig::getConfigKey, bo.getConfigKey()));

        if (existing != null) {
            // 更新
            existing.setConfigValue(bo.getConfigValue());
            existing.setConfigType(bo.getConfigType());
            existing.setRemark(bo.getRemark());
            return tenantConfigMapper.updateById(existing);
        } else {
            // 新增
            SysTenantConfig config = new SysTenantConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(bo.getConfigKey());
            config.setConfigValue(bo.getConfigValue());
            config.setConfigType(bo.getConfigType());
            config.setRemark(bo.getRemark());
            return tenantConfigMapper.insert(config);
        }
    }

    @Override
    public Map<String, Object> getUsageStats(Long tenantId) {
        Map<String, Object> stats = new HashMap<>();
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException("租户不存在");
        }

        // 基本信息
        stats.put("tenantCode", tenant.getTenantCode());
        stats.put("companyName", tenant.getCompanyName());
        stats.put("edition", tenant.getEdition());
        stats.put("status", tenant.getStatus());
        stats.put("maxUsers", tenant.getMaxUsers());

        // TODO: 查询实际用户数（需要跨租户查询，暂时返回0）
        stats.put("currentUsers", 0);

        // 版本变更次数
        Long versionChangeCount = versionLogMapper.selectCount(
            Wrappers.<SysTenantVersionLog>lambdaQuery()
                .eq(SysTenantVersionLog::getTenantId, tenantId));
        stats.put("versionChangeCount", versionChangeCount);

        // 配置项数量
        Long configCount = tenantConfigMapper.selectCount(
            Wrappers.<SysTenantConfig>lambdaQuery()
                .eq(SysTenantConfig::getTenantId, tenantId));
        stats.put("configCount", configCount);

        return stats;
    }

    /**
     * 自动生成租户编码：T + 企业名称 + - + 当前时间戳后4位
     * 若编码冲突则自动重试，最多3次
     */
    private String generateTenantCode(String companyName) {
        String prefix = "T" + (StrUtil.isNotBlank(companyName) ? companyName.trim() : "Unknown");
        for (int attempt = 0; attempt < 3; attempt++) {
            String suffix = String.format("%04d", System.currentTimeMillis() % 10000);
            String code = prefix + "-" + suffix;
            Long cnt = tenantMapper.selectCount(
                Wrappers.<SysTenant>lambdaQuery().eq(SysTenant::getTenantCode, code));
            if (cnt == null || cnt == 0) {
                return code;
            }
            try { Thread.sleep(10); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        throw new ServiceException("租户编码生成失败，请稍后重试");
    }

    /**
     * 将版本功能列表转换为JSON字符串
     */
    private String convertFeaturesToJson(String edition) {
        try {
            Set<String> features = EditionFeatureConfig.getFeaturesByEdition(edition);
            return objectMapper.writeValueAsString(features);
        } catch (Exception e) {
            log.error("[Tenant] 功能列表序列化失败", e);
            return "[]";
        }
    }

    /**
     * 比较版本等级
     *
     * @return 正数=升级，负数=降级，0=相同
     */
    private int compareEdition(String from, String to) {
        int fromLevel = getEditionLevel(from);
        int toLevel = getEditionLevel(to);
        return toLevel - fromLevel;
    }

    // ==================== 菜单授权模板复制 ====================

    // ==================== 租户级角色克隆 ====================

    /**
     * 为租户克隆预设角色（绑定 tenant_id），并按“角色×版本”模板授权菜单（T2.5）
     * 幂等：租户已存在同标识角色时不重复创建，仅追加缺失菜单；
     * 克隆源为全局共享预设角色 9201~9205（仅复制角色定义，不复制其菜单）
     *
     * @param tenantId 租户ID
     * @param edition  租户版本（BASIC/ADVANCED/PROFESSIONAL）
     * @return roleKey -> 克隆角色ID 映射
     */
    private Map<String, Long> clonePresetRoles(Long tenantId, String edition) {
        Map<String, Long> roleKeyToId = new HashMap<>();
        List<SysRole> presets = roleMapper.selectBatchIds(Arrays.stream(PRESET_ROLE_IDS).boxed().toList());
        for (SysRole preset : presets) {
            // 租户下已存在同标识克隆角色时直接复用（幂等）
            SysRole exists = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleKey, preset.getRoleKey())
                .last("LIMIT 1"));
            Long roleId;
            if (exists != null) {
                roleId = exists.getRoleId();
            } else {
                SysRole role = new SysRole();
                role.setTenantId(tenantId);
                role.setRoleName(preset.getRoleName());
                role.setRoleKey(preset.getRoleKey());
                role.setRoleSort(preset.getRoleSort());
                role.setDataScope(preset.getDataScope());
                role.setMenuCheckStrictly(preset.getMenuCheckStrictly());
                role.setDeptCheckStrictly(preset.getDeptCheckStrictly());
                role.setStatus("0");
                role.setRemark("租户[" + tenantId + "]克隆预设角色，来源:" + preset.getRoleId());
                roleMapper.insert(role);
                roleId = role.getRoleId();
            }
            roleKeyToId.put(preset.getRoleKey(), roleId);
            // 按“角色×版本”模板授权菜单（追加式，幂等）
            copyMenuTemplate(getRoleTemplateKey(preset.getRoleKey(), edition), new long[]{roleId});
        }
        log.info("[Tenant] 租户级角色克隆完成：tenantId={}, edition={}, roles={}", tenantId, edition, roleKeyToId);
        return roleKeyToId;
    }

    /**
     * 获取“角色×版本”菜单模板 key，如 BOSS_BASIC / PLATFORM_ADMIN_ADVANCED
     */
    private String getRoleTemplateKey(String roleKey, String edition) {
        return roleKey.toUpperCase() + "_" + normalizeEdition(edition);
    }

    /**
     * 归一化版本标识（统一委托 {@link EditionFeatureConfig#normalizeEdition(String)}，T10.7）
     * <p>兼容存量大写全名（BASIC/ADVANCED/PROFESSIONAL/ENTERPRISE）与需求文档小写短码（basic/advanced/pro）
     */
    private String normalizeEdition(String edition) {
        return EditionFeatureConfig.normalizeEdition(edition);
    }

    /**
     * 从 sys_edition_menu_template 复制菜单到 sys_role_menu（开通租户时调用）
     * 追加模式：不删除已有记录，仅补充缺失的
     */
    private void copyMenuTemplate(String edition, long[] roleIds) {
        // 1. 查询版本模板中的 menu_id 列表
        List<Long> menuIds = jdbcTemplate.queryForList(
            "SELECT menu_id FROM sys_edition_menu_template WHERE edition = ?",
            Long.class, edition);
        if (menuIds.isEmpty()) {
            log.warn("[Tenant] 版本 {} 无菜单授权模板，跳过复制", edition);
            return;
        }
        // 2. 批量插入 sys_role_menu（跳过已存在的）
        int count = 0;
        for (long roleId : roleIds) {
            for (Long menuId : menuIds) {
                Long exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_role_menu WHERE role_id = ? AND menu_id = ?",
                    Long.class, roleId, menuId);
                if (exists == null || exists == 0) {
                    SysRoleMenu rm = new SysRoleMenu();
                    rm.setRoleId(roleId);
                    rm.setMenuId(menuId);
                    roleMenuMapper.insert(rm);
                    count++;
                }
            }
        }
        log.info("[Tenant] 菜单授权模板复制完成：edition={}, roles={}, 新增 {} 条", edition, roleIds.length, count);
    }

    /**
     * 版本变更时重新复制菜单（先删后插，确保与目标版本严格一致）
     */
    private void reCopyMenuTemplate(String edition, long[] roleIds) {
        // 1. 删除四业务角色的所有菜单授权
        for (long roleId : roleIds) {
            jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        }
        // 2. 按新版本模板重新复制
        copyMenuTemplate(edition, roleIds);
    }

    // ==================== 租户默认数据初始化 ====================

    /**
     * 初始化租户默认数据（从 tenant_id=0 模板复制到新租户）
     */
    private void initTenantDefaultData(Long tenantId) {
        // 1. 初始化租户默认部门树（根部门“总公司”，绑定 tenant_id）
        try {
            Long rootDeptId = System.currentTimeMillis();
            jdbcTemplate.update(
                "INSERT INTO sys_dept (dept_id, tenant_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_time) "
                + "VALUES (?, ?, 0, '0', ?, 0, NULL, '0', '0', NOW())",
                rootDeptId, tenantId, "总公司");
            log.info("[Tenant] 默认部门树已创建：tenantId={}, rootDeptId={}", tenantId, rootDeptId);
        } catch (Exception e) {
            log.warn("[Tenant] 默认部门树创建失败: {}", e.getMessage());
        }

        // 2. 复制项目关联规则模板（fee_project_link_rule tenant_id=0 → 新租户）
        try {
            jdbcTemplate.update(
                "INSERT INTO fee_project_link_rule (tenant_id, scope, target, rule, is_enabled, create_time, update_time) "
                + "SELECT ?, scope, target, rule, is_enabled, NOW(), NOW() "
                + "FROM fee_project_link_rule WHERE tenant_id = 0",
                tenantId);
            log.info("[Tenant] 项目关联规则模板已复制到租户 {}", tenantId);
        } catch (Exception e) {
            log.warn("[Tenant] 项目关联规则复制失败（表可能不存在）: {}", e.getMessage());
        }

        // 3. 复制凭证导出模板（fee_export_template tenant_id=0 → 新租户）
        try {
            jdbcTemplate.update(
                "INSERT INTO fee_export_template (tenant_id, template_code, template_name, target_system, delimiter, is_builtin, sort_order, create_time, update_time) "
                + "SELECT ?, template_code, template_name, target_system, delimiter, is_builtin, sort_order, NOW(), NOW() "
                + "FROM fee_export_template WHERE tenant_id = 0",
                tenantId);
            log.info("[Tenant] 凭证导出模板已复制到租户 {}", tenantId);
        } catch (Exception e) {
            log.warn("[Tenant] 凭证导出模板复制失败（表可能不存在）: {}", e.getMessage());
        }
    }

    private int getEditionLevel(String edition) {
        // T10.7：先归一化再比较，兼容 ENTERPRISE / pro / adv 等别名
        switch (normalizeEdition(edition)) {
            case EditionFeatureConfig.EDITION_PROFESSIONAL:
                return 3;
            case EditionFeatureConfig.EDITION_ADVANCED:
                return 2;
            case EditionFeatureConfig.EDITION_BASIC:
            default:
                return 1;
        }
    }

    // ==================== 企微凭据加解密与脱敏（T10.1） ====================

    /**
     * AES 加密企微密钥（明文→密文落库）；空值直接返回 null
     */
    private String encryptSecret(String plain) {
        if (StrUtil.isBlank(plain)) {
            return null;
        }
        return EncryptUtils.encryptByAes(plain, wecomAesKey);
    }

    /**
     * 接口返回脱敏：企微密钥非空时以占位符 "******" 代替密文，避免密钥外泄
     */
    private void desensitizeSecret(SysTenantVo vo) {
        if (vo != null && StrUtil.isNotBlank(vo.getWecomSecret())) {
            vo.setWecomSecret(SECRET_MASK);
        }
    }
}
