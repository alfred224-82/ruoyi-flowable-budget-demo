import request from '@/utils/request'

// 贷款列表
export function listLoan(query) {
  return request({
    url: '/fee/loanInterest/list',
    method: 'get',
    params: query
  })
}

// 新增/修改贷款
export function saveLoan(data) {
  return request({
    url: '/fee/loanInterest',
    method: 'post',
    data: data
  })
}

// 结清贷款（停止计息）
export function settleLoan(id) {
  return request({
    url: '/fee/loanInterest/settle/' + id,
    method: 'put'
  })
}

// 利息计提记录列表
export function listLoanInterest(query) {
  return request({
    url: '/fee/loanInterest/interest/list',
    method: 'get',
    params: query
  })
}

// 手动触发月度利息计提（幂等）
export function accrueInterests(accrualMonth) {
  return request({
    url: '/fee/loanInterest/accrue',
    method: 'put',
    params: { accrualMonth }
  })
}
