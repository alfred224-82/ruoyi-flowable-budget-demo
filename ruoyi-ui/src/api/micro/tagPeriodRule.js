import request from '@/utils/request'

// ===== T18.9 标签管理 =====

export function listTags(query) {
  return request({ url: '/micro/tagPeriodRule/tag/list', method: 'get', params: query })
}
export function listAllTags(query) {
  return request({ url: '/micro/tagPeriodRule/tag/all', method: 'get', params: query })
}
export function getTag(id) {
  return request({ url: '/micro/tagPeriodRule/tag/' + id, method: 'get' })
}
export function addTag(data) {
  return request({ url: '/micro/tagPeriodRule/tag', method: 'post', data })
}
export function updateTag(data) {
  return request({ url: '/micro/tagPeriodRule/tag', method: 'put', data })
}
export function removeTag(id) {
  return request({ url: '/micro/tagPeriodRule/tag/' + id, method: 'delete' })
}

// ===== T18.9 周期规则管理 =====

export function listPeriodRules(query) {
  return request({ url: '/micro/tagPeriodRule/rule/list', method: 'get', params: query })
}
export function getPeriodRule(id) {
  return request({ url: '/micro/tagPeriodRule/rule/' + id, method: 'get' })
}
export function addPeriodRule(data) {
  return request({ url: '/micro/tagPeriodRule/rule', method: 'post', data })
}
export function updatePeriodRule(data) {
  return request({ url: '/micro/tagPeriodRule/rule', method: 'put', data })
}
export function removePeriodRule(id) {
  return request({ url: '/micro/tagPeriodRule/rule/' + id, method: 'delete' })
}
