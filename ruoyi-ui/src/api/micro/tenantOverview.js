import request from '@/utils/request'

// ===== T18.6 租户管理员企业概览首页 =====

export function getTenantOverview() {
  return request({ url: '/micro/tenantOverview', method: 'get' })
}
