import request from '@/utils/request'

// 三轨管控配置列表
export function listTrackConfig(query) {
  return request({ url: '/fee/track/config/list', method: 'get', params: query })
}

// 三轨管控校验
export function checkTrack(params) {
  return request({ url: '/fee/track/check', method: 'post', params })
}

// 查询科目轨道
export function getTrackType(params) {
  return request({ url: '/fee/track/type', method: 'get', params })
}

// 批量配置轨道
export function batchConfigTrack(data) {
  return request({ url: '/fee/track/config/batchSet', method: 'post', data })
}
