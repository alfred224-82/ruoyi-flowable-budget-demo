import request from '@/utils/request'

// 查询收款单列表
export function listReceipt(query) {
  return request({
    url: '/fee/receipt/list',
    method: 'get',
    params: query
  })
}

// 查询收款单详情
export function getReceipt(id) {
  return request({
    url: '/fee/receipt/' + id,
    method: 'get'
  })
}

// 财务创建收款单
export function addReceipt(data) {
  return request({
    url: '/fee/receipt',
    method: 'post',
    data: data
  })
}

// 审核通过（更新项目收入 + 联动生成已核验收入确认）
export function approveReceipt(id) {
  return request({
    url: '/fee/receipt/approve/' + id,
    method: 'put'
  })
}

// 审核驳回
export function rejectReceipt(id, reason) {
  return request({
    url: '/fee/receipt/reject/' + id,
    method: 'put',
    params: { reason }
  })
}
