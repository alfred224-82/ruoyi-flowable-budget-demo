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

// 新增报销单（创建即提交）
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
