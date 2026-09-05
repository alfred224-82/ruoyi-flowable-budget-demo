import request from '@/utils/request'

// 模板列表（内置7套 + 租户自定义）
export function listExportTemplate() {
  return request({
    url: '/fee/export/templates',
    method: 'get'
  })
}

// 执行凭证导出（Excel 二进制流）
export function runExport(month, templateId, projectId) {
  return request({
    url: '/fee/export/run',
    method: 'get',
    params: { month, templateId, projectId },
    responseType: 'blob'
  })
}
