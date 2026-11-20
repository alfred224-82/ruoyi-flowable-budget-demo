import request from '@/utils/request'

// 平台监管（T25.4/T25.5，platform_admin PC 监管台 + 合规协议管理 + 活动留痕）
// ★system:supervision:* 权限，platform_admin 专属

// ===== 投诉监管 =====

// 全部投诉列表（平台 Admin 跨租户查看）
// params: { channel, status, tenantId }
export function listAllComplaints(params) {
  return request({
    url: '/system/supervision/complaints',
    method: 'get',
    params: params
  })
}

// 停止活动
export function stopActivity(activityId) {
  return request({
    url: '/system/supervision/stopActivity/' + activityId,
    method: 'put'
  })
}

// 停止租户
export function stopTenant(tenantId) {
  return request({
    url: '/system/supervision/stopTenant/' + tenantId,
    method: 'put'
  })
}

// 恢复租户
export function resumeTenant(tenantId) {
  return request({
    url: '/system/supervision/resumeTenant/' + tenantId,
    method: 'put'
  })
}

// 发放 license
// data: { tenantId, licenseType }
export function issueLicense(data) {
  return request({
    url: '/system/supervision/issueLicense',
    method: 'post',
    data: data
  })
}

// 活动留痕查询
// params: { limit }
export function activityTrace(tenantId, params) {
  return request({
    url: '/system/supervision/activityTrace/' + tenantId,
    method: 'get',
    params: params
  })
}

// ===== 合规协议 =====

// 获取当前协议版本/状态
export function getAgreementCurrent() {
  return request({
    url: '/micro/complianceAgreement/current',
    method: 'get'
  })
}

// 签署协议
// params: { agreementVersion }
export function signAgreement(params) {
  return request({
    url: '/micro/complianceAgreement/sign',
    method: 'post',
    params: params
  })
}

// 检查是否已签署
export function checkAgreementSigned() {
  return request({
    url: '/micro/complianceAgreement/check',
    method: 'get'
  })
}

// ===== 合规协议管理（平台 Admin OSS 方案） =====

// 获取当前协议配置（版本号 + OSS URL）
export function getAgreementConfig() {
  return request({
    url: '/system/supervision/agreement/config',
    method: 'get'
  })
}

// 更新协议配置（版本号 + OSS URL）
// data: { version: 'v1.1', contentUrl: 'https://...' }
export function updateAgreementConfig(data) {
  return request({
    url: '/system/supervision/agreement/config',
    method: 'put',
    data: data
  })
}
