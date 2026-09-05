import request from '@/utils/request'

// 计提记录列表
export function listAccrual(query) {
  return request({ url: '/fee/accrual/list', method: 'get', params: query })
}

// 确认凭证
export function confirmVoucher(id) {
  return request({ url: '/fee/accrual/confirm/' + id, method: 'post' })
}

// 作废计提
export function voidVoucher(id, reason) {
  return request({ url: '/fee/accrual/void/' + id, method: 'post', params: { reason } })
}

// 手动执行计提
export function executeAccrual(period) {
  return request({ url: '/fee/accrual/execute', method: 'post', params: { period } })
}
