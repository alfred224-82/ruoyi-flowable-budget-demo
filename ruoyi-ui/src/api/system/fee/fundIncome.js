import request from '@/utils/request'

// 查询收入确认列表（三态：待核验/已核验/已驳回）
export function listIncome(query) {
  return request({
    url: '/fee/income/list',
    method: 'get',
    params: query
  })
}

// 查询收入确认详情
export function getIncome(id) {
  return request({
    url: '/fee/income/' + id,
    method: 'get'
  })
}

// 项目经理创建收入确认（待核验）
export function addIncome(data) {
  return request({
    url: '/fee/income',
    method: 'post',
    data: data
  })
}

// 财务核验通过
export function verifyIncome(id) {
  return request({
    url: '/fee/income/verify/' + id,
    method: 'put'
  })
}

// 财务核验驳回
export function rejectIncome(id, reason) {
  return request({
    url: '/fee/income/reject/' + id,
    method: 'put',
    params: { reason }
  })
}
