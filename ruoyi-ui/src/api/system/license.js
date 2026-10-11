import request from '@/utils/request'

// 获取当前授权信息（前端根据此接口动态渲染菜单/按钮）
export function getLicenseInfo() {
  return request({
    url: '/system/license/info',
    method: 'get'
  })
}

// 导出机器指纹
export function getFingerprint() {
  return request({
    url: '/system/license/fingerprint',
    method: 'get'
  })
}

// 替换License / 切换版本
export function replaceLicense(data) {
  return request({
    url: '/system/license/replace',
    method: 'post',
    data: data
  })
}

// Admin 侧签发 License（T15.33+）：为指定租户生成可直接导入的 License JSON
// data 为签发参数对象：{ tenantId, edition, versionType, tenantType, licenseType, durationDays, customer, industries, bizCode, plan }
// 响应 data 为标准 License JSON 字符串，前端复制后交给租户管理员在「版本升级」页面粘贴导入
export function generateLicense(data) {
  return request({
    url: '/system/license/generate',
    method: 'post',
    data: data
  })
}

// 获取试用剩余时长
export function getTrialStatus() {
  return request({
    url: '/system/license/trial-status',
    method: 'get'
  })
}

// 获取当前版本等级
export function getEdition() {
  return request({
    url: '/system/license/edition',
    method: 'get'
  })
}
