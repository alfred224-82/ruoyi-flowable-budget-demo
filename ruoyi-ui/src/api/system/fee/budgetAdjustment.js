import request from '@/utils/request'

// 提交预算调整申请
export function applyBudgetAdjustment(data) {
  return request({
    url: '/fee/budgetAdjustment/apply',
    method: 'post',
    data: data
  })
}

// 查询申请列表
export function listBudgetAdjustment(query) {
  return request({
    url: '/fee/budgetAdjustment/list',
    method: 'get',
    params: query
  })
}

// 查询申请详情
export function getBudgetAdjustment(id) {
  return request({
    url: '/fee/budgetAdjustment/' + id,
    method: 'get'
  })
}

// 同意分摊（分公司/总公司审批时需传分摊金额）
export function approveBudgetAdjustment(id, opinion, allocateAmount) {
  const params = { opinion }
  if (allocateAmount !== null && allocateAmount !== undefined) {
    params.allocateAmount = allocateAmount
  }
  return request({
    url: '/fee/budgetAdjustment/approve/' + id,
    method: 'post',
    params
  })
}

// 驳回申请
export function rejectBudgetAdjustment(id, reason) {
  return request({
    url: '/fee/budgetAdjustment/reject/' + id,
    method: 'post',
    params: { reason }
  })
}

// 向上级转审
export function escalateBudgetAdjustment(id, reason) {
  return request({
    url: '/fee/budgetAdjustment/escalate/' + id,
    method: 'post',
    params: { reason }
  })
}

// 待我审批列表
export function pendingBudgetAdjustmentList(query) {
  return request({
    url: '/fee/budgetAdjustment/pendingList',
    method: 'get',
    params: query
  })
}

// 查询指定科目的预算余额
export function getSubjectBudget(subjectCode) {
  return request({
    url: '/fee/budgetAdjustment/subjectBudget/' + subjectCode,
    method: 'get'
  })
}

// 撤回申请
export function cancelBudgetAdjustment(id) {
  return request({
    url: '/fee/budgetAdjustment/cancel/' + id,
    method: 'post'
  })
}

// 删除申请单（未审批通过的可删除）
export function delBudgetAdjustment(id) {
  return request({
    url: '/fee/budgetAdjustment/' + id,
    method: 'delete'
  })
}

// 获取当前用户资金池信息
export function getMyFundPool() {
  return request({
    url: '/fee/budgetAdjustment/myFundPool',
    method: 'get'
  })
}
