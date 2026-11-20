import request from '@/utils/request'

// 业务角色管理（T15.9）：平台管理员从标准角色集勾选本租户角色 → 改名 → 启停 → 排序
// ★平台管理员不能新建角色 code、不能改角色权限（菜单/区块），改映射后无需重启即生效

// 本租户已挑选角色映射列表
export function listTenantRole(query) {
  return request({
    url: '/micro/tenantRole/list',
    method: 'get',
    params: query
  })
}

// ★本租户角色显示名映射（role_code → display_name，登录可读不校验权限）：供页面按角色映射显示行业术语
export function listRoleDisplayNames() {
  return request({
    url: '/micro/tenantRole/displayNames',
    method: 'get'
  })
}

// 标准角色集挑选列表（合并本租户映射状态，未挑选者 id=null）
export function listStandardRoles() {
  return request({
    url: '/micro/tenantRole/standardRoles',
    method: 'get'
  })
}

// ★T15.25 可选角色列表（角色可见性硬隔离）：后端按本租户 tenant_type 过滤 sys_role.tenant_type_scope
// 小微租户只返回 boss/salesman/customer，企业租户只返回 boss/project_manager/expense_staff/finance
export function listOptionalRoles() {
  return request({
    url: '/micro/tenantRole/optional-list',
    method: 'get'
  })
}

// ① 挑选标准角色（批量写入本租户映射，幂等跳过已存在）
export function selectRoles(roleCodes) {
  return request({
    url: '/micro/tenantRole/select',
    method: 'post',
    data: roleCodes
  })
}

// ② 改名（display_name，如 salesman→「助教」）
export function renameRole(data) {
  return request({
    url: '/micro/tenantRole/rename',
    method: 'put',
    data: data
  })
}

// ③ 启用/禁用（status：0启用 1禁用）
export function toggleRole(id, status) {
  return request({
    url: '/micro/tenantRole/toggle/' + id + '/' + status,
    method: 'put'
  })
}

// ⑤ 调整排序（sort_order）
export function sortRole(data) {
  return request({
    url: '/micro/tenantRole/sort',
    method: 'put',
    data: data
  })
}
