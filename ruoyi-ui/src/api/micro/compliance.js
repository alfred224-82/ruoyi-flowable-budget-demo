import request from '@/utils/request'

// ===== T18.2 Admin 平台合规管理 =====

// 查询全局录音留存上限天数
export function getGlobalRetention() {
  return request({ url: '/system/compliance/globalRetention', method: 'get' })
}

// 设置全局录音留存上限天数
export function setGlobalRetention(days) {
  return request({ url: '/system/compliance/globalRetention', method: 'put', params: { days } })
}

// 合规告警看板（近 7 天命中趋势）
export function getAlertDashboard() {
  return request({ url: '/system/compliance/alertDashboard', method: 'get' })
}

// 平台敏感词列表（分页）
export function listPlatformWords(query) {
  return request({ url: '/system/compliance/sensitiveWord/list', method: 'get', params: query })
}

// 平台敏感词详情
export function getPlatformWord(id) {
  return request({ url: '/system/compliance/sensitiveWord/' + id, method: 'get' })
}

// 新增平台敏感词
export function addPlatformWord(data) {
  return request({ url: '/system/compliance/sensitiveWord', method: 'post', data })
}

// 修改平台敏感词
export function updatePlatformWord(data) {
  return request({ url: '/system/compliance/sensitiveWord', method: 'put', data })
}

// 删除平台敏感词
export function removePlatformWord(id) {
  return request({ url: '/system/compliance/sensitiveWord/' + id, method: 'delete' })
}

// 批量导入平台敏感词
export function importPlatformWords(words, category, level) {
  return request({ url: '/system/compliance/sensitiveWord/import', method: 'post', params: { words, category, level } })
}

// ===== T18.3 租户管理员合规配置 =====

// 合规规则列表（分页）
export function listComplianceRules(query) {
  return request({ url: '/micro/compliance/config/rule/list', method: 'get', params: query })
}

// 合规规则详情
export function getComplianceRule(id) {
  return request({ url: '/micro/compliance/config/rule/' + id, method: 'get' })
}

// 新增合规规则
export function addComplianceRule(data) {
  return request({ url: '/micro/compliance/config/rule', method: 'post', data })
}

// 修改合规规则
export function updateComplianceRule(data) {
  return request({ url: '/micro/compliance/config/rule', method: 'put', data })
}

// 删除合规规则
export function removeComplianceRule(id) {
  return request({ url: '/micro/compliance/config/rule/' + id, method: 'delete' })
}

// 查询录音策略
export function getRecordingPolicy() {
  return request({ url: '/micro/compliance/config/policy', method: 'get' })
}

// 保存录音策略
export function saveRecordingPolicy(data) {
  return request({ url: '/micro/compliance/config/policy', method: 'put', data })
}

// 租户敏感词列表（分页）→ 新接口 /compliance/tenant/sensitiveWord/list
export function listTenantWords(query) {
  return request({ url: '/compliance/tenant/sensitiveWord/list', method: 'get', params: query })
}

// 全局底库只读列表（分页）
export function listGlobalWords(query) {
  return request({ url: '/compliance/tenant/sensitiveWord/global', method: 'get', params: query })
}

// 新增自定义敏感词 → 新接口 /compliance/tenant/sensitiveWord
export function addTenantWord(data) {
  return request({ url: '/compliance/tenant/sensitiveWord', method: 'post', data })
}

// 修改自定义敏感词
export function updateTenantWord(data) {
  return request({ url: '/compliance/tenant/sensitiveWord', method: 'put', data })
}

// 删除自定义敏感词
export function removeTenantWord(id) {
  return request({ url: '/compliance/tenant/sensitiveWord/' + id, method: 'delete' })
}

// 有效敏感词列表（按策略合并）
export function getEffectiveWords(strategy) {
  return request({ url: '/micro/compliance/config/sensitiveWord/effective', method: 'get', params: { strategy } })
}

// ===== T18.4 老板合规记录（只读） =====

// 合规命中记录列表（分页）
export function listComplianceHits(query) {
  return request({ url: '/micro/compliance/records/list', method: 'get', params: query })
}
