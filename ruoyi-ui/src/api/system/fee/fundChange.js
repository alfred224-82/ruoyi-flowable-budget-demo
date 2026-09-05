import request from '@/utils/request'

// 查询合同变更列表
export function listChange(query) {
  return request({
    url: '/fee/change/list',
    method: 'get',
    params: query
  })
}

// 查询变更详情
export function getChange(id) {
  return request({
    url: '/fee/change/' + id,
    method: 'get'
  })
}

// 发起合同变更
export function addChange(data) {
  return request({
    url: '/fee/change',
    method: 'post',
    data: data
  })
}

// 老板审批通过（合同额/预算增量叠加）
export function approveChange(id) {
  return request({
    url: '/fee/change/approve/' + id,
    method: 'put'
  })
}

// 老板驳回
export function rejectChange(id, reason) {
  return request({
    url: '/fee/change/reject/' + id,
    method: 'put',
    params: { reason }
  })
}

// 财务收款确认（闭环）
export function confirmChange(id) {
  return request({
    url: '/fee/change/confirm/' + id,
    method: 'put'
  })
}
