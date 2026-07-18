import request from '@/utils/request'

// 查询操作日志列表
export function listOperationLog(query) {
  return request({
    url: '/fee/operationLog/list',
    method: 'get',
    params: query
  })
}

// 查询单据操作时间线
export function getOperationTimeline(billNo) {
  return request({
    url: '/fee/operationLog/timeline/' + billNo,
    method: 'get'
  })
}
