import { listRoleDisplayNames } from '@/api/micro/tenantRole'

// 角色显示名工具（T15.9 角色映射的行业术语显示）
// ★「助教/导游/教练」等行业名来自 tenant_role_mapping.display_name（platform_admin 在「角色映射」维护），
//   页面文案不硬编码行业术语：优先按本租户角色映射显示，租户未配置映射时回退通用名（员工/顾客等）。

// 模块级 Promise 缓存：同一次会话内多页面共用一次请求；失败时重置允许重试
let cachePromise = null

/**
 * 加载本租户角色显示名映射（role_code → display_name）
 * @returns {Promise<Object>} 如 { boss: '老板', salesman: '助教', customer: '顾客' }；请求失败返回空对象
 */
export function loadRoleDisplayNames() {
  if (!cachePromise) {
    cachePromise = listRoleDisplayNames()
      .then(res => res.data || {})
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
