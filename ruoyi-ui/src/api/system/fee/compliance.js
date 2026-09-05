import request from '@/utils/request'

// 合规配置列表
export function listComplianceConfig(query) {
  return request({ url: '/fee/compliance/config/list', method: 'get', params: query })
}

// 批量设置合规级别
export function batchSetLevel(data) {
  return request({ url: '/fee/compliance/config/batchSet', method: 'post', data })
}

// 执行合规校验
export function checkCompliance(billId, params) {
  return request({ url: '/fee/compliance/check/' + billId, method: 'post', params })
}

// 财务盖章确认
export function confirmSeal(recordId) {
  return request({ url: '/fee/compliance/confirmSeal/' + recordId, method: 'post' })
}

// 查询合规记录
export function getComplianceRecord(billId) {
  return request({ url: '/fee/compliance/record/' + billId, method: 'get' })
}
