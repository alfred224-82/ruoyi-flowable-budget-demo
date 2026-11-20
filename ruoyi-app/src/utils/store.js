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

/**
 * ========== T29.2/T29.4 Boss 手动输租户号登录上下文 ==========
 * tenantNo：Boss 首次登录手动输入的平台租户号，持久化后「后续免输」直接进登录页；
 * 「切换租户」清除 tenantNo 后回到租户号输入页。
 * storeCandidates：按 tenantNo 查得的门店候选列表（供门店列表页点选，不自动跳转）。
 */
const TENANT_NO_KEY = 'tenantNo'
const TENANT_STORE_CANDIDATES_KEY = 'tenant_store_candidates'

/** 保存租户号（Boss 手动输入，后续免输） */
export function setTenantNo(tenantNo) {
    if (!tenantNo) {
        uni.removeStorageSync(TENANT_NO_KEY)
        return
    }
    uni.setStorageSync(TENANT_NO_KEY, tenantNo)
}

/** 获取已记住的租户号（无则返回空串） */
export function getTenantNo() {
    return uni.getStorageSync(TENANT_NO_KEY) || ''
}

/** 清除租户号（切换租户时调用） */
export function clearTenantNo() {
    uni.removeStorageSync(TENANT_NO_KEY)
}

/** 缓存按租户号查得的门店候选列表（供门店列表页点选） */
export function setTenantStoreCandidates(list) {
    uni.setStorageSync(TENANT_STORE_CANDIDATES_KEY, list || [])
}

/** 获取门店候选列表 */
export function getTenantStoreCandidates() {
    return uni.getStorageSync(TENANT_STORE_CANDIDATES_KEY) || []
}

/** 清除门店候选列表 */
export function clearTenantStoreCandidates() {
    uni.removeStorageSync(TENANT_STORE_CANDIDATES_KEY)
}
