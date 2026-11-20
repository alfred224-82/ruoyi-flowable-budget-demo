package com.ruoyi.system.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.system.domain.IndustryRoleMap;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.domain.TenantRoleMapping;
import com.ruoyi.system.domain.UserTenantRole;
import com.ruoyi.system.domain.bo.UserTenantRoleBo;
import com.ruoyi.system.domain.vo.UserTenantRoleVo;
import com.ruoyi.system.mapper.IndustryRoleMapMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.mapper.TenantRoleMappingMapper;
import com.ruoyi.system.mapper.UserTenantRoleMapper;
import com.ruoyi.system.service.IUserTenantRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户 × 租户 × 角色关联服务实现（T15.9 分配用户）
 * <p>user_tenant_role 租户级自动隔离；分配前校验角色已在本租户 tenant_role_mapping 挑选且启用；
 * 同租户同用户同角色幂等（uk_tenant_user_role）；用户首个角色自动置为主角色。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTenantRoleServiceImpl implements IUserTenantRoleService {

    private final UserTenantRoleMapper userRoleMapper;
    private final TenantRoleMappingMapper mappingMapper;
    private final SysUserMapper userMapper;
    private final IndustryRoleMapMapper industryRoleMapMapper;
    private final SysTenantMapper tenantMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public TableDataInfo<UserTenantRoleVo> queryPageList(UserTenantRoleBo bo, PageQuery pageQuery) {
        // 如果传入了手机号/昵称搜索条件，先从 sys_user 查出匹配的 userId 集合
        List<Long> matchedUserIds = null;
        if (StrUtil.isNotBlank(bo.getPhonenumber()) || StrUtil.isNotBlank(bo.getNickName())) {
            LambdaQueryWrapper<SysUser> userWrapper = Wrappers.<SysUser>lambdaQuery()
                .like(StrUtil.isNotBlank(bo.getPhonenumber()), SysUser::getPhonenumber, bo.getPhonenumber())
                .like(StrUtil.isNotBlank(bo.getNickName()), SysUser::getNickName, bo.getNickName())
                .select(SysUser::getUserId);
            // sys_user 无自动租户隔离，需手动加 tenant_id
            Long currentTenantId = LoginHelper.isAdmin() ? null : LoginHelper.getTenantId();
            if (currentTenantId != null) {
                userWrapper.eq(SysUser::getTenantId, currentTenantId);
            }
            // ★隐藏同租户下的租户管理员账号（platform_admin）
            if (!LoginHelper.isAdmin()) {
                userWrapper.notInSql(SysUser::getUserId,
                    "SELECT sur.user_id FROM sys_user_role sur " +
                    "INNER JOIN sys_role sr ON sur.role_id = sr.role_id " +
                    "WHERE sr.role_key = 'platform_admin' AND sr.del_flag = '0'");
            }
            List<SysUser> matchedUsers = userMapper.selectList(userWrapper);
            if (matchedUsers.isEmpty()) {
                return TableDataInfo.build(new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize()));
            }
            matchedUserIds = matchedUsers.stream().map(SysUser::getUserId).collect(Collectors.toList());
        }
        LambdaQueryWrapper<UserTenantRole> wrapper = Wrappers.<UserTenantRole>lambdaQuery()
            .eq(bo.getUserId() != null, UserTenantRole::getUserId, bo.getUserId())
            .eq(StrUtil.isNotBlank(bo.getRoleCode()), UserTenantRole::getRoleCode, bo.getRoleCode())
            .eq(StrUtil.isNotBlank(bo.getStatus()), UserTenantRole::getStatus, bo.getStatus())
            .in(matchedUserIds != null, UserTenantRole::getUserId, matchedUserIds)
            // ★隐藏同租户下的租户管理员账号（platform_admin），避免租户管理员在人员管理中看到自己角色对应的账号
            .ne(!LoginHelper.isAdmin(), UserTenantRole::getRoleCode, "platform_admin")
            .orderByDesc(UserTenantRole::getCreateTime);
        Page<UserTenantRoleVo> page = userRoleMapper.selectVoPage(pageQuery.build(), wrapper);
        fillDisplay(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<UserTenantRoleVo> listByUser(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<UserTenantRoleVo> list = userRoleMapper.selectVoList(Wrappers.<UserTenantRole>lambdaQuery()
            .eq(UserTenantRole::getUserId, userId)
            .orderByDesc(UserTenantRole::getIsPrimary)
            .orderByAsc(UserTenantRole::getId));
        fillDisplay(list);
        return list;
    }

    @Override
    public String resolveRoleDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        List<UserTenantRole> roles = userRoleMapper.selectList(Wrappers.<UserTenantRole>lambdaQuery()
            .eq(UserTenantRole::getUserId, userId)
            .orderByDesc(UserTenantRole::getIsPrimary)
            .orderByAsc(UserTenantRole::getId));
        if (CollUtil.isEmpty(roles)) {
            return null;
        }
        // tenant_role_mapping 自动按当前租户隔离，作为角色显示名来源
        List<TenantRoleMapping> mappings = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery());
        Map<Long, String> mappingNameMap = mappings.stream()
            .collect(Collectors.toMap(TenantRoleMapping::getId, TenantRoleMapping::getDisplayName, (a, b) -> a));
        Map<String, String> codeNameMap = mappings.stream()
            .collect(Collectors.toMap(TenantRoleMapping::getRoleCode, TenantRoleMapping::getDisplayName, (a, b) -> a));
        for (UserTenantRole role : roles) {
            // ★M15_32 同 fillDisplay 口径：优先按分配时选中映射行取显示名（同 role_code 多业务名时区分导游/司机），空回退 role_code 首名
            if (role.getRoleMappingId() != null && mappingNameMap.containsKey(role.getRoleMappingId())) {
                return mappingNameMap.get(role.getRoleMappingId());
            }
            if (StrUtil.isNotBlank(role.getRoleCode()) && codeNameMap.containsKey(role.getRoleCode())) {
                return codeNameMap.get(role.getRoleCode());
            }
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int assign(UserTenantRoleBo bo) {
        // ★编辑入口（显式传 userId）：同步更新用户资料（密码留空不改/登录账号/头像/昵称/邮箱/性别/描述）
        if (bo.getUserId() != null) {
            SysUser exist = userMapper.selectById(bo.getUserId());
            if (exist == null) {
                throw new ServiceException("用户不存在");
            }
            SysUser update = new SysUser();
            update.setUserId(bo.getUserId());
            boolean changed = false;
            // ★登录账号允许修改（租户内查重），用于修复历史脏数据/账号调整
            if (StrUtil.isNotBlank(bo.getUserName()) && !bo.getUserName().trim().equals(exist.getUserName())) {
                String newUserName = bo.getUserName().trim();
                Long dupCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUserName, newUserName)
                    .eq(SysUser::getDelFlag, "0")
                    .ne(SysUser::getUserId, bo.getUserId())
                    .eq(!LoginHelper.isAdmin(), SysUser::getTenantId, LoginHelper.getTenantId()));
                if (dupCount != null && dupCount > 0) {
                    throw new ServiceException("登录账号「" + newUserName + "」已被其他用户使用，请更换");
                }
                update.setUserName(newUserName);
                changed = true;
            }
            if (StrUtil.isNotBlank(bo.getPassword())) {
                update.setPassword(BCrypt.hashpw(bo.getPassword()));
                changed = true;
            }
            if (bo.getAvatar() != null && !bo.getAvatar().equals(exist.getAvatar())) {
                update.setAvatar(bo.getAvatar());
                changed = true;
            }
            if (StrUtil.isNotBlank(bo.getNickName()) && !bo.getNickName().equals(exist.getNickName())) {
                update.setNickName(bo.getNickName());
                changed = true;
            }
            if (bo.getEmail() != null && !bo.getEmail().equals(exist.getEmail())) {
                update.setEmail(bo.getEmail());
                changed = true;
            }
            if (StrUtil.isNotBlank(bo.getSex()) && !bo.getSex().equals(exist.getSex())) {
                update.setSex(bo.getSex());
                changed = true;
            }
            if (bo.getDescription() != null && !bo.getDescription().equals(exist.getRemark())) {
                update.setRemark(bo.getDescription());
                changed = true;
            }
            if (changed) {
                update.setUpdateBy(LoginHelper.getUsername());
                userMapper.updateById(update);
                log.info("[UserManage] 编辑入口更新用户资料：userId={}", bo.getUserId());
            }
        }
        // ★platform_admin 新增人员入口：支持按手机号/登录账号查找或自动创建用户
        if (bo.getUserId() == null && (StrUtil.isNotBlank(bo.getPhonenumber()) || StrUtil.isNotBlank(bo.getUserName()))) {
            // sys_user 无自动租户隔离，需手动加 tenant_id 条件
            Long currentTenantId = LoginHelper.isAdmin() ? null : LoginHelper.getTenantId();
            String phone = StrUtil.isNotBlank(bo.getPhonenumber()) ? bo.getPhonenumber().trim() : null;
            String loginName = StrUtil.isNotBlank(bo.getUserName()) ? bo.getUserName().trim() : null;
            // ① 优先按手机号查找，未命中再按登录账号查找。
            //   ★不过滤 del_flag 且启用优先（orderByAsc del_flag）：命中逻辑删除残留用户（del_flag='2'）时
            //     走复活复用而非 INSERT，修复「删除后重新建立相同账号报已存在」（新插入与残留记录冲突）
            SysUser existUser = null;
            if (phone != null) {
                existUser = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getPhonenumber, phone)
                    .eq(currentTenantId != null, SysUser::getTenantId, currentTenantId)
                    .orderByAsc(SysUser::getDelFlag)
                    .last("LIMIT 1"));
            }
            if (existUser == null && loginName != null) {
                existUser = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                    .eq(SysUser::getUserName, loginName)
                    .eq(currentTenantId != null, SysUser::getTenantId, currentTenantId)
                    .orderByAsc(SysUser::getDelFlag)
                    .last("LIMIT 1"));
            }
            if (existUser != null) {
                // ★安全护栏：命中用户在本租户仍有角色分配（如 boss 等存量人员）时禁止静默复用覆盖，
                //   修复「新建导游误改存量人员信息」；引导管理员直接在列表中修改该人员
                Long activeRoleCount = userRoleMapper.selectCount(Wrappers.<UserTenantRole>lambdaQuery()
                    .eq(UserTenantRole::getUserId, existUser.getUserId()));
                if (activeRoleCount != null && activeRoleCount > 0) {
                    throw new ServiceException("手机号或登录账号已被本租户人员「" + existUser.getNickName()
                        + "」使用，如需调整角色请直接修改该人员");
                }
                bo.setUserId(existUser.getUserId());
                // ★复用/复活残留用户：全量同步提交的资料（含头像/密码/邮箱/性别/描述），
                //   修复旧逻辑仅同步账号/昵称导致「头像要上传两遍才生效」
                syncProfileOnReuse(existUser, bo, loginName, phone, currentTenantId);
            } else {
                // 手机号与登录账号均未占用 → 自动创建用户（状态正常）
                // ★登录用户名：优先用传入值，留空则用手机号
                String loginUserName = loginName != null ? loginName : phone;
                // ★登录密码：优先用传入值，留空则默认 123456
                String rawPassword = StrUtil.isNotBlank(bo.getPassword()) ? bo.getPassword() : "123456";
                SysUser newUser = new SysUser();
                newUser.setUserName(loginUserName);
                newUser.setNickName(StrUtil.isNotBlank(bo.getNickName()) ? bo.getNickName() : phone);
                newUser.setPhonenumber(phone);
                newUser.setPassword(BCrypt.hashpw(rawPassword));
                newUser.setUserType("sys_user");
                newUser.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "0");
                newUser.setCreateBy(LoginHelper.getUsername());
                if (currentTenantId != null) {
                    newUser.setTenantId(currentTenantId);
                }
                // 可选字段：头像/邮箱/性别/描述
                if (StrUtil.isNotBlank(bo.getAvatar())) {
                    newUser.setAvatar(bo.getAvatar());
                }
                if (StrUtil.isNotBlank(bo.getEmail())) {
                    newUser.setEmail(bo.getEmail());
                }
                if (StrUtil.isNotBlank(bo.getSex())) {
                    newUser.setSex(bo.getSex());
                }
                if (StrUtil.isNotBlank(bo.getDescription())) {
                    newUser.setRemark(bo.getDescription());
                }
                userMapper.insert(newUser);
                bo.setUserId(newUser.getUserId());
                log.info("[UserManage] 按手机号自动创建用户：userId={}, userName={}, phone={}, nickName={}, tenantId={}",
                    newUser.getUserId(), loginUserName, newUser.getPhonenumber(), newUser.getNickName(), currentTenantId);
            }
        }
        if (bo.getUserId() == null) {
            throw new ServiceException("用户不能为空");
        }
        List<String> codes = CollUtil.isNotEmpty(bo.getRoleCodes()) ? bo.getRoleCodes()
            : (StrUtil.isNotBlank(bo.getRoleCode()) ? Collections.singletonList(bo.getRoleCode()) : Collections.emptyList());
        if (codes.isEmpty()) {
            throw new ServiceException("请至少指定一个角色");
        }
        // ★M15_32 人员管理入口：记录具体所选业务角色映射行（同 role_code 多业务名，如导游/司机）
        Long assignedMappingId = bo.getRoleMappingId();
        String assignedMappingCode = null;
        if (assignedMappingId != null) {
            TenantRoleMapping assigned = mappingMapper.selectById(assignedMappingId);
            if (assigned == null) {
                throw new ServiceException("所选角色映射不存在，请刷新后重试");
            }
            if ("1".equals(assigned.getStatus())) {
                throw new ServiceException("角色「" + assigned.getDisplayName() + "」已禁用，不能分配");
            }
            if (!codes.contains(assigned.getRoleCode())) {
                throw new ServiceException("所选角色映射与角色编码不一致，请重新选择");
            }
            assignedMappingCode = assigned.getRoleCode();
        }
        // 校验角色已在本租户挑选且启用
        Map<String, TenantRoleMapping> rawMap = mappingMapper.selectList(
                Wrappers.<TenantRoleMapping>lambdaQuery().in(TenantRoleMapping::getRoleCode, codes))
            .stream().collect(Collectors.toMap(TenantRoleMapping::getRoleCode, m -> m, (a, b) -> a));
        // ★自愈：缺失映射时自动从 industry_role_map 补录（避免必须先手动进入「业务角色管理」挑选）
        List<String> missingCodes = codes.stream().filter(c -> !rawMap.containsKey(c)).collect(Collectors.toList());
        if (!missingCodes.isEmpty()) {
            int healed = healMissingMappings(missingCodes);
            if (healed > 0) {
                // 重新加载映射
                Map<String, TenantRoleMapping> reloaded = mappingMapper.selectList(
                        Wrappers.<TenantRoleMapping>lambdaQuery().in(TenantRoleMapping::getRoleCode, codes))
                    .stream().collect(Collectors.toMap(TenantRoleMapping::getRoleCode, m -> m, (a, b) -> a));
                rawMap.putAll(reloaded);
            }
        }
        for (String code : codes) {
            TenantRoleMapping m = rawMap.get(code);
            if (m == null) {
                throw new ServiceException("角色 " + code + " 未在本租户挑选，请先在「业务角色管理」中挑选");
            }
            if ("1".equals(m.getStatus())) {
                throw new ServiceException("角色「" + m.getDisplayName() + "」已禁用，不能分配");
            }
        }
        // ★修复软删除唯一约束冲突：物理删除该用户的所有软删除记录
        // 原因：@TableLogic 软删除后记录仍存在于数据库，uk_tenant_user_role 唯一约束不包含 del_flag
        // 导致重新分配同一用户+角色时 INSERT 失败（Duplicate entry）
        physicalDeleteSoftDeletedRoles(bo.getUserId());

        // 现有角色：判断首个角色（自动置主）+ 幂等 upsert 复用
        List<UserTenantRole> existingRoles = userRoleMapper.selectList(Wrappers.<UserTenantRole>lambdaQuery()
            .eq(UserTenantRole::getUserId, bo.getUserId()));
        Map<String, UserTenantRole> existMap = existingRoles.stream()
            .collect(Collectors.toMap(UserTenantRole::getRoleCode, r -> r, (a, b) -> a));
        boolean firstRole = existingRoles.isEmpty();
        int rows = 0;
        int seq = 0;
        for (String code : codes) {
            seq++;
            Integer primary = bo.getIsPrimary() != null ? bo.getIsPrimary() : ((firstRole && seq == 1) ? 1 : 0);
            UserTenantRole exist = existMap.get(code);
            if (exist == null) {
                UserTenantRole entity = new UserTenantRole();
                entity.setUserId(bo.getUserId());
                entity.setRoleCode(code);
                entity.setRoleMappingId(code.equals(assignedMappingCode) ? assignedMappingId : null);
                entity.setIsPrimary(primary);
                entity.setStatus(StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "0");
                entity.setRemark(bo.getRemark());
                rows += userRoleMapper.insert(entity);
            } else {
                UserTenantRole update = new UserTenantRole();
                update.setId(exist.getId());
                update.setIsPrimary(primary);
                if (code.equals(assignedMappingCode)) {
                    update.setRoleMappingId(assignedMappingId);
                }
                if (StrUtil.isNotBlank(bo.getStatus())) {
                    update.setStatus(bo.getStatus());
                }
                rows += userRoleMapper.updateById(update);
            }
            if (primary != null && primary == 1) {
                clearOtherPrimary(bo.getUserId(), code);
            }
        }
        // ★同步 sys_user_role（legacy 表），确保登录时 getRolePermission() 回退查询结果与 user_tenant_role 一致
        // 根因：assign() 此前只写 user_tenant_role，但登录流程 SysPermissionService.getRolePermission()
        //       通过 sys_user_role → sys_role 读取角色权限，两表不一致导致用户拿到错误角色的菜单
        syncSysUserRole(bo.getUserId(), codes);

        return rows;
    }

    @Override
    public int removeById(Long id) {
        if (id == null) {
            throw new ServiceException("关联ID不能为空");
        }
        UserTenantRole target = userRoleMapper.selectById(id);
        int rows = userRoleMapper.deleteById(id);
        // ★移除后同步 sys_user_role（legacy 表）：避免被移除人员仍残留克隆角色权限可登录
        if (rows > 0 && target != null) {
            syncSysUserRole(target.getUserId(), Collections.emptyList());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int setPrimary(Long id) {
        UserTenantRole target = userRoleMapper.selectById(id);
        if (target == null) {
            throw new ServiceException("用户角色关联不存在");
        }
        clearOtherPrimary(target.getUserId(), target.getRoleCode());
        UserTenantRole update = new UserTenantRole();
        update.setId(id);
        update.setIsPrimary(1);
        return userRoleMapper.updateById(update);
    }

    /**
     * ★自愈：缺失的角色映射从 industry_role_map 按当前租户行业自动补录。
     * <p>仅补录 codes 中缺失的角色（不影响其它已有映射），幂等（uk_tenant_role_code_name 去重）。
     * 失败 fail-soft：返回 0，由调用方决定是否继续抛异常。
     */
    private int healMissingMappings(List<String> missingCodes) {
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return 0;
        }
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null || StrUtil.isBlank(tenant.getIndustry())) {
            return 0;
        }
        // 查本行业的全部角色映射（系统级共享表，无租户过滤）
        List<IndustryRoleMap> roleMaps = industryRoleMapMapper.selectList(Wrappers.<IndustryRoleMap>lambdaQuery()
            .eq(IndustryRoleMap::getIndustryCode, tenant.getIndustry()));
        if (roleMaps.isEmpty()) {
            return 0;
        }
        // 本租户已有映射（手动按 tenantId 过滤，admin 上下文插件可能忽略过滤）
        Set<String> existingCodes = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery()
                .eq(TenantRoleMapping::getTenantId, tenantId))
            .stream().map(TenantRoleMapping::getRoleCode).collect(Collectors.toSet());
        int rows = 0;
        int baseSort = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery()
                .eq(TenantRoleMapping::getTenantId, tenantId))
            .stream().map(m -> m.getSortOrder() == null ? 0 : m.getSortOrder())
            .max(Integer::compareTo).orElse(0);
        Set<String> missingSet = new java.util.HashSet<>(missingCodes);
        for (IndustryRoleMap rm : roleMaps) {
            String roleCode = rm.getRoleCode();
            // 仅补录缺失的 + 未重复的
            if (!missingSet.contains(roleCode) || existingCodes.contains(roleCode)) {
                continue;
            }
            TenantRoleMapping entity = new TenantRoleMapping();
            entity.setTenantId(tenantId);
            entity.setRoleCode(roleCode);
            entity.setDisplayName(StrUtil.isNotBlank(rm.getBizRoleName()) ? rm.getBizRoleName() : roleCode);
            entity.setSortOrder(rm.getSortOrder() != null ? rm.getSortOrder() : ++baseSort);
            entity.setStatus(rm.getDefaultFlag() != null && rm.getDefaultFlag() == 1 ? "0" : "1");
            try {
                rows += mappingMapper.insert(entity);
                existingCodes.add(roleCode);
            } catch (Exception e) {
                // uk_tenant_role_code_name 冲突 → 跳过（幂等）
                log.debug("[UserManage] 自愈跳过已存在映射：tenantId={}, roleCode={}", tenantId, roleCode);
            }
        }
        if (rows > 0) {
            log.info("[UserManage] 自愈：已为租户 {} 补录 {} 条缺失角色映射（industry={}）",
                tenantId, rows, tenant.getIndustry());
        }
        return rows;
    }

    /**
     * ★新增入口复用已有用户时全量同步提交的资料（修复旧逻辑仅同步账号/昵称，
     *   静默丢弃头像/密码/邮箱/性别/描述，导致「头像要上传两遍才能替换」）。
     * <p>命中逻辑删除残留用户（del_flag='2'）时先复活（恢复 del_flag='0'）再复用，
     *   避免新 INSERT 与残留记录冲突报「已存在」。
     */
    private void syncProfileOnReuse(SysUser existUser, UserTenantRoleBo bo, String loginName, String phone, Long currentTenantId) {
        boolean revive = !"0".equals(existUser.getDelFlag());
        if (revive) {
            // @TableLogic 字段不参与 updateById，需 lambdaUpdate 显式恢复 del_flag
            userMapper.update(null, Wrappers.<SysUser>lambdaUpdate()
                .set(SysUser::getDelFlag, "0")
                .set(SysUser::getStatus, StrUtil.isNotBlank(bo.getStatus()) ? bo.getStatus() : "0")
                .eq(SysUser::getUserId, existUser.getUserId()));
            log.info("[UserManage] 复活逻辑删除残留用户并复用：userId={}, userName={}",
                existUser.getUserId(), existUser.getUserName());
        }
        SysUser update = new SysUser();
        update.setUserId(existUser.getUserId());
        boolean changed = false;
        if (StrUtil.isNotBlank(loginName) && !loginName.equals(existUser.getUserName())) {
            // 校验新用户名未被本租户其他启用用户占用（sys_user 无自动租户隔离，需手动过滤）
            Long dupCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUserName, loginName)
                .eq(SysUser::getDelFlag, "0")
                .ne(SysUser::getUserId, existUser.getUserId())
                .eq(currentTenantId != null, SysUser::getTenantId, currentTenantId));
            if (dupCount != null && dupCount > 0) {
                throw new ServiceException("登录账号「" + loginName + "」已被其他用户使用，请更换");
            }
            update.setUserName(loginName);
            changed = true;
        }
        if (StrUtil.isNotBlank(phone) && !phone.equals(existUser.getPhonenumber())) {
            // 校验手机号未被本租户其他启用用户占用（手机号为租户内唯一标识，跨租户允许同号）
            Long dupCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getPhonenumber, phone)
                .eq(SysUser::getDelFlag, "0")
                .ne(SysUser::getUserId, existUser.getUserId())
                .eq(currentTenantId != null, SysUser::getTenantId, currentTenantId));
            if (dupCount != null && dupCount > 0) {
                throw new ServiceException("手机号「" + phone + "」已被其他用户使用，请更换");
            }
            update.setPhonenumber(phone);
            changed = true;
        }
        if (StrUtil.isNotBlank(bo.getNickName()) && !bo.getNickName().equals(existUser.getNickName())) {
            update.setNickName(bo.getNickName());
            changed = true;
        }
        if (StrUtil.isNotBlank(bo.getPassword())) {
            update.setPassword(BCrypt.hashpw(bo.getPassword()));
            changed = true;
        }
        if (StrUtil.isNotBlank(bo.getAvatar()) && !bo.getAvatar().equals(existUser.getAvatar())) {
            update.setAvatar(bo.getAvatar());
            changed = true;
        }
        if (bo.getEmail() != null && !bo.getEmail().equals(existUser.getEmail())) {
            update.setEmail(bo.getEmail());
            changed = true;
        }
        if (StrUtil.isNotBlank(bo.getSex()) && !bo.getSex().equals(existUser.getSex())) {
            update.setSex(bo.getSex());
            changed = true;
        }
        if (bo.getDescription() != null && !bo.getDescription().equals(existUser.getRemark())) {
            update.setRemark(bo.getDescription());
            changed = true;
        }
        if (changed) {
            update.setUpdateBy(LoginHelper.getUsername());
            userMapper.updateById(update);
            log.info("[UserManage] 复用用户同步资料：userId={}, revive={}", existUser.getUserId(), revive);
        }
    }

    /**
     * ★物理删除指定用户的所有软删除记录（解决 uk_tenant_user_role 唯一约束冲突）
     * <p>场景：用户被移除（软删除 del_flag='2'）后重新分配同一角色时，
     * 软删除记录仍占用唯一约束，导致 INSERT 失败。
     * <p>兼容历史脏数据中 del_flag='1' 等非 '0' 取值，统一清理。
     * <p>使用原生 @Delete SQL 绕过 @TableLogic 拦截。
     */
    private void physicalDeleteSoftDeletedRoles(Long userId) {
        if (userId == null) {
            return;
        }
        int deleted = userRoleMapper.physicalDeleteSoftDeleted(userId);
        if (deleted > 0) {
            log.info("[UserManage] 物理删除用户 {} 的 {} 条软删除记录（避免唯一约束冲突）", userId, deleted);
        }
    }

    /**
     * 清除同一用户在本租户下、除 keepRoleCode 外的其它主角色标记（保证主角色唯一）
     */
    private void clearOtherPrimary(Long userId, String keepRoleCode) {
        List<UserTenantRole> others = userRoleMapper.selectList(Wrappers.<UserTenantRole>lambdaQuery()
            .eq(UserTenantRole::getUserId, userId)
            .eq(UserTenantRole::getIsPrimary, 1)
            .ne(UserTenantRole::getRoleCode, keepRoleCode));
        for (UserTenantRole o : others) {
            UserTenantRole update = new UserTenantRole();
            update.setId(o.getId());
            update.setIsPrimary(0);
            userRoleMapper.updateById(update);
        }
    }

    /**
     * ★同步 sys_user_role（legacy 表）与 user_tenant_role 保持一致
     * <p>根因：assign() 只写 user_tenant_role，但登录时 SysPermissionService.getRolePermission()
     *       通过 sys_user_role → sys_role 读取角色权限（SysRoleMapper.xml selectRolePermissionByUserId）。
     *       两表不一致会导致用户拿到错误角色的菜单（如 boss 用户看到 platform_admin 的「租户管理菜单」）。
     * <p>策略：删除该用户在本租户克隆角色上的 sys_user_role 关联，再按当前分配的 roleCodes 重新插入。
     *       仅操作本租户克隆角色（tenant_id = currentTenantId），不影响其他租户或全局共享角色。
     */
    private void syncSysUserRole(Long userId, List<String> assignedRoleCodes) {
        if (userId == null) {
            return;
        }
        Long currentTenantId = LoginHelper.isAdmin() ? null : LoginHelper.getTenantId();
        if (currentTenantId == null) {
            return;
        }
        try {
            // 1. ★同步基准 = user_tenant_role 全量启用角色 ∪ 本次传入 codes（M21_07）：
            //    仅按本次传入 codes 重写会误删未传入角色（如租户管理员固有的 platform_admin）
            //    的 sys_user_role 关联，导致管理员变成无角色用户（首页兜底落企业版看板）
            List<String> effectiveCodes = userRoleMapper.selectList(Wrappers.<UserTenantRole>lambdaQuery()
                    .eq(UserTenantRole::getUserId, userId)
                    .eq(UserTenantRole::getTenantId, currentTenantId))
                .stream().filter(r -> !"1".equals(r.getStatus()))
                .map(UserTenantRole::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
            if (CollUtil.isNotEmpty(assignedRoleCodes)) {
                for (String code : assignedRoleCodes) {
                    if (!effectiveCodes.contains(code)) {
                        effectiveCodes.add(code);
                    }
                }
            }
            // 2. 查本租户所有克隆角色的 role_id（tenant_id = currentTenantId 的角色均为克隆角色）
            List<SysRole> tenantRoles = sysRoleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getTenantId, currentTenantId));
            if (tenantRoles.isEmpty()) {
                return;
            }
            // 3. 构建 roleCode → roleId 映射
            Map<String, Long> codeToIdMap = tenantRoles.stream()
                .collect(Collectors.toMap(SysRole::getRoleKey,
                    SysRole::getRoleId, (a, b) -> a));
            List<Long> tenantRoleIds = tenantRoles.stream()
                .map(SysRole::getRoleId)
                .collect(Collectors.toList());

            // 4. 删除该用户在所有本租户克隆角色上的 sys_user_role 关联
            //   ★先删再判空：人员被移除后无启用角色时也需清空 legacy 关联，避免残留权限
            sysUserRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .in(SysUserRole::getRoleId, tenantRoleIds));
            if (effectiveCodes.isEmpty()) {
                log.info("[UserManage] 同步 sys_user_role：userId={}, tenantId={}, 已清空（无启用角色）", userId, currentTenantId);
                return;
            }

            // 5. 按全量启用角色重新插入 sys_user_role
            for (String code : effectiveCodes) {
                Long roleId = codeToIdMap.get(code);
                if (roleId != null) {
                    SysUserRole userRole = new SysUserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    sysUserRoleMapper.insert(userRole);
                }
            }
            log.info("[UserManage] 同步 sys_user_role：userId={}, tenantId={}, roles={}",
                userId, currentTenantId, effectiveCodes);
        } catch (Exception e) {
            // 同步失败不阻断主流程（user_tenant_role 已写入成功）
            log.warn("[UserManage] 同步 sys_user_role 失败 userId={}, tenantId={}: {}",
                userId, currentTenantId, e.getMessage());
        }
    }

    /**
     * 批量回填用户账号/昵称/手机号（sys_user）+ 租户内角色显示名（tenant_role_mapping.display_name）
     */
    private void fillDisplay(List<UserTenantRoleVo> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> userIds = list.stream().map(UserTenantRoleVo::getUserId)
            .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Collections.emptyMap()
            : userMapper.selectList(Wrappers.<SysUser>lambdaQuery().in(SysUser::getUserId, userIds))
            .stream().collect(Collectors.toMap(SysUser::getUserId, u -> u, (a, b) -> a));
        // tenant_role_mapping 自动按当前租户隔离，作为角色显示名来源
        List<TenantRoleMapping> mappings = mappingMapper.selectList(Wrappers.<TenantRoleMapping>lambdaQuery());
        Map<String, String> displayNameMap = mappings
            .stream().collect(Collectors.toMap(TenantRoleMapping::getRoleCode, TenantRoleMapping::getDisplayName, (a, b) -> a));
        // ★M15_32 映射ID→显示名：分配时选中具体映射行（导游/司机）时优先按其显示
        Map<Long, String> mappingNameMap = mappings
            .stream().collect(Collectors.toMap(TenantRoleMapping::getId, TenantRoleMapping::getDisplayName, (a, b) -> a));
        for (UserTenantRoleVo vo : list) {
            SysUser u = userMap.get(vo.getUserId());
            if (u != null) {
                vo.setUserName(u.getUserName());
                vo.setNickName(u.getNickName());
                vo.setPhonenumber(u.getPhonenumber());
                vo.setAvatar(u.getAvatar());
                vo.setEmail(u.getEmail());
                vo.setSex(u.getSex());
                vo.setDescription(u.getRemark());
            }
            String roleName = displayNameMap.getOrDefault(vo.getRoleCode(), vo.getRoleCode());
            if (vo.getRoleMappingId() != null && mappingNameMap.containsKey(vo.getRoleMappingId())) {
                roleName = mappingNameMap.get(vo.getRoleMappingId());
            }
            vo.setRoleName(roleName);
        }
    }
}
