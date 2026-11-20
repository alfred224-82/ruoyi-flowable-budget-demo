import request from '@/utils/request'

// 固定支出管理（T15.37）

// 固定支出列表（分页）
export function listCost(query) {
  return request({
    url: '/micro/cost/list',
    method: 'get',
    params: query
  })
}

// 固定支出列表（不分页，小程序端用）
export function listAllCost(query) {
  return request({
    url: '/micro/cost/all',
    method: 'get',
    params: query
  })
}

// 固定支出详情
export function getCost(id) {
  return request({
    url: '/micro/cost/' + id,
    method: 'get'
  })
}

// 新增固定支出
export function addCost(data) {
  return request({
    url: '/micro/cost',
    method: 'post',
    data: data
  })
}

// 修改固定支出
export function updateCost(data) {
  return request({
    url: '/micro/cost',
    method: 'put',
    data: data
  })
}

// 删除固定支出
export function delCost(ids) {
  return request({
    url: '/micro/cost/' + ids,
    method: 'delete'
  })
}

// 本月固定支出合计
export function getMonthSum() {
  return request({
    url: '/micro/cost/monthSum',
    method: 'get'
  })
}

// 获取所有启用的固定支出标签（boss 表单用）
export function listAllTags() {
  return request({
    url: '/micro/tagPeriodRule/tag/all',
    method: 'get',
    params: { status: 'ENABLED' }
  })
}

// 根据标签ID获取启用的周期规则（boss 表单自动带出）
export function listRulesByTag(tagId) {
  return request({
    url: '/micro/tagPeriodRule/rule/byTag/' + tagId,
    method: 'get'
  })
}
