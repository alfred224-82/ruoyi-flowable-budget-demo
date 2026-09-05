import request from '@/utils/request'

// 借款列表
export function listLoan(query) {
  return request({ url: '/fee/loan/list', method: 'get', params: query })
}

// 新增借款
export function addLoan(data) {
  return request({ url: '/fee/loan', method: 'post', data })
}

// 结清借款
export function settleLoan(id) {
  return request({ url: '/fee/loan/settle/' + id, method: 'post' })
}

// 手动触发利息计提
export function executeInterest(period) {
  return request({ url: '/fee/loan/interest/execute', method: 'post', params: { period } })
}

// 逾期检测
export function checkOverdue() {
  return request({ url: '/fee/loan/checkOverdue', method: 'post' })
}
