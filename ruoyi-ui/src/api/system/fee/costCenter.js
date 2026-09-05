import request from '@/utils/request'

// 查询成本中心列表
export function listCostCenter(query) {
  return request({ url: '/fee/costCenter/list', method: 'get', params: query })
}

// 查询成本中心详情
export function getCostCenter(id) {
  return request({ url: '/fee/costCenter/' + id, method: 'get' })
}

// 新增成本中心
export function addCostCenter(data) {
  return request({ url: '/fee/costCenter', method: 'post', data })
}

// 修改成本中心
export function updateCostCenter(data) {
  return request({ url: '/fee/costCenter', method: 'put', data })
}

// 删除成本中心
export function delCostCenter(id) {
  return request({ url: '/fee/costCenter/' + id, method: 'delete' })
}
