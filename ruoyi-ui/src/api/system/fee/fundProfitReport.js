import request from '@/utils/request'

// 项目利润报表分页列表（T6.5）
export function listProfitReport(query) {
  return request({
    url: '/fee/profitReport/list',
    method: 'get',
    params: query
  })
}

// 单项目利润详情（利润拆解）
export function getProfitDetail(projectId) {
  return request({
    url: '/fee/profitReport/' + projectId,
    method: 'get'
  })
}

// 项目收支时间线
export function getProfitTimeline(projectId) {
  return request({
    url: '/fee/profitReport/timeline/' + projectId,
    method: 'get'
  })
}

// 导出利润报表（按项目/按月，Excel 二进制流）
export function exportProfitReport(query) {
  return request({
    url: '/fee/profitReport/export',
    method: 'get',
    params: query,
    responseType: 'blob'
  })
}
