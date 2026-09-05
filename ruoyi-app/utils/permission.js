/**
 * 页面级权限（《各角色菜单权限文档》5.2，T8.1 越权页面不可达）
 * 每个业务页面在 onLoad 调用 guardPage()，当前角色不在白名单则返回上一页
 */
import { getRoles } from './auth'
import { ROLES } from './roles'

const PAGE_ROLES = {
    '/pages/dashboard/category': [ROLES.BOSS],
    '/pages/project/list': [ROLES.BOSS, ROLES.MANAGER],
    '/pages/project/detail': [ROLES.BOSS, ROLES.MANAGER],
    '/pages/project/income': [ROLES.MANAGER],
    '/pages/project/incomeCreate': [ROLES.MANAGER],
    '/pages/approval/list': [ROLES.BOSS],
    '/pages/approval/detail': [ROLES.BOSS],
    // 报销人（含项目经理兼任，权限文档 6.6 多角色并集）
    '/pages/expense/create': [ROLES.EMPLOYEE, ROLES.MANAGER],
    '/pages/expense/mine': [ROLES.EMPLOYEE, ROLES.MANAGER],
    '/pages/expense/supplement': [ROLES.EMPLOYEE, ROLES.MANAGER]
}

/** 当前角色是否可访问指定页面路径 */
export function canAccess(path) {
    const allow = PAGE_ROLES[path]
    // 未登记的页面默认放行（tab 页与登录页）
    if (!allow) return true
    const roles = getRoles()
    return allow.some(r => roles.includes(r))
}

/**
 * 页面守卫：越权访问时提示并返回
 * @param pagePath 当前页面路径（如 '/pages/approval/list'）
 * @returns {boolean} true=可继续加载
 */
export function guardPage(pagePath) {
    if (canAccess(pagePath)) {
        return true
    }
    uni.showToast({ title: '无权限访问该页面', icon: 'none' })
    setTimeout(() => {
        const pages = getCurrentPages()
        if (pages.length > 1) {
            uni.navigateBack()
        } else {
            uni.switchTab({ url: '/pages/tab-home/index' })
        }
    }, 800)
    return false
}
