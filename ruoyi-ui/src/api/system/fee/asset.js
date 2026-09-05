import request from '@/utils/request'

// 资产卡片列表
export function listAsset(query) {
  return request({ url: '/fee/asset/list', method: 'get', params: query })
}

// 资产详情
export function getAsset(id) {
  return request({ url: '/fee/asset/' + id, method: 'get' })
}

// 新增资产
export function addAsset(data) {
  return request({ url: '/fee/asset', method: 'post', data })
}

// 修改资产
export function updateAsset(data) {
  return request({ url: '/fee/asset', method: 'put', data })
}

// 资产处置
export function disposeAsset(id) {
  return request({ url: '/fee/asset/dispose/' + id, method: 'post' })
}
