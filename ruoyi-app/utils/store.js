/**
 * 引导态选定门店持久化（T8.10 门店选择 / T13.1 双阶段租户识别）
 *
 * 引导态仅缓存用户【显式选定】的门店公开展示信息，用于登录页门店横幅与"切换门店"；
 * 【严禁】在此缓存或伪造会话/租户上下文——真正的租户以登录后 JWT 携带的 tenant_id 为准，
 * 后端 TenantLineInnerInterceptor 依据登录态 LoginHelper.getTenantId() 做行级隔离。
 */
const STORE_KEY = 'app_selected_store'

/**
 * 保存选定门店（仅取 PortalStoreVo 的公开展示字段子集）
 * @param {Object} store 门店对象（来自 /portal/stores 或 /portal/tenant/resolve）
 */
export function setSelectedStore(store) {
    if (!store) {
        uni.removeStorageSync(STORE_KEY)
        return
    }
    uni.setStorageSync(STORE_KEY, {
        tenantId: store.tenantId,
        tenantCode: store.tenantCode,
        companyName: store.companyName,
        city: store.city,
        address: store.address,
        logoUrl: store.logoUrl,
        bizCode: store.bizCode,
        accessChannel: store.accessChannel
    })
}

/** 获取选定门店（未选择返回 null） */
export function getSelectedStore() {
    return uni.getStorageSync(STORE_KEY) || null
}

/** 清除选定门店（切换门店/需要重新引导时调用） */
export function clearSelectedStore() {
    uni.removeStorageSync(STORE_KEY)
}
