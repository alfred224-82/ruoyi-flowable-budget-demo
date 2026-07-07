import request from '@/utils/request'

/**
 * 生成AI报表
 * @param {Object} data - { reportType, sheetIds, parameters }
 */
export function generateReport(data) {
  return request({
    url: '/ai/report/generate',
    method: 'post',
    data: data,
    timeout: 180000 // AI生成报表可能需要较长时间
  })
}

/**
 * 查询报表生成历史
 * @param {Object} params - { reportType, pageNum, pageSize }
 */
export function listReportHistory(params) {
  return request({
    url: '/ai/report/history',
    method: 'get',
    params: params
  })
}

/**
 * 获取报表详情
 * @param {Number} id - 报表ID
 */
export function getReportDetail(id) {
  return request({
    url: '/ai/report/detail/' + id,
    method: 'get'
  })
}

/**
 * 删除报表记录
 * @param {Number} id - 报表ID
 */
export function deleteReport(id) {
  return request({
    url: '/ai/report/' + id,
    method: 'delete'
  })
}

/**
 * 获取报表类型列表
 */
export function getReportTypes() {
  return request({
    url: '/ai/report/types',
    method: 'get'
  })
}
