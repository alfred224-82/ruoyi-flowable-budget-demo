import request from '@/utils/request'

// 查询分成列表
export function listSharing(query) {
  return request({
    url: '/fee/sharing/list',
    method: 'get',
    params: query
  })
}

// 查询分成详情
export function getSharing(id) {
  return request({
    url: '/fee/sharing/' + id,
    method: 'get'
  })
}

// 申请分成（项目经理，状态=待审批）
export function addSharing(data) {
  return request({
    url: '/fee/sharing',
    method: 'post',
    data: data
  })
}

// 老板审批通过（设定比例与基数，计算分成金额）
export function approveSharing(id, ratio, baseAmount) {
  return request({
    url: '/fee/sharing/approve/' + id,
    method: 'put',
    params: { ratio, baseAmount }
  })
}

// 老板驳回（删除待审批记录）
export function rejectSharing(id) {
  return request({
    url: '/fee/sharing/reject/' + id,
    method: 'put'
  })
}

// 标记已支付
export function paySharing(id) {
  return request({
    url: '/fee/sharing/pay/' + id,
    method: 'put'
  })
}
