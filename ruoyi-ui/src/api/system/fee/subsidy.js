import request from '@/utils/request'

// 补助标准列表
export function listStandards(query) {
  return request({ url: '/fee/subsidy/standards', method: 'get', params: query })
}

// 保存补助标准
export function saveStandard(data) {
  return request({ url: '/fee/subsidy/standard', method: 'post', data })
}

// 试算补助
export function calculateSubsidy(params) {
  return request({ url: '/fee/subsidy/calculate', method: 'get', params })
}

// 删除补助标准
export function deleteStandard(id) {
  return request({ url: '/fee/subsidy/standard/' + id, method: 'delete' })
}
