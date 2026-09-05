import request from '@/utils/request'

// 查询付款单列表
export function listPayment(query) {
  return request({
    url: '/fee/payment/list',
    method: 'get',
    params: query
  })
}

// 查询付款单详情
export function getPayment(id) {
  return request({
    url: '/fee/payment/' + id,
    method: 'get'
  })
}

// 财务登记付款单
export function addPayment(data) {
  return request({
    url: '/fee/payment',
    method: 'post',
    data: data
  })
}

// 老板审批通过（扣项目预算 + 记总支出）
export function approvePayment(id) {
  return request({
    url: '/fee/payment/approve/' + id,
    method: 'put'
  })
}

// 老板驳回
export function rejectPayment(id, reason) {
  return request({
    url: '/fee/payment/reject/' + id,
    method: 'put',
    params: { reason }
  })
}
