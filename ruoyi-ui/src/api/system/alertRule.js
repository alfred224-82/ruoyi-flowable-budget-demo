import request from '@/utils/request'

// ===== T18.15 告警规则配置 =====

export function listAlertRule(params) {
  return request({ url: '/system/alertRule/list', method: 'get', params })
}

export function getAlertRule(id) {
  return request({ url: '/system/alertRule/' + id, method: 'get' })
}

export function addAlertRule(data) {
  return request({ url: '/system/alertRule', method: 'post', data })
}

export function updateAlertRule(data) {
  return request({ url: '/system/alertRule', method: 'put', data })
}

export function deleteAlertRule(ids) {
  return request({ url: '/system/alertRule/' + ids, method: 'delete' })
}

export function toggleAlertRule(id, enabled) {
  return request({ url: '/system/alertRule/toggle', method: 'put', params: { id, enabled } })
}
