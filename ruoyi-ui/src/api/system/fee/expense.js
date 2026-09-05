import request from '@/utils/request'

// 查询报销单列表
export function listExpenseBill(query) {
  return request({
    url: '/fee/expense/list',
    method: 'get',
    params: query
  })
}

// 查询报销单详细
export function getExpenseBill(id) {
  return request({
    url: '/fee/expense/' + id,
    method: 'get'
  })
}

// 新增报销单（创建即提交，V5.4支持多币种/补助）
export function addExpenseBill(data) {
  return request({
    url: '/fee/expense',
    method: 'post',
    data: data
  })
}

// 查询待审批报销单列表
export function listPendingApproval(query) {
  return request({
    url: '/fee/expense/pendingApproval',
    method: 'get',
    params: query
  })
}

// 部门经理审批通过
export function approveExpenseBill(id, opinion) {
  return request({
    url: '/fee/expense/approve/' + id,
    method: 'post',
    params: { opinion }
  })
}

// 部门经理驳回
export function rejectExpenseBill(id, reason, opinion) {
  return request({
    url: '/fee/expense/reject/' + id,
    method: 'post',
    params: { reason, opinion }
  })
}

// ========== T6.7 批量审核（FEAT_EVIDENCE_CHAIN） ==========

// 批量审批通过（逐条独立事务，返回成功数+失败明细）
export function batchApproveExpenseBill(billIds, opinion) {
  return request({
    url: '/fee/expense/batchApprove',
    method: 'post',
    params: { opinion },
    data: billIds
  })
}

// 批量驳回（逐条独立事务，返回成功数+失败明细）
export function batchRejectExpenseBill(billIds, reason, opinion) {
  return request({
    url: '/fee/expense/batchReject',
    method: 'post',
    params: { reason, opinion },
    data: billIds
  })
}

// ========== V5.4 新增接口 ==========

// 试算补助金额
export function calculateSubsidy(subsidyType, days) {
  return request({
    url: '/fee/expense/subsidy/calculate',
    method: 'get',
    params: { subsidyType, days }
  })
}

// 获取盈利提示
export function getProfitTip(budgetSubjectCode, amount) {
  return request({
    url: '/fee/expense/profitTip',
    method: 'get',
    params: { budgetSubjectCode, amount }
  })
}

// 获取汇率
export function getExchangeRate(currencyCode, period) {
  return request({
    url: '/fee/expense/exchangeRate',
    method: 'get',
    params: { currencyCode, period }
  })
}

// 获取可用币种列表
export function listCurrencies() {
  return request({
    url: '/fee/exchangeRate/currencies',
    method: 'get'
  })
}

// ========== 导入功能 ==========

// 导入报销单数据
export function importExpenseBill(data) {
  return request({
    url: '/fee/expense/importData',
    method: 'post',
    headers: { 'Content-Type': 'multipart/form-data' },
    data: data
  })
}

// 下载导入模板
export function importTemplate() {
  return request({
    url: '/fee/expense/importTemplate',
    method: 'post',
    responseType: 'blob'
  })
}
