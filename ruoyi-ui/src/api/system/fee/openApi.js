import request from '@/utils/request'

// ==================== API凭证（appSecret 脱敏，仅创建/重置一次性返回明文） ====================

// 凭证分页列表
export function listCredential(query) {
  return request({ url: '/biz/openapi/list', method: 'get', params: query })
}

// 凭证详情
export function getCredential(id) {
  return request({ url: '/biz/openapi/' + id, method: 'get' })
}

// 新建凭证（返回 { appKey, appSecret } 明文，仅此一次）
export function addCredential(data) {
  return request({ url: '/biz/openapi', method: 'post', data })
}

// 编辑凭证（不含 appSecret）
export function updateCredential(data) {
  return request({ url: '/biz/openapi', method: 'put', data })
}

// 重置 appSecret（返回新明文，仅此一次）
export function resetSecret(id) {
  return request({ url: '/biz/openapi/resetSecret/' + id, method: 'put' })
}

// 启停凭证（status: ACTIVE/DISABLED）
export function changeStatus(id, status) {
  return request({ url: '/biz/openapi/status/' + id + '/' + status, method: 'put' })
}

// 删除凭证
export function delCredential(ids) {
  return request({ url: '/biz/openapi/' + ids, method: 'delete' })
}

// 调用审计分页列表
export function listCallLog(query) {
  return request({ url: '/biz/openapi/callLog', method: 'get', params: query })
}

// ==================== Webhook 回调注册（secret 脱敏，仅创建/重置一次性返回明文） ====================

// Webhook 分页列表
export function listWebhook(query) {
  return request({ url: '/biz/openapi/webhook/list', method: 'get', params: query })
}

// Webhook 详情
export function getWebhook(id) {
  return request({ url: '/biz/openapi/webhook/' + id, method: 'get' })
}

// 新建 Webhook（返回 { id, secret } 明文，仅此一次）
export function addWebhook(data) {
  return request({ url: '/biz/openapi/webhook', method: 'post', data })
}

// 编辑 Webhook（不含 secret）
export function updateWebhook(data) {
  return request({ url: '/biz/openapi/webhook', method: 'put', data })
}

// 重置 Webhook secret（返回新明文，仅此一次）
export function resetWebhookSecret(id) {
  return request({ url: '/biz/openapi/webhook/resetSecret/' + id, method: 'put' })
}

// 删除 Webhook
export function delWebhook(ids) {
  return request({ url: '/biz/openapi/webhook/' + ids, method: 'delete' })
}
