import request from '@/utils/request'

// 获取盈利提示
export function getTip(params) {
  return request({ url: '/fee/profitTip/get', method: 'get', params })
}

// 盈利提示配置列表
export function listProfitTipConfig(query) {
  return request({ url: '/fee/profitTip/config/list', method: 'get', params: query })
}

// 保存盈利提示配置
export function saveProfitTipConfig(data) {
  return request({ url: '/fee/profitTip/config', method: 'post', data })
}

// 删除盈利提示配置
export function deleteProfitTipConfig(id) {
  return request({ url: '/fee/profitTip/config/' + id, method: 'delete' })
}
