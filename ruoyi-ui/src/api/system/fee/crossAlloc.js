import request from '@/utils/request'

// 手动触发跨部门分摊
export function executeAllocation(params) {
  return request({ url: '/fee/crossAlloc/execute', method: 'post', params })
}

// 分摊记录查询
export function listAllocLogs(query) {
  return request({ url: '/fee/crossAlloc/logs', method: 'get', params: query })
}
