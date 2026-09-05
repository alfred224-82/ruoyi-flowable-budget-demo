/**
 * 角色识别与 Tab 分流配置（《各角色菜单权限文档》5.1，T8.1）
 * 单包多角色：登录后按角色渲染不同的 Tab 标题与页面内容
 */
import { getRoles } from './auth'

/** 业务角色常量（与后端 role_key 一致） */
export const ROLES = {
    BOSS: 'boss',
    FINANCE: 'finance',
    MANAGER: 'manager',
    EMPLOYEE: 'employee',
    PLATFORM_ADMIN: 'platform_admin'
}

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

/** 获取主业务角色（多角色并集时按优先级：老板>项目经理>报销人>财务，权限文档 6.6） */
export function getPrimaryRole() {
    const roles = getRoles()
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
 * 登录后应用角色分流：刷新 TabBar 文案、隐藏不开放的业务 Tab
 * 注意：微信小程序 tabBar 最少保留 2 项，财务角色隐藏 Tab2/Tab3 后保留 通知/我的
 */
export function applyRoleTabs() {
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
