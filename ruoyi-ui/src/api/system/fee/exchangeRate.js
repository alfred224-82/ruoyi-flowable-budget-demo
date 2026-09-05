import request from '@/utils/request'

// 汇率列表
export function listExchangeRate(query) {
  return request({ url: '/fee/exchangeRate/list', method: 'get', params: query })
}

// 保存汇率
export function saveExchangeRate(data) {
  return request({ url: '/fee/exchangeRate', method: 'post', data })
}

// 折算人民币
export function convertToCny(params) {
  return request({ url: '/fee/exchangeRate/convert', method: 'get', params })
}

// 获取汇率
export function getRate(params) {
  return request({ url: '/fee/exchangeRate/rate', method: 'get', params })
}

// 删除汇率
export function deleteExchangeRate(id) {
  return request({ url: '/fee/exchangeRate/' + id, method: 'delete' })
}
