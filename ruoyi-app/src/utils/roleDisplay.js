/**
 * 角色显示名工具（行业术语动态化，对齐 PC 端 ruoyi-ui/src/utils/roleDisplay.js）
 * ★「助教/导游/教练」等行业名来自 tenant_role_mapping.display_name（platform_admin 在「角色映射」维护），
 *   页面文案不硬编码行业术语：优先按本租户角色映射显示，租户未配置映射时回退通用名（员工/顾客）。
 */
import { get } from './request'
import config from '../config'

// 模块级 Promise 缓存：同一次会话内多页面共用一次请求；失败时重置允许重试
let cachePromise = null

/**
 * 加载本租户角色显示名映射（role_code → display_name）
 * @returns {Promise<Object>} 如 { boss: '老板', salesman: '导游', customer: '游客' }；请求失败返回空对象
 */
export function loadRoleDisplayNames() {
    if (!cachePromise) {
        cachePromise = get(config.api.microRoleDisplayNames)
            .then(res => (res && res.data) || {})
            .catch(() => {
                cachePromise = null
                return {}
            })
    }
    return cachePromise
}

/**
 * 获取指定角色的显示名：优先本租户角色映射，无映射回退通用名
 * @param {string} roleCode 标准角色编码（如 salesman/customer）
 * @param {string} defaultName 通用回退名（如 员工/顾客）
 * @returns {Promise<string>} 显示名
 */
export function getRoleDisplayName(roleCode, defaultName) {
    return loadRoleDisplayNames().then(map => map[roleCode] || defaultName)
}

/**
 * ★BUG-03 获取空间资源行业显示名（后端 TenantRoleMappingServiceImpl.SPACE_DISPLAY_KEY='_space_'）
 * <p>旅游→车辆；桌游/剧本杀/KTV/电竞/轰趴→房间；足浴/推拿/SPA/美容美发→场地；未知→默认房间。
 * @param {string} defaultName 回退名（默认 '房间'）
 * @returns {Promise<string>} 空间显示名
 */
export function getSpaceDisplayName(defaultName) {
    return loadRoleDisplayNames().then(map => map._space_ || defaultName || '房间')
}
