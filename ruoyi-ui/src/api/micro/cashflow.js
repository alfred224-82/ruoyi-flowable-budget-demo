import request from '@/utils/request'

// 收入流水列表（后端强制 flowType=INCOME）
export function listIncome(query) {
  return request({
    url: '/micro/cashflow/income/list',
    method: 'get',
    params: query
  })
}

// 手动补录收入
export function addIncome(data) {
  return request({
    url: '/micro/cashflow/income',
    method: 'post',
    data: data
  })
}

// 支出流水列表（后端强制 flowType=EXPENSE）
export function listExpense(query) {
  return request({
    url: '/micro/cashflow/expense/list',
    method: 'get',
    params: query
  })
}

// 手动补录支出（工资/房租/水电/采购...）
export function addExpense(data) {
  return request({
    url: '/micro/cashflow/expense',
    method: 'post',
    data: data
  })
}
