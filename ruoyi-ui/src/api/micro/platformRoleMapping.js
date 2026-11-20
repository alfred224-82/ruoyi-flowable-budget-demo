import request from '@/utils/request'

// platform_admin 角色映射管理（T15.33，租户管理员三件套之二）
// ★与 /micro/tenantRole 共用底层 Service，仅 URL 路径不同（platform_admin 菜单独立入口）

// 本租户角色映射列表
export function listPlatformRoleMapping(query) {
  return request({
    url: '/micro/platform/roleMapping/list',
    method: 'get',
    params: query
  })
}

// 标准角色集挑选列表（合并本租户映射状态）
export function listPlatformStandardRoles() {
  return request({
    url: '/micro/platform/roleMapping/standardRoles',
    method: 'get'
  })
}

// 改名（display_name）
export function renamePlatformRole(data) {
  return request({
    url: '/micro/platform/roleMapping/rename',
    method: 'put',
    data: data
  })
}

// 启用/禁用（status：0启用 1禁用）
export function togglePlatformRole(id, status) {
  return request({
    url: '/micro/platform/roleMapping/toggle/' + id + '/' + status,
    method: 'put'
  })
}
