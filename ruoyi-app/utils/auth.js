/**
 * Token 与登录态管理
 */
const TOKEN_KEY = 'Authorization'
const ROLE_KEY = 'app_roles'
const USER_KEY = 'app_user'
const ORG_KEY = 'app_org'

export function getToken() {
    return uni.getStorageSync(TOKEN_KEY)
}

export function setToken(token) {
    uni.setStorageSync(TOKEN_KEY, token)
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
    uni.removeStorageSync(TOKEN_KEY)
    uni.removeStorageSync(ROLE_KEY)
    uni.removeStorageSync(USER_KEY)
    uni.removeStorageSync(ORG_KEY)
    uni.reLaunch({ url: '/pages/login/index' })
}
