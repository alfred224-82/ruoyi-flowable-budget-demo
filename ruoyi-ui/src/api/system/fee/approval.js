import request from '@/utils/request'

// 查询超预算审批列表
export function listApproval(query) {
  return request({
    url: '/fee/approval/list',
    method: 'get',
    params: query
  })
}

// 查询审批单详情
export function getApproval(id) {
  return request({
    url: '/fee/approval/' + id,
    method: 'get'
  })
}

// 处理审批
export function processApproval(data) {
  return request({
    url: '/fee/approval/process',
    method: 'post',
    data: data
  })
}

// 查询调剂流水
export function queryAdjustmentLog(approvalId) {
  return request({
    url: '/fee/approval/adjustmentLog/' + approvalId,
    method: 'get'
  })
}
