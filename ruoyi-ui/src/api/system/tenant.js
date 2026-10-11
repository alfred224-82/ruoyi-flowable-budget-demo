import request from '@/utils/request'

// 查询租户列表
export function listTenant(query) {
  return request({
    url: '/system/tenant/list',
    method: 'get',
    params: query
  })
}

// 查询租户详细
export function getTenant(id) {
  return request({
    url: '/system/tenant/' + id,
    method: 'get'
  })
}

// 新增租户
export function addTenant(data) {
  return request({
    url: '/system/tenant',
    method: 'post',
    data: data
  })
}

// 修改租户
export function updateTenant(data) {
  return request({
    url: '/system/tenant',
    method: 'put',
    data: data
  })
}

// 启用/停用租户
export function changeTenantStatus(id, status) {
  return request({
    url: '/system/tenant/status/' + id,
    method: 'put',
    params: { status }
  })
}

// 分配版本
export function assignTenantVersion(id, edition) {
  return request({
    url: '/system/tenant/assignVersion/' + id,
    method: 'post',
    params: { edition }
  })
}

// 分配版本档位（T15.26/T15.29 零迁移升降级）
// version_type ∈ micro_lite/micro_pro/basic/advanced/pro；后端 assignVersionType 校验档位必须属于本租户产品线，
// 跨线升降级抛 ServiceException；仅 UPDATE version_type + 发布缓存刷新，角色 code / 用户 / 业务数据零迁移
export function assignTenantVersionType(id, versionType) {
  return request({
    url: '/system/tenant/assignVersionType/' + id,
    method: 'post',
    params: { versionType }
  })
}

// 版本变更日志
export function listVersionLog(id) {
  return request({
    url: '/system/tenant/versionLog/' + id,
    method: 'get'
  })
}

// 获取功能配置
export function getTenantConfig(id, configKey) {
  return request({
    url: '/system/tenant/config/' + id,
    method: 'get',
    params: { configKey }
  })
}

// 更新功能配置
export function saveTenantConfig(id, data) {
  return request({
    url: '/system/tenant/config/' + id,
    method: 'put',
    data: data
  })
}

// 用量统计
export function getTenantUsage(id) {
  return request({
    url: '/system/tenant/usage/' + id,
    method: 'get'
  })
}

// 导入租户 License（T3.11）
export function importTenantLicense(id, data) {
  return request({
    url: '/system/tenant/' + id + '/importLicense',
    method: 'post',
    data: data
  })
}

// 查看租户 License 信息（T3.14）
export function getTenantLicense(id) {
  return request({
    url: '/system/tenant/' + id + '/license',
    method: 'get'
  })
}

// 租户选择器（T3.17）
export function getTenantSelector(name) {
  return request({
    url: '/system/tenant/tenantSelector',
    method: 'get',
    params: { name }
  })
}
