/**
 * 角色识别与 Tab 分流配置（《各角色菜单权限文档》5.1，T8.1）
 * 单包多角色：登录后按角色渲染不同的 Tab 标题与页面内容
 */
import { getRoles, getVersionType } from './auth'
import { getSelectedStore } from './store'

/** 业务角色常量（与后端 role_key 一致） */
export const ROLES = {
    BOSS: 'boss',
    FINANCE: 'finance',
    MANAGER: 'manager',
    EMPLOYEE: 'employee',
    PLATFORM_ADMIN: 'platform_admin',
    // 小微版角色（T15.15 单包多角色）
    SALESMAN: 'salesman',
    CUSTOMER: 'customer'
}

/** 小微版角色集合（用于快速判断是否走小微版 Tab 分流） */
const MICRO_ROLES = [ROLES.BOSS, ROLES.SALESMAN, ROLES.CUSTOMER]

/**
 * Tab 分流表（5.1）：
 * - 老板：大类看板 / 项目卡片 / 审批 / 我的
 * - 财务：通知预警（仅查看，不开放业务 Tab，T8.6）/ 我的
 * - 项目经理：我的项目 / 收入确认 / （无审批）/ 我的
 * - 报销人：首页待办 / 发起报销 / 我的报销 / 我的
 */
export const TAB_CONFIG = {
    [ROLES.BOSS]: {
        name: '老板',
        tabs: [
            { index: 0, text: '看板', view: 'dashboard', page: '/pages/dashboard/category' },
            { index: 1, text: '项目', view: 'projects', page: '/pages/project/list' },
            { index: 2, text: '审批', view: 'approval', page: '/pages/approval/list' },
            { index: 3, text: '我的', view: 'mine', page: '' }
        ]
    },
    [ROLES.FINANCE]: {
        name: '财务',
        tabs: [
            { index: 0, text: '通知', view: 'notice', page: '' },
            { index: 1, text: '', view: 'hidden', page: '' },
            { index: 2, text: '', view: 'hidden', page: '' },
            { index: 3, text: '我的', view: 'mine', page: '' }
        ]
    },
    [ROLES.MANAGER]: {
        name: '项目经理',
        tabs: [
            { index: 0, text: '我的项目', view: 'projects', page: '/pages/project/list' },
            { index: 1, text: '收入确认', view: 'income', page: '/pages/project/income' },
            { index: 2, text: '', view: 'hidden', page: '' },
            { index: 3, text: '我的', view: 'mine', page: '' }
        ]
    },
    [ROLES.EMPLOYEE]: {
        name: '报销人',
        tabs: [
            { index: 0, text: '首页', view: 'todo', page: '' },
            { index: 1, text: '发起报销', view: 'expense', page: '/pages/expense/create' },
            { index: 2, text: '我的报销', view: 'myExpense', page: '/pages/expense/mine' },
            { index: 3, text: '我的', view: 'mine', page: '' }
        ]
    }
}

/** 当前是否为小微版（roles 含 boss/salesman/customer 之一即走小微版 Tab 分流，T15.15） */
export function isMicroVersion() {
    const roles = getRoles()
    return MICRO_ROLES.some(r => roles.includes(r))
}

/** 获取主业务角色（多角色并集时按优先级：老板>项目经理>报销人>财务，权限文档 6.6）
 *  小微版优先级：boss > salesman > customer */
export function getPrimaryRole() {
    const roles = getRoles()
    if (isMicroVersion()) {
        const microPriority = [ROLES.BOSS, ROLES.SALESMAN, ROLES.CUSTOMER]
        for (const r of microPriority) {
            if (roles.includes(r)) return r
        }
    }
    const priority = [ROLES.BOSS, ROLES.MANAGER, ROLES.EMPLOYEE, ROLES.FINANCE]
    for (const r of priority) {
        if (roles.includes(r)) return r
    }
    return ''
}

/** 当前角色是否为管理员（平台管理员不使用小程序，权限文档 3.5/5.1 备注） */
export function isPlatformAdmin() {
    return getRoles().includes(ROLES.PLATFORM_ADMIN)
}

/** 获取当前角色的 Tab 配置 */
export function getTabConfig() {
    return TAB_CONFIG[getPrimaryRole()] || TAB_CONFIG[ROLES.EMPLOYEE]
}

/**
 * 小微版 Tab 配置表（V3.7 底部导航定稿，client_type=mini，单包多角色）
 * boss（4 Tab）：首页(经营驾驶舱) / 最新活动 / 我的待处理(待办与异常中心) / 我的推广码(微信二维码 租户+人员+角色)
 * salesman（3 Tab）：工作台(我参与的活动·未结束) / 我的推广码 / 我的
 * customer（4 Tab）：发现 / 我的推广码(邀请有礼) / 我参与的活动(未结束) / 我的
 * ★V3.7 未定义的底部 Tab 一律去掉（老板旧预约/报表/我的 Tab、员工旧活动/收入 Tab、顾客旧首页/预约 Tab）；
 *   对应页面保留，由页内入口跳转。lite/pro 底部导航统一，首页内部按档位条件渲染。
 */

/** 判断当前是否为小微版 lite 档（micro_lite） */
export function isMicroLite() {
    const vt = getVersionType() || (getSelectedTenant() && getSelectedTenant().versionType) || ''
    return isMicroVersion() && vt === 'micro_lite'
}

export const MICRO_TAB_CONFIG = {
    [ROLES.BOSS]: {
        name: '老板',
        tabs: [
            { index: 0, text: '首页', icon: '🏠', view: 'bossHome', page: '/pages/micro/bossHome/index' },
            { index: 1, text: '我的活动', icon: '🎉', view: 'myActivity', page: '/pages/micro/bossBook/list' },
            { index: 2, text: '我的待处理', icon: '📋', view: 'todo', page: '/pages/micro/bossTodo/index' },
            { index: 3, text: '我的推广码', icon: '📢', view: 'promo', page: '/pages/micro/bossPromo/index' }
        ]
    },
    [ROLES.SALESMAN]: {
        name: '业务员',
        tabs: [
            { index: 0, text: '工作台', icon: '🏠', view: 'schedule', page: '/pages/micro/staffHome/index' },
            { index: 1, text: '我的推广码', icon: '📢', view: 'promo', page: '/pages/micro/staff/promo' },
            { index: 2, text: '我的', icon: '👤', view: 'mine', page: '/pages/micro/staffMine/index' }
        ]
    },
    [ROLES.CUSTOMER]: {
        name: '顾客',
        tabs: [
            { index: 0, text: '发现', icon: '🏠', view: 'shop', page: '/pages/micro/customerHome/index' },
            { index: 1, text: '我的推广码', icon: '📢', view: 'promo', page: '/pages/micro/customer/promo' },
            { index: 2, text: '我的活动', icon: '🎉', view: 'myBook', page: '/pages/micro/customerBook/list' },
            { index: 3, text: '我的', icon: '👤', view: 'mine', page: '/pages/micro/customer/mine' }
        ]
    }
}

/** 获取小微版 Tab 配置（V3.7：lite/pro 底部导航统一，首页内部按档位条件渲染） */
export function getMicroTabConfig() {
    const role = getPrimaryRole()
    const conf = MICRO_TAB_CONFIG[role] || MICRO_TAB_CONFIG[ROLES.CUSTOMER]
    return { name: conf.name, tabs: conf.tabs }
}

/**
 * 登录后应用角色分流：刷新 TabBar 文案、隐藏不开放的业务 Tab
 * 注意：微信小程序 tabBar 最少保留 2 项，财务角色隐藏 Tab2/Tab3 后保留 通知/我的
 * 小微版三角色（boss/salesman/customer）各自独立 Tab 文案与页面入口
 */
export function applyRoleTabs() {
    if (isMicroVersion()) {
        const conf = getMicroTabConfig()
        conf.tabs.forEach(tab => {
            if (tab.view === 'hidden') {
                try {
                    uni.hideTabBarRedDot && uni.hideTabBarRedDot({ index: tab.index })
                } catch (e) { /* ignore */ }
            } else if (tab.text) {
                try {
                    uni.setTabBarItem({ index: tab.index, text: tab.text })
                } catch (e) { /* ignore */ }
            }
        })
        return
    }
    const conf = getTabConfig()
    conf.tabs.forEach(tab => {
        if (tab.view === 'hidden') {
            try {
                uni.hideTabBarRedDot && uni.hideTabBarRedDot({ index: tab.index })
            } catch (e) { /* ignore */ }
        } else if (tab.text) {
            uni.setTabBarItem({ index: tab.index, text: tab.text })
        }
    })
    // 财务角色隐藏两个业务 Tab（不支持动态删除，以空文案+内容区拦截兜底）
    if (getPrimaryRole() === ROLES.FINANCE) {
        uni.setTabBarItem({ index: 1, text: '预警' })
        uni.setTabBarItem({ index: 2, text: '通知' })
    }
}

/**
 * ========== 多租户「我的店铺」上下文（T15.36§6）==========
 * 同一手机号可归属多个租户；登录后须锁定当前 tenant 上下文，否则后端租户拦截器失效。
 * - 登录响应 needSelectTenant=true → setTenantList 缓存候选 → tenant/select.vue 展示；
 * - 「我的 → 切换店铺」→ 调 /micro/tenant/myTenants 刷新候选 → tenant/select.vue；
 * - selectTenant 锁定选定租户并持久化，作为会话租户上下文与首页分流依据。
 */
const TENANT_LIST_KEY = 'app_tenant_list'
const SELECTED_TENANT_KEY = 'app_selected_tenant'

/** 缓存可切换租户候选列表 */
export function setTenantList(list) {
    uni.setStorageSync(TENANT_LIST_KEY, list || [])
}

/** 获取可切换租户候选列表（T15.36§6.5 getTenantList） */
export function getTenantList() {
    return uni.getStorageSync(TENANT_LIST_KEY) || []
}

/** 锁定当前租户（T15.36§6.5 selectTenant）：持久化选定项作为会话租户上下文 */
export function selectTenant(tenant) {
    if (!tenant) {
        uni.removeStorageSync(SELECTED_TENANT_KEY)
        return
    }
    uni.setStorageSync(SELECTED_TENANT_KEY, {
        tenantId: tenant.tenantId,
        tenantName: tenant.tenantName,
        roleCode: tenant.roleCode,
        roleName: tenant.roleName,
        versionType: tenant.versionType,
        industryCode: tenant.industryCode,
        logoUrl: tenant.logoUrl,
        city: tenant.city
    })
}

/** 获取当前锁定租户（未锁定返回 null） */
export function getSelectedTenant() {
    return uni.getStorageSync(SELECTED_TENANT_KEY) || null
}

/** 清除多租户上下文（退出登录/重新引导时调用） */
export function clearTenantContext() {
    uni.removeStorageSync(TENANT_LIST_KEY)
    uni.removeStorageSync(SELECTED_TENANT_KEY)
}

/**
 * 获取当前租户（门店）名称：优先「我的店铺」锁定项，其次引导态选定门店；
 * T29.1：导航栏标题固定为系统名「阿杰小账本」，租户名改由页面内副标题（门店选择区顶部大字号）展示，
 * 本函数仅供页面内展示租户名使用（登录页/门店选择页大字号标题）。
 */
export function getTenantName() {
    const tenant = getSelectedTenant()
    if (tenant && tenant.tenantName) return tenant.tenantName
    const store = getSelectedStore()
    if (store && store.companyName) return store.companyName
    return ''
}

/** 获取当前租户ID：优先「我的店铺」锁定项，其次引导态选定门店（活动浏览等按租户查询的接口入参） */
export function getTenantId() {
    const tenant = getSelectedTenant()
    if (tenant && tenant.tenantId) return tenant.tenantId
    const store = getSelectedStore()
    if (store && store.tenantId) return store.tenantId
    return null
}

/**
 * 系统名（T29.1）：导航栏标题固定为「阿杰小账本」，租户名不再占用导航栏，改由页面内副标题分层展示。
 */
export const SYSTEM_NAME = '阿杰小账本'

/** 导航栏标题固定设为系统名「阿杰小账本」（T29.1）；租户名由页面内大字号副标题展示 */
export function applyNavBarTitle(retried) {
    // H5冷启动onShow/跳转拦截器complete触发时页面栈可能尚未就绪，直接调用会报 page not found
    if (getCurrentPages().length === 0) {
        if (!retried) setTimeout(() => applyNavBarTitle(true), 50)
        return
    }
    // 传fail回调：避免H5页面切换过渡期失败产生未处理的Promise拒绝（Uncaught in promise）
    uni.setNavigationBarTitle({
        title: SYSTEM_NAME,
        fail: () => { if (!retried) setTimeout(() => applyNavBarTitle(true), 50) }
    })
}
