import request from '@/utils/request'

// 查询超预算审批列表
export function listOverBudget(query) {
  return request({
    url: '/fee/approval/list',
    method: 'get',
    params: query
  })
}

// 查询超预算审批详情
export function getOverBudget(id) {
  return request({
    url: '/fee/approval/' + id,
    method: 'get'
  })
}

// 处理超预算审批
export function processOverBudget(data) {
  return request({
    url: '/fee/approval/process',
    method: 'post',
    data: data
  })
}

// 查询调剂流水
export function getAdjustmentLog(approvalId) {
  return request({
    url: '/fee/approval/adjustmentLog/' + approvalId,
    method: 'get'
  })
}

// 待审批列表
export function pendingOverBudgetList(query) {
  return request({
    url: '/fee/approval/list',
    method: 'get',
    params: { status: 'PENDING', ...query }
  })
}
