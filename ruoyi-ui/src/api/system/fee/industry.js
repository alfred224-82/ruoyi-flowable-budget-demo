import request from '@/utils/request'

// 行业配置列表
export function listIndustry() {
  return request({ url: '/fee/industry/list', method: 'get' })
}

// 行业详情
export function getIndustry(code) {
  return request({ url: '/fee/industry/' + code, method: 'get' })
}

// 保存行业配置
export function saveIndustry(data) {
  return request({ url: '/fee/industry', method: 'post', data })
}

// 删除行业配置
export function deleteIndustry(id) {
  return request({ url: '/fee/industry/' + id, method: 'delete' })
}

// 获取当前用户行业
export function getCurrentIndustry() {
  return request({ url: '/fee/industry/current', method: 'get' })
}
