/**
 * Token 与登录态管理
 */
const TOKEN_KEY = 'Authorization'
const ROLE_KEY = 'app_roles'
const USER_KEY = 'app_user'
const ORG_KEY = 'app_org'
const VERSION_TYPE_KEY = 'app_version_type'

export function getToken() {
    return uni.getStorageSync(TOKEN_KEY)
}

export function setToken(token) {
    uni.setStorageSync(TOKEN_KEY, token)
}

export function removeToken() {
    uni.removeStorageSync(TOKEN_KEY)
}

/** 保存角色标识集合（如 ['boss'] / ['manager','employee']，多角色取并集，权限文档 6.6） */
export function setRoles(roles) {
    uni.setStorageSync(ROLE_KEY, roles || [])
}

export function getRoles() {
    return uni.getStorageSync(ROLE_KEY) || []
}

export function setUser(user) {
    uni.setStorageSync(USER_KEY, user || {})
}

export function getUser() {
    return uni.getStorageSync(USER_KEY) || {}
}

/** 保存当前组织（专业版多组织切换，FEAT_MULTI_ORG，T8.5） */
export function setOrg(org) {
    uni.setStorageSync(ORG_KEY, org || null)
}

export function getOrg() {
    return uni.getStorageSync(ORG_KEY) || null
}

/** 清空登录态并回登录页 */
export function clearAuth() {
    clearAuthTo('/pages/login/index')
}

/** 清空登录态并跳转到指定页（T29.4「切换租户」跳租户号输入页用） */
let authNavigating = false
export function clearAuthTo(url) {
    uni.removeStorageSync(TOKEN_KEY)
    uni.removeStorageSync(ROLE_KEY)
    uni.removeStorageSync(USER_KEY)
    uni.removeStorageSync(ORG_KEY)
    uni.removeStorageSync(VERSION_TYPE_KEY)

    const target = url || '/pages/login/index'
    // ★已在目标页时不再跳转：并发 401（如登录页同时发多个请求）会多次进入本函数，
    //   重复 reLaunch 到同一页会互相取消，报
    //   「reLaunch:fail Navigation cancelled from "/pages/login/index" to "/pages/login/index"」
    const pages = getCurrentPages() || []
    const current = pages.length ? '/' + (pages[pages.length - 1].route || '') : ''
    if (current === target) return
    // ★上一次 401 触发的跳转尚未完成时跳过，避免并发导航竞争
    if (authNavigating) return
    authNavigating = true
    uni.reLaunch({ url: target, complete: () => { authNavigating = false } })
}

/** 保存当前租户版本档位（micro_pro/micro_lite/basic/advanced/pro） */
export function setVersionType(versionType) {
    uni.setStorageSync(VERSION_TYPE_KEY, versionType || '')
}

/** 获取当前租户版本档位 */
export function getVersionType() {
    return uni.getStorageSync(VERSION_TYPE_KEY) || ''
}
