import request from '@/utils/request'

// 查询预留预算列表
export function listReservedBudget(query) {
  return request({
    url: '/fee/reservedBudget/list',
    method: 'get',
    params: query
  })
}

// 查询预留预算详情
export function getReservedBudget(id) {
  return request({
    url: '/fee/reservedBudget/' + id,
    method: 'get'
  })
}

// 新增预留预算
export function addReservedBudget(data) {
  return request({
    url: '/fee/reservedBudget',
    method: 'post',
    data: data
  })
}

// 修改预留预算
export function updateReservedBudget(data) {
  return request({
    url: '/fee/reservedBudget',
    method: 'put',
    data: data
  })
}

// 预留预算总览（本级）
export function getReservedBudgetOverview(budgetPeriod) {
  return request({
    url: '/fee/reservedBudget/overview',
    method: 'get',
    params: { budgetPeriod }
  })
}

// 删除预留预算
export function delReservedBudget(ids) {
  return request({
    url: '/fee/reservedBudget/' + ids,
    method: 'delete'
  })
}
