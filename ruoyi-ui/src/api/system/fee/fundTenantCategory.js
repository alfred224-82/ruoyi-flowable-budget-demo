import request from '@/utils/request'

// 查询当前租户大类启用配置（含未启用，platform_admin 勾选管理用）
export function listTenantCategory() {
  return request({
    url: '/fee/tenantCategory/list',
    method: 'get'
  })
}

// 查询当前租户已启用的大类（仅启用状态，业务下拉用）
export function listEnabledTenantCategory() {
  return request({
    url: '/fee/tenantCategory/enabledList',
    method: 'get'
  })
}

// 批量保存租户大类启用配置（整体替换）
export function saveTenantCategories(categoryCodes) {
  return request({
    url: '/fee/tenantCategory/save',
    method: 'post',
    data: categoryCodes
  })
}
