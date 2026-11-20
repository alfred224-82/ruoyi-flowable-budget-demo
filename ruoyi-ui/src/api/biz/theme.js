import request from '@/utils/request'

// 主题预设列表（四套 Design Token）
export function listPresets() {
  return request({
    url: '/biz/theme/presets',
    method: 'get'
  })
}

// 按 key 取预设 Design Token（CSS 变量键值）
export function getTokens(presetKey) {
  return request({
    url: '/biz/theme/tokens/' + presetKey,
    method: 'get'
  })
}

// 三层融合预览（活动级优先，缺失回落租户级）
export function previewTheme(activityId) {
  return request({
    url: '/biz/theme/preview',
    method: 'get',
    params: { activityId }
  })
}

// 装修保存（内容安全审核 → 落库 → 衍生图管线 → 回写成品URL）
export function decorate(data) {
  return request({
    url: '/biz/theme/decorate',
    method: 'post',
    data: data
  })
}
