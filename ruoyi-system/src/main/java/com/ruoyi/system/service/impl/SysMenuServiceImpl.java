package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StreamUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TreeBuildUtils;
import com.ruoyi.system.domain.SysRoleMenu;
import com.ruoyi.system.domain.SysTenant;
import com.ruoyi.system.domain.vo.MetaVo;
import com.ruoyi.system.domain.vo.RouterVo;
import com.ruoyi.system.domain.vo.SysTenantVo;
import com.ruoyi.system.mapper.SysMenuMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysRoleMenuMapper;
import com.ruoyi.system.mapper.SysTenantMapper;
import com.ruoyi.system.service.IBizBundleService;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysTenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 菜单 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper baseMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    /** T2.2 业态菜单过滤：biz_bundle.menus 叠加在 sys_role_menu 之上 */
    private final IBizBundleService bizBundleService;
    private final ISysTenantService tenantService;
    /** 租户到期菜单过滤：EXPIRED 租户管理员仅保留版本升级菜单 */
    private final SysTenantMapper tenantMapper;

    /** 过期租户管理员可见菜单（版本升级目录 + 升级页面） */
    private static final Set<Long> EXPIRED_ADMIN_MENU_IDS = new HashSet<>(Arrays.asList(7500L, 7501L));

    /**
     * 根据用户查询系统菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(Long userId) {
        return selectMenuList(new SysMenu(), userId);
    }

    /**
     * 查询系统菜单列表
     *
     * @param menu 菜单信息
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuList(SysMenu menu, Long userId) {
        List<SysMenu> menuList = null;
        // 管理员显示所有菜单信息
        if (LoginHelper.isAdmin(userId)) {
            menuList = baseMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .like(StringUtils.isNotBlank(menu.getMenuName()), SysMenu::getMenuName, menu.getMenuName())
                .eq(StringUtils.isNotBlank(menu.getVisible()), SysMenu::getVisible, menu.getVisible())
                .eq(StringUtils.isNotBlank(menu.getStatus()), SysMenu::getStatus, menu.getStatus())
                .orderByAsc(SysMenu::getParentId)
                .orderByAsc(SysMenu::getOrderNum));
        } else {
            QueryWrapper<SysMenu> wrapper = Wrappers.query();
            wrapper.eq("sur.user_id", userId)
                .like(StringUtils.isNotBlank(menu.getMenuName()), "m.menu_name", menu.getMenuName())
                .eq(StringUtils.isNotBlank(menu.getVisible()), "m.visible", menu.getVisible())
                .eq(StringUtils.isNotBlank(menu.getStatus()), "m.status", menu.getStatus())
                .orderByAsc("m.parent_id")
                .orderByAsc("m.order_num");
            menuList = baseMapper.selectMenuListByUserId(wrapper);
        }
        return menuList;
    }

    /**
     * 根据用户ID查询权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByUserId(Long userId) {
        List<String> perms = baseMapper.selectMenuPermsByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.trim()));
            }
        }
        return permsSet;
    }

    /**
     * 根据角色ID查询权限
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectMenuPermsByRoleId(Long roleId) {
        List<String> perms = baseMapper.selectMenuPermsByRoleId(roleId);
        Set<String> permsSet = new HashSet<>();
        for (String perm : perms) {
            if (StringUtils.isNotEmpty(perm)) {
                permsSet.addAll(StringUtils.splitList(perm.trim()));
            }
        }
        return permsSet;
    }

    /**
     * 根据用户ID查询菜单
     *
     * @param userId 用户名称
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId) {
        List<SysMenu> menus = null;
        if (LoginHelper.isAdmin(userId)) {
            menus = baseMapper.selectMenuTreeAll();
        } else {
            menus = baseMapper.selectMenuTreeByUserId(userId);
        }
        // 过期租户管理员仅保留版本升级菜单
        menus = applyExpiredTenantFilter(menus, userId);
        return getChildPerms(menus, 0);
    }

    /**
     * 根据用户ID + 客户端类型查询菜单树（T15.8）
     * <p>在角色可见菜单基础上再按 client_type 端过滤：保留与 clientType 一致及 both 通用的菜单；
     * clientType 为空则不过滤（兼容存量 PC /getRouters）。client_type 为空的菜单视为 both 通用。
     * <p><b>T2.2 业态过滤层</b>：在 client_type 过滤后再叠加 biz_bundle.menus 业态声明过滤，
     * 仅保留当前租户业态包声明的菜单及其祖先节点；未声明 menus（空集）时跳过业态过滤（兼容存量包）；
     * admin 超管不受业态过滤限制。
     *
     * @param userId     用户ID
     * @param clientType 客户端类型（pc/mini/both，可空）
     * @return 菜单列表
     */
    @Override
    public List<SysMenu> selectMenuTreeByUserId(Long userId, String clientType) {
        List<SysMenu> menus;
        if (LoginHelper.isAdmin(userId)) {
            menus = baseMapper.selectMenuTreeAll();
        } else {
            menus = baseMapper.selectMenuTreeByUserId(userId);
        }
        if (StringUtils.isNotBlank(clientType)) {
            menus = StreamUtils.filter(menus, m -> StringUtils.isBlank(m.getClientType())
                || clientType.equalsIgnoreCase(m.getClientType())
                || "both".equalsIgnoreCase(m.getClientType()));
        }
        // T2.2 业态过滤层：admin 免检；非 admin 且业态包声明了 menus 时叠加过滤
        if (!LoginHelper.isAdmin(userId)) {
            menus = applyBizFilter(menus);
        }
        // 过期租户管理员仅保留版本升级菜单
        menus = applyExpiredTenantFilter(menus, userId);
        return getChildPerms(menus, 0);
    }

    /**
     * T2.2 业态菜单过滤：取当前租户 biz_code 对应 biz_bundle.menus 交集
     * <p>规则：
     * <ol>
     *   <li>tenantId 为空 / 业态包未声明 menus（空集） → 跳过过滤，原样返回；</li>
     *   <li>保留 menuId ∈ 声明集的菜单，并递归保留其祖先目录节点（parentId 链），
     *       避免树结构断裂导致子菜单无父容器；</li>
     *   <li>祖先节点自身不满足业态声明时也保留，因前端 buildMenus 依赖 parent→child 层级。</li>
     * </ol>
     */
    private List<SysMenu> applyBizFilter(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return menus;
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return menus;
        }
        SysTenantVo tenant;
        try {
            tenant = tenantService.queryById(tenantId);
        } catch (Exception e) {
            // 租户服务异常时保守放行，由 ②③ 层 API 拦截兼底
            return menus;
        }
        if (tenant == null) {
            return menus;
        }
        String bizCode = StringUtils.isNotBlank(tenant.getBizCode())
            ? tenant.getBizCode() : BizBundleServiceImpl.DEFAULT_BIZ_CODE;
        Set<Long> allowed = bizBundleService.getMenuIds(bizCode);
        if (CollUtil.isEmpty(allowed)) {
            // 未声明 menus → 不做业态过滤（兼容存量默认包 fund_control）
            return menus;
        }
        // 建 menuId → SysMenu 索引，用于回溯祖先链
        Map<Long, SysMenu> index = new HashMap<>(menus.size());
        for (SysMenu m : menus) {
            if (m != null && m.getMenuId() != null) {
                index.put(m.getMenuId(), m);
            }
        }
        Set<Long> keep = new HashSet<>();
        for (SysMenu m : menus) {
            if (m == null || m.getMenuId() == null) {
                continue;
            }
            if (allowed.contains(m.getMenuId())) {
                // 保留自身 + 全部祖先
                Long cursor = m.getMenuId();
                int guard = 0;
                while (cursor != null && cursor != 0L && guard++ < 32) {
                    keep.add(cursor);
                    SysMenu node = index.get(cursor);
                    if (node == null) {
                        break;
                    }
                    cursor = node.getParentId();
                }
            }
        }
        if (keep.isEmpty()) {
            // 声明集与可见菜单无交集（可能业态包声明错误）：不返回空菜单以免锁死页面，
            // 记录告警后回退到未过滤状态，由 API 拦截层兼底。
            return menus;
        }
        return StreamUtils.filter(menus, m -> m != null && m.getMenuId() != null && keep.contains(m.getMenuId()));
    }

    /**
     * 根据角色ID查询菜单树信息
     *
     * @param roleId 角色ID
     * @return 选中菜单列表
     */
    @Override
    public List<Long> selectMenuListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        return baseMapper.selectMenuListByRoleId(roleId, role.getMenuCheckStrictly());
    }

    /**
     * 构建前端路由所需要的菜单
     *
     * @param menus 菜单列表
     * @return 路由列表
     */
    @Override
    public List<RouterVo> buildMenus(List<SysMenu> menus) {
        List<RouterVo> routers = new LinkedList<>();
        for (SysMenu menu : menus) {
            RouterVo router = new RouterVo();
            router.setHidden("1".equals(menu.getVisible()));
            router.setName(getRouteName(menu));
            router.setPath(getRouterPath(menu));
            router.setComponent(getComponent(menu));
            router.setQuery(menu.getQueryParam());
            router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
            List<SysMenu> cMenus = menu.getChildren();
            if (CollUtil.isNotEmpty(cMenus) && UserConstants.TYPE_DIR.equals(menu.getMenuType())) {
                router.setAlwaysShow(true);
                router.setRedirect("noRedirect");
                router.setChildren(buildMenus(cMenus));
            } else if (isMenuFrame(menu)) {
                router.setMeta(null);
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                children.setPath(menu.getPath());
                children.setComponent(menu.getComponent());
                children.setName(StringUtils.capitalize(menu.getPath()));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), StringUtils.equals("1", menu.getIsCache()), menu.getPath()));
                children.setQuery(menu.getQueryParam());
                childrenList.add(children);
                router.setChildren(childrenList);
            } else if (menu.getParentId().intValue() == 0 && isInnerLink(menu)) {
                router.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon()));
                router.setPath("/");
                List<RouterVo> childrenList = new ArrayList<>();
                RouterVo children = new RouterVo();
                String routerPath = innerLinkReplaceEach(menu.getPath());
                children.setPath(routerPath);
                children.setComponent(UserConstants.INNER_LINK);
                children.setName(StringUtils.capitalize(routerPath));
                children.setMeta(new MetaVo(menu.getMenuName(), menu.getIcon(), menu.getPath()));
                childrenList.add(children);
                router.setChildren(childrenList);
            }
            routers.add(router);
        }
        return routers;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param menus 菜单列表
     * @return 下拉树结构列表
     */
    @Override
    public List<Tree<Long>> buildMenuTreeSelect(List<SysMenu> menus) {
        if (CollUtil.isEmpty(menus)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.build(menus, (menu, tree) ->
            tree.setId(menu.getMenuId())
                .setParentId(menu.getParentId())
                .setName(menu.getMenuName())
                .setWeight(menu.getOrderNum()));
    }

    /**
     * 根据菜单ID查询信息
     *
     * @param menuId 菜单ID
     * @return 菜单信息
     */
    @Override
    public SysMenu selectMenuById(Long menuId) {
        return baseMapper.selectById(menuId);
    }

    /**
     * 是否存在菜单子节点
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean hasChildByMenuId(Long menuId) {
        return baseMapper.exists(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
    }

    /**
     * 查询菜单使用数量
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public boolean checkMenuExistRole(Long menuId) {
        return roleMenuMapper.exists(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, menuId));
    }

    /**
     * 新增保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int insertMenu(SysMenu menu) {
        return baseMapper.insert(menu);
    }

    /**
     * 修改保存菜单信息
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public int updateMenu(SysMenu menu) {
        return baseMapper.updateById(menu);
    }

    /**
     * 删除菜单管理信息
     *
     * @param menuId 菜单ID
     * @return 结果
     */
    @Override
    public int deleteMenuById(Long menuId) {
        return baseMapper.deleteById(menuId);
    }

    /**
     * 校验菜单名称是否唯一
     *
     * @param menu 菜单信息
     * @return 结果
     */
    @Override
    public boolean checkMenuNameUnique(SysMenu menu) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getMenuName, menu.getMenuName())
            .eq(SysMenu::getParentId, menu.getParentId())
            .ne(ObjectUtil.isNotNull(menu.getMenuId()), SysMenu::getMenuId, menu.getMenuId()));
        return !exist;
    }

    /**
     * 获取路由名称
     *
     * @param menu 菜单信息
     * @return 路由名称
     */
    public String getRouteName(SysMenu menu) {
        String routerName = StringUtils.capitalize(menu.getPath());
        // 非外链并且是一级目录（类型为目录）
        if (isMenuFrame(menu)) {
            routerName = StringUtils.EMPTY;
        }
        return routerName;
    }

    /**
     * 获取路由地址
     *
     * @param menu 菜单信息
     * @return 路由地址
     */
    public String getRouterPath(SysMenu menu) {
        String routerPath = menu.getPath();
        // 内链打开外网方式
        if (menu.getParentId().intValue() != 0 && isInnerLink(menu)) {
            routerPath = innerLinkReplaceEach(routerPath);
        }
        // 非外链并且是一级目录（类型为目录）
        if (0 == menu.getParentId().intValue() && UserConstants.TYPE_DIR.equals(menu.getMenuType())
            && UserConstants.NO_FRAME.equals(menu.getIsFrame())) {
            routerPath = "/" + menu.getPath();
        }
        // 非外链并且是一级目录（类型为菜单）
        else if (isMenuFrame(menu)) {
            routerPath = "/";
        }
        return routerPath;
    }

    /**
     * 获取组件信息
     *
     * @param menu 菜单信息
     * @return 组件信息
     */
    public String getComponent(SysMenu menu) {
        String component = UserConstants.LAYOUT;
        if (StringUtils.isNotEmpty(menu.getComponent()) && !isMenuFrame(menu)) {
            component = menu.getComponent();
        } else if (StringUtils.isEmpty(menu.getComponent()) && menu.getParentId().intValue() != 0 && isInnerLink(menu)) {
            component = UserConstants.INNER_LINK;
        } else if (StringUtils.isEmpty(menu.getComponent()) && isParentView(menu)) {
            component = UserConstants.PARENT_VIEW;
        }
        return component;
    }

    /**
     * 是否为菜单内部跳转
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isMenuFrame(SysMenu menu) {
        return menu.getParentId().intValue() == 0 && UserConstants.TYPE_MENU.equals(menu.getMenuType())
            && menu.getIsFrame().equals(UserConstants.NO_FRAME);
    }

    /**
     * 是否为内链组件
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isInnerLink(SysMenu menu) {
        return menu.getIsFrame().equals(UserConstants.NO_FRAME) && StringUtils.ishttp(menu.getPath());
    }

    /**
     * 是否为parent_view组件
     *
     * @param menu 菜单信息
     * @return 结果
     */
    public boolean isParentView(SysMenu menu) {
        return menu.getParentId().intValue() != 0 && UserConstants.TYPE_DIR.equals(menu.getMenuType());
    }

    /**
     * 过期租户管理员菜单过滤：租户 EXPIRED 且当前用户为租户管理员时，
     * 仅保留版本升级菜单（7500 目录 + 7501 升级页面），引导管理员完成续费升级。
     */
    private List<SysMenu> applyExpiredTenantFilter(List<SysMenu> menus, Long userId) {
        if (CollUtil.isEmpty(menus) || userId == null) {
            return menus;
        }
        Long tenantId = LoginHelper.getTenantId();
        if (tenantId == null) {
            return menus;
        }
        if (!isExpiredTenantAdmin(tenantId, userId)) {
            return menus;
        }
        return StreamUtils.filter(menus, m -> m != null && m.getMenuId() != null
            && EXPIRED_ADMIN_MENU_IDS.contains(m.getMenuId()));
    }

    /**
     * 判断指定用户是否为已到期租户的管理员（sys_tenant.adminUserId 匹配）
     */
    private boolean isExpiredTenantAdmin(Long tenantId, Long userId) {
        try {
            SysTenant tenant = tenantMapper.selectById(tenantId);
            return tenant != null && "EXPIRED".equals(tenant.getStatus())
                && userId.equals(tenant.getAdminUserId());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 过期租户管理员权限过滤：租户 EXPIRED 且当前用户为租户管理员时，
     * 仅保留版本升级相关权限（micro:platform:upgrade*），前端据此只渲染升级按钮。
     *
     * @param perms 原始权限集合
     * @return 过滤后的权限集合
     */
    @Override
    public Set<String> filterPermissionsForExpiredTenant(Set<String> perms) {
        if (perms == null || perms.isEmpty()) {
            return perms;
        }
        Long userId = LoginHelper.getUserId();
        Long tenantId = LoginHelper.getTenantId();
        if (userId == null || tenantId == null) {
            return perms;
        }
        if (!isExpiredTenantAdmin(tenantId, userId)) {
            return perms;
        }
        Set<String> filtered = new HashSet<>();
        for (String perm : perms) {
            if (perm != null && perm.startsWith("micro:platform:upgrade")) {
                filtered.add(perm);
            }
        }
        return filtered;
    }

    /**
     * 根据父节点的ID获取所有子节点
     *
     * @param list     分类表
     * @param parentId 传入的父节点ID
     * @return String
     */
    public List<SysMenu> getChildPerms(List<SysMenu> list, int parentId) {
        List<SysMenu> returnList = new ArrayList<>();
        for (SysMenu t : list) {
            // 一、根据传入的某个父节点ID,遍历该父节点的所有子节点
            if (t.getParentId() == parentId) {
                recursionFn(list, t);
                returnList.add(t);
            }
        }
        return returnList;
    }

    /**
     * 递归列表
     *
     * @param list
     * @param t
     */
    private void recursionFn(List<SysMenu> list, SysMenu t) {
        // 得到子节点列表
        List<SysMenu> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysMenu tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysMenu> getChildList(List<SysMenu> list, SysMenu t) {
        return StreamUtils.filter(list, n -> n.getParentId().equals(t.getMenuId()));
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysMenu> list, SysMenu t) {
        return CollUtil.isNotEmpty(getChildList(list, t));
    }

    /**
     * 内链域名特殊字符替换
     */
    public String innerLinkReplaceEach(String path) {
        return StringUtils.replaceEach(path, new String[]{Constants.HTTP, Constants.HTTPS, Constants.WWW, "."},
            new String[]{"", "", "", "/"});
    }
}
