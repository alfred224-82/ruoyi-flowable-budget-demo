import request from '@/utils/request'

// 脱敏规则列表
export function listDesensitizeRules() {
  return request({ url: '/fee/desensitize/list', method: 'get' })
}

// 保存脱敏规则
export function saveDesensitizeRule(data) {
  return request({ url: '/fee/desensitize', method: 'post', data })
}

// 删除脱敏规则
export function deleteDesensitizeRule(id) {
  return request({ url: '/fee/desensitize/' + id, method: 'delete' })
}

// 测试脱敏效果
export function testDesensitize(text) {
  return request({ url: '/fee/desensitize/test', method: 'get', params: { text } })
}

// 敏感词检测
export function checkSensitive(params) {
  return request({ url: '/fee/desensitize/checkSensitive', method: 'get', params })
}
