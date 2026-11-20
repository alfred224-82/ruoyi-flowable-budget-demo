import request from '@/utils/request'

// 租户业务配置（T10.4/T10.5/T10.6，JSON Schema 校验 + 文案映射 + Schema 渲染引擎）

// 查询租户单店覆盖配置
export function getTenantBizConfig(tenantId) {
  return request({
    url: '/system/tenantBizConfig/' + tenantId,
    method: 'get'
  })
}

// 查询合并后的模块覆盖（业态包默认 ∪ 租户覆盖）
export function getMergedOverrides(tenantId, bizCode) {
  return request({
    url: '/system/tenantBizConfig/' + tenantId + '/merged',
    method: 'get',
    params: { bizCode }
  })
}

// 保存租户单店覆盖配置（后端做 JSON Schema 校验）
export function saveTenantBizConfig(data) {
  return request({
    url: '/system/tenantBizConfig',
    method: 'post',
    data: data
  })
}

// T10.4：获取 module_overrides 的 JSON Schema 描述
export function getOverridesSchema() {
  return request({
    url: '/system/tenantBizConfig/schema',
    method: 'get'
  })
}

// 获取当前租户渲染 Schema（T10.6 Schema 页面引擎）
export function getRenderSchema() {
  return request({
    url: '/system/tenantBizConfig/render',
    method: 'get'
  })
}

// 获取当前租户文案映射（T10.5 i18n）
export function listBizCopy(query) {
  return request({
    url: '/system/bizCopy/list',
    method: 'get',
    params: query
  })
}

// 保存文案映射
export function saveBizCopy(data) {
  return request({
    url: '/system/bizCopy',
    method: 'post',
    data: data
  })
}

// 批量保存文案映射
export function batchSaveBizCopy(data) {
  return request({
    url: '/system/bizCopy/batch',
    method: 'post',
    data: data
  })
}

// 获取 Schema 描述（T10.4）
export function describeSchema() {
  return request({
    url: '/system/tenantBizConfig/describeSchema',
    method: 'get'
  })
}
