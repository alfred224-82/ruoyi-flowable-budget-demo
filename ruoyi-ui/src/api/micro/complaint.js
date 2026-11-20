import request from '@/utils/request'

// Boss PC 投诉管理（T25.4，micro:complaint:view / micro:complaint:handle 权限）

// 投诉列表（Boss 查看本租户投诉）
// params: { channel, status }
export function listComplaints(params) {
  return request({
    url: '/micro/complaint/list',
    method: 'get',
    params: params
  })
}

// 处理投诉（回复）
// data: { handlerReply, status }
export function replyComplaint(id, data) {
  return request({
    url: '/micro/complaint/' + id + '/reply',
    method: 'put',
    data: data
  })
}
